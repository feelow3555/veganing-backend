package com.veganing.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public S3Presigner s3Presigner() {
        // 액세스키 + 시크릿키로 자격증명 객체 생성
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        // Presigned URL 발급기 빈 등록 (클라이언트가 S3에 직접 업로드할 때 사용)
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public S3Client s3Client() {
        // 액세스키 + 시크릿키로 자격증명 객체 생성
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        // S3 클라이언트 빈 등록 (서버에서 직접 S3에 파일 올릴 때 사용 - Phase 7 레시피 텍스트 업로드)
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}