package com.veganing.global.infra.voyage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

// ─────────────────────────────────────────────────────────────
// 왜 @InjectMocks를 안 쓰나?
//
// VoyageAiService는 생성자 안에서 RestClient.builder().build()를 직접 호출함.
// Mockito의 @InjectMocks는 객체를 생성한 뒤 필드를 주입하는데,
// 생성자 실행 시점에 이미 진짜 RestClient가 만들어져 버려서
// 나중에 Mock을 주입해도 이미 세팅된 필드를 교체하지 않음.
//
// 해결: new VoyageAiService()로 직접 생성 후
//       ReflectionTestUtils.setField()로 private 필드를 강제로 교체.
//       Spring이 리플렉션으로 @Value를 주입하는 방식과 동일한 원리.
// ─────────────────────────────────────────────────────────────
@ExtendWith(MockitoExtension.class)
class VoyageAiServiceTest {

    private VoyageAiService voyageAiService;

    // RestClient는 빌더 패턴으로 체이닝되는 구조:
    // restClient.post()                → RequestBodyUriSpec
    //           .uri("/embeddings")    → RequestBodySpec
    //           .header(...)           → RequestBodySpec (자기 자신 반환)
    //           .body(...)             → RequestBodySpec (자기 자신 반환)
    //           .retrieve()            → ResponseSpec
    //           .body(Map.class)       → 실제 응답 Map
    //
    // 체이닝의 각 단계가 다음 단계 타입을 반환하므로
    // 단계별로 Mock을 따로 만들어야 함.
    @Mock private RestClient mockRestClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;  // post() 반환 타입
    @Mock private RestClient.RequestBodySpec requestBodySpec;        // uri(), header(), body() 반환 타입
    @Mock private RestClient.ResponseSpec responseSpec;              // retrieve() 반환 타입

    @BeforeEach
    void setUp() {
        // 생성자를 직접 호출 → 내부에서 진짜 RestClient가 만들어짐
        voyageAiService = new VoyageAiService();

        // 진짜 RestClient를 Mock으로 교체 (private 필드라 ReflectionTestUtils 사용)
        // ReflectionTestUtils: Spring Test가 제공하는 리플렉션 유틸.
        //   private, final 필드도 강제로 읽고 쓸 수 있음.
        //   테스트 환경에서 @Value 미주입 문제를 해결하는 표준 방법.
        ReflectionTestUtils.setField(voyageAiService, "restClient", mockRestClient);
        ReflectionTestUtils.setField(voyageAiService, "apiKey", "test-api-key");
    }

    @Test
    @DisplayName("텍스트 임베딩 요청 시 Voyage AI 응답을 float[]로 변환해 반환한다")
    void embed_success() {
        // given ─ Voyage AI 실제 응답 구조를 Map으로 모방
        // 실제 응답 JSON:
        // {
        //   "data": [
        //     { "embedding": [0.1, 0.2, 0.3, 0.4, 0.5] }
        //   ]
        // }
        // Jackson은 JSON 숫자를 기본적으로 Double로 역직렬화하므로 List<Double>로 세팅
        List<Double> embeddingValues = List.of(0.1, 0.2, 0.3, 0.4, 0.5);
        Map<String, Object> embeddingData = Map.of("embedding", embeddingValues);
        Map<String, Object> mockResponse = Map.of("data", List.of(embeddingData));

        // RestClient 체이닝 각 단계별 Mock 동작 정의
        // given().willReturn()의 반환값이 다음 체이닝 단계의 입력이 됨
        given(mockRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(any(String.class))).willReturn(requestBodySpec);
        // header()는 자기 자신(RequestBodySpec)을 반환하므로 같은 Mock을 계속 반환
        given(requestBodySpec.header(eq("Authorization"), any())).willReturn(requestBodySpec);
        given(requestBodySpec.header(eq("Content-Type"), any())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Map.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(mockResponse);

        // when
        float[] result = voyageAiService.embed("두부 150g 브로콜리 비건 레시피");

        // then
        // 응답 배열 길이 = 입력 embedding 값 개수
        assertThat(result).hasSize(5);
        // within(0.001f): float 부동소수점 오차 허용 범위
        // 0.1 == 0.1f 비교 시 정밀도 차이로 미세한 오차 발생 가능 → 범위 비교
        assertThat(result[0]).isEqualTo(0.1f, within(0.001f));
        assertThat(result[1]).isEqualTo(0.2f, within(0.001f));
    }

    @Test
    @DisplayName("Double 타입 임베딩 값이 float[]으로 정확히 변환된다")
    void embed_parsesDoubleToFloat() {
        // given
        // 변환 정밀도 검증을 위해 소수점이 많은 값, 경계값(1.0, -1.0, 0.0)을 포함
        // Double(64bit) → float(32bit) 변환 시 하위 비트가 잘려 미세한 오차 발생 가능
        // 벡터 검색에서 이 정도 오차는 유사도 계산에 영향 없으므로 허용
        List<Double> embeddingValues = List.of(0.123456789, -0.987654321, 1.0, -1.0, 0.0);
        Map<String, Object> embeddingData = Map.of("embedding", embeddingValues);
        Map<String, Object> mockResponse = Map.of("data", List.of(embeddingData));

        // RestClient 체이닝 Mock (embed_success와 동일한 패턴)
        given(mockRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(any(String.class))).willReturn(requestBodySpec);
        given(requestBodySpec.header(eq("Authorization"), any())).willReturn(requestBodySpec);
        given(requestBodySpec.header(eq("Content-Type"), any())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Map.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(mockResponse);

        // when
        float[] result = voyageAiService.embed("테스트 텍스트");

        // then
        assertThat(result).hasSize(5);
        // float 정밀도(소수점 약 7자리)를 고려한 허용 오차 0.0001f로 비교
        assertThat(result[0]).isEqualTo((float) 0.123456789, within(0.0001f));
        // 경계값 1.0, -1.0, 0.0은 float 변환 시 정확히 표현되므로 오차 없어야 함
        assertThat(result[2]).isEqualTo(1.0f, within(0.0001f));
        assertThat(result[3]).isEqualTo(-1.0f, within(0.0001f));
        assertThat(result[4]).isEqualTo(0.0f, within(0.0001f));
    }
}