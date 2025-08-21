package com.bilicraft.oraxenhostingservice.provider;

import java.io.File;

public abstract class StorageProvider {

    private final String providerName;
    private String cachedUrl;
    private long expireAt;
    private final long urlExpireMinutes;

    public long getUrlExpireMinutes() {
        return urlExpireMinutes;
    }

    public String getProviderName() {
        return providerName;
    }

    public StorageProvider(long urlExpireMinutes, String providerName) {
        this.urlExpireMinutes = urlExpireMinutes;
        this.providerName = providerName;
    }

    /**
     * 上传资源包
     * @return 上传是否成功
     */
    public abstract boolean uploadFile(File localFile);

    /**
     * 获取资源包直链。
     * 在 urlExpireMinutes 内返回同一个 URL，过期后刷新。
     */
    public synchronized String getFileUrl() {
        long now = System.currentTimeMillis();
        if (urlExpireMinutes < 0 || cachedUrl == null || now >= expireAt) {
            cachedUrl = generatePresignedUrl();
            expireAt = now + urlExpireMinutes * 60 * 1000;
        }
        return cachedUrl;
    }

    /**
     * 生成新的 Presigned URL
     */
    protected abstract String generatePresignedUrl();

    public void close() {}
}
