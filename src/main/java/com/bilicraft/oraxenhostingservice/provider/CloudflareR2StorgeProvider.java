package com.bilicraft.oraxenhostingservice.provider;

import com.bilicraft.oraxenhostingservice.OraxenHostingService;

import java.io.File;
import java.time.Duration;

public class CloudflareR2StorgeProvider extends StorageProvider {
    private final CloudflareR2Client r2Client;
    // 存储桶名称
    private final String bucketName;
    // 对象键，对象在存储桶中的唯一标识
    private final String objectKey;

    public CloudflareR2StorgeProvider(String providerName) {
        super(OraxenHostingService.config.getLong(providerName + ".expire-time"), providerName);

        // 加载配置
        bucketName = OraxenHostingService.config.getString(providerName + ".bucket-name");
        objectKey = OraxenHostingService.config.getString(providerName + ".object-key");

        CloudflareR2Client.S3Config config = new CloudflareR2Client.S3Config(
                OraxenHostingService.config.getString(providerName + ".account-id"),
                OraxenHostingService.config.getString(providerName + ".access-key"),
                OraxenHostingService.config.getString(providerName + ".secret-key")
        );
        this.r2Client = new CloudflareR2Client(config);
    }

    @Override
    public boolean uploadFile(File localFile) {
        return r2Client.uploadFile(localFile, bucketName, objectKey);
    }

    @Override
    protected String generatePresignedUrl() {
        return r2Client.generatePresignedDownloadUrl(
                bucketName,
                objectKey,
                Duration.ofMinutes(this.getUrlExpireMinutes())
        );
    }
}
