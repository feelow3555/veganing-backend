package com.veganing.global.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final S3Presigner s3Presigner; // Presigned URL 발급기
    private final S3Client s3Client; // S3 직접 접근 클라이언트 (이미지 다운로드용)

    // 클라이언트가 S3에 직접 업로드할 수 있는 Presigned URL 발급
    // 이미지가 백엔드를 거치지 않아 서버 메모리/대역폭 절약
    public String generatePresignedUrl(String fileName) {

        // S3에 저장될 경로: meals/파일명
        String key = "meals/" + fileName;

        // PUT 요청용 Presigned URL 생성 조건
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/jpeg")
                .build();

        // URL 유효시간 10분 설정
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        // Presigned URL 생성 후 문자열로 반환
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    // Presigned URL 발급 시 같이 반환할 S3 접근 URL 생성
    // 클라이언트가 업로드 완료 후 이 URL을 POST /api/meal 요청에 포함시킴
    public String getImageUrl(String fileName) {
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/meals/" + fileName;
    }

    // Vision AI에 이미지 전달하기 위해 S3에서 바이트로 직접 다운로드
    // Claude API 는 S3 private 버킷 URL 직접 접근 불가 → base64 변환 후 전달
    public byte[] downloadImage(String imageUrl) {
        // URL 디코딩 후 key 추출 (%40 → @ 등)
        String decodedUrl = java.net.URLDecoder.decode(imageUrl, java.nio.charset.StandardCharsets.UTF_8);
        String key = decodedUrl.substring(decodedUrl.indexOf("meals/"));

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(getRequest).asByteArray();
    }
}
