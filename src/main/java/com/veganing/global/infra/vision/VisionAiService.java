package com.veganing.global.infra.vision;

import com.veganing.global.infra.s3.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class VisionAiService {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-5";

    @Value("${ai.anthropic.api-key}")
    private String apiKey;

    private final RestClient restClient;
    private final S3Service s3Service; // S3에서 이미지 다운로드용

    public VisionAiService(S3Service s3Service) {
        this.restClient = RestClient.create();
        this.s3Service = s3Service;
    }

    // 식단 사진 + 음식 정보 + 비건 단계 → 한 번에 전체 분석
    public Map<String, Object> analyzeMeal(String imageUrl, String foodName, String foodDescription, String veganLevel) {

        // S3 private 버킷 URL은 Claude API가 직접 접근 불가
        // → 서버에서 S3 이미지를 바이트로 받아 base64로 변환 후 전달
        byte[] imageBytes = s3Service.downloadImage(imageUrl);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String prompt = buildPrompt(foodName, foodDescription, veganLevel);

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "max_tokens", 1024,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        // 이미지 (base64 인코딩)
                                        Map.of(
                                                "type", "image",
                                                "source", Map.of(
                                                        "type", "base64",
                                                        "media_type", "image/jpeg",
                                                        "data", base64Image
                                                )
                                        ),
                                        // 텍스트 프롬프트
                                        Map.of(
                                                "type", "text",
                                                "text", prompt
                                        )
                                )
                        )
                )
        );

        // Claude API 호출
        String response = restClient.post()
                .uri(CLAUDE_API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseResponse(response);
    }

    // 프롬프트 구성 (벤치마크 조건 B: 사진 + 음식정보 + 비건단계)
    private String buildPrompt(String foodName, String foodDescription, String veganLevel) {
        return String.format("""
        음식 이름: %s
        설명: %s
        사용자 비건 단계: %s
        
        위 사진을 분석해서 아래 JSON 형식으로만 응답하세요. 다른 텍스트 없이 JSON만.
        
        {
          "ingredients": [
            {"name": "식재료명", "amount_g": 100}
          ],
          "is_vegan_compliant": true,
          "vegan_violations": ["위반 항목"],
          "nutrition_feedback": "비건 관점 영양소 피드백 3~5문장"
        }
        
        판단 원칙:
        - 사진에서 100%% 확실하게 보이는 동물성 재료가 있을 때만 위반으로 판정.
        - 빵가루, 소스, 드레싱 등 비건/논비건 둘 다 가능한 재료는 위반으로 보지 않음.
        - 확실하지 않으면 is_vegan_compliant: true로 판정. 의심만으로 false 금지.
        - vegan_violations는 사진에서 육안으로 명확히 확인된 것만 포함.
        
        비건 단계별 판단 기준 (사용자 단계: %s):
        - FLEXITARIAN: 모든 식품 허용 → 항상 true
        - POLLO_PESCO: 사진에 적색육(소/돼지)이 명확히 보일 때만 false
        - PESCO: 사진에 적색육/백색육(닭)이 명확히 보일 때만 false
        - POLLO: 사진에 적색육/어패류가 명확히 보일 때만 false
        - LACTO_OVO: 사진에 적색육/백색육/어패류가 명확히 보일 때만 false
        - LACTO: 사진에 적색육/백색육/어패류/달걀이 명확히 보일 때만 false
        - OVO: 사진에 적색육/백색육/어패류/유제품이 명확히 보일 때만 false
        - VEGAN: 사진에 적색육/백색육/어패류/달걀/유제품이 명확히 보일 때만 false
        - FRUITARIAN: 사진에 과일류 외 재료가 명확히 보일 때만 false
        
        nutrition_feedback은 반드시 한국어로.
        vegan_violations는 위반 없으면 빈 배열 [].
        """, foodName, foodDescription, veganLevel, veganLevel);
    }

    // Claude 응답 파싱 → Map으로 반환
    private Map<String, Object> parseResponse(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Claude 응답 구조: content[0].text에 실제 텍스트 있음
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("content").get(0).path("text").asText();

            // 마크다운 코드펜스 제거 (벤치마크에서 확인된 전처리)
            text = text.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(text, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Vision AI 응답 파싱 실패: " + e.getMessage());
        }
    }
}