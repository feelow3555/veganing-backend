package com.veganing.global.infra.voyage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class VoyageAiService {

    // Spring의 RestClient = HTTP 요청을 보내는 클라이언트
    // WebClient(비동기)와 달리 RestClient는 동기 방식 (Spring Boot 3.2+부터 표준)
    private final RestClient restClient;

    // application-local.yaml의 voyage.api-key 값을 주입받음
    @Value("${voyage.api-key}")
    private String apiKey;

    public VoyageAiService() {
        // baseUrl 설정 → 이후 .uri("/embeddings")만 써도 full URL이 완성됨
        this.restClient = RestClient.builder()
                .baseUrl("https://api.voyageai.com/v1")
                .build();
    }

    // 레시피 텍스트 한 줄을 받아 1024차원 float 배열로 반환
    // 스케줄러(임베딩 저장)와 추천 API(쿼리 벡터 생성) 두 곳에서 호출됨
    public float[] embed(String text) {

        // Voyage AI 요청 바디
        // input은 배열 형태 (여러 텍스트 한번에 가능하지만 여기선 1개씩)
        Map<String, Object> requestBody = Map.of(
                "input", List.of(text),
                "model", "voyage-3-lite"  // 1024차원, 가격 대비 성능 최고
        );

        // POST https://api.voyageai.com/v1/embeddings
        // Authorization: Bearer {apiKey} 헤더로 인증
        // .body(Map.class) → 응답 JSON을 Map으로 역직렬화
        Map response = restClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        // Voyage AI 응답 구조:
        // {
        //   "data": [
        //     { "embedding": [0.1, 0.2, ..., 0.9] }  ← 1024개 숫자
        //   ]
        // }
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<Double> embedding = (List<Double>) data.get(0).get("embedding");

        // JSON 숫자는 기본적으로 Double로 역직렬화됨
        // DB에 저장할 float[]로 변환 (float이 Double보다 메모리 절반, 벡터 검색엔 충분한 정밀도)
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}