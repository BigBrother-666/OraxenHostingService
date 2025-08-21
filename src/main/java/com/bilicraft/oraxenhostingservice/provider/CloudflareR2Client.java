package com.bilicraft.oraxenhostingservice.provider;

import com.bilicraft.oraxenhostingservice.Utils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.net.URI;
import java.time.Duration;

/**
 * Client for interacting with Cloudflare R2 Storage using AWS SDK S3 compatibility
 */
public class CloudflareR2Client {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    /**
     * Creates a new CloudflareR2Client with the provided configuration
     */
    public CloudflareR2Client(S3Config config) {
        this.s3Client = buildS3Client(config);
        this.presigner = buildS3Presigner(config);
    }

    /**
     * Configuration class for R2 credentials and endpoint
     */
    public static class S3Config {
        private final String accessKey;
        private final String secretKey;
        private final String endpoint;

        public S3Config(String accountId, String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);
        }

        public String getAccessKey() { return accessKey; }
        public String getSecretKey() { return secretKey; }
        public String getEndpoint() { return endpoint; }
    }

    /**
     * Builds and configures the S3 client with R2-specific settings
     */
    private static S3Client buildS3Client(S3Config config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
        );

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    /**
     * Builds and configures the S3 presigner with R2-specific settings
     */
    private static S3Presigner buildS3Presigner(CloudflareR2Client.S3Config config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
        );

        return S3Presigner.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    public boolean uploadFile(File file, String bucketName, String objectKey) {
        // 上传文件
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromFile(file)
        );
        return response.sdkHttpResponse().isSuccessful();
    }

    public String generatePresignedDownloadUrl(String bucketName, String objectKey, Duration expiration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(builder -> builder
                        .bucket(bucketName)
                        .key(objectKey)
                        .build())
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }
}
