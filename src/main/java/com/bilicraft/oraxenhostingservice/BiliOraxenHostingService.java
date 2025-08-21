package com.bilicraft.oraxenhostingservice;

import com.bilicraft.oraxenhostingservice.provider.CloudflareR2StorgeProvider;
import com.bilicraft.oraxenhostingservice.provider.StorageProvider;
import com.bilicraft.oraxenhostingservice.provider.PanStorageProvider;
import com.bilicraft.oraxenhostingservice.provider.TencentCosStorageProvider;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class BiliOraxenHostingService implements HostingProvider {
    private final List<StorageProvider> enabledStorageProvider = new ArrayList<>();
    // 当前正在使用的 StorageProvider
    private StorageProvider currProvider;
    private String sha1;
    private UUID packUUID;

    public BiliOraxenHostingService() {
        loadConfig();
    }

    public List<StorageProvider> getEnabledStorageProvider() {
        return enabledStorageProvider;
    }

    /**
     * 设置指定idx的provider，如果越界，则设置为第一个
     *
     * @param idx 要设置的provider的idx
     * @return 实际设置的idx
     */
    public int setProvider(int idx) {
        if (enabledStorageProvider.isEmpty()) {
            return -1;
        }
        // 如果当前为空，使用第一个
        if (currProvider == null || idx >= enabledStorageProvider.size() || idx < 0) {
            currProvider = enabledStorageProvider.get(0);
            return 0;
        }
        currProvider = enabledStorageProvider.get(idx);
        return idx;
    }

    /**
     * 加载对象存储配置
     */
    public void loadConfig() {
        for (StorageProvider storageProvider : enabledStorageProvider) {
            storageProvider.close();
        }
        enabledStorageProvider.clear();
        for (String key : OraxenHostingService.config.getKeys(false)) {
            if (OraxenHostingService.config.getBoolean(key + ".enable", false)) {
                switch (key) {
                    case "123pan":
                        enabledStorageProvider.add(new PanStorageProvider(key));
                        break;
                    case "tencent-cos":
                        enabledStorageProvider.add(new TencentCosStorageProvider(key));
                        break;
                    case "cloud-flare-r2":
                        enabledStorageProvider.add(new CloudflareR2StorgeProvider(key));
                        break;
                    default:
                        OraxenHostingService.logger.error("未知的配置：{}", key);
                        continue;
                }
                OraxenHostingService.logger.info("成功启用 => {}", key);
            }
        }
        // 获取第一个Provider
        setProvider(0);
    }

    @Override
    public boolean uploadPack(File file) {
        if (enabledStorageProvider.isEmpty()) {
            return false;
        }
        // 上传至所有启用的client
        Iterator<StorageProvider> iterator = enabledStorageProvider.iterator();
        while (iterator.hasNext()) {
            StorageProvider provider = iterator.next();
            Instant start = Instant.now();
            boolean success;
            try {
                success = provider.uploadFile(file);
            } catch (Exception e) {
                success = false;
                OraxenHostingService.logger.error(e.toString());
            }
            Instant end = Instant.now();
            long millis = Duration.between(start, end).toMillis();

            if (success) {
                OraxenHostingService.logger.info("上传资源包 {} 至 {} 成功，耗时 {} ms",
                        file.getName(), provider.getProviderName(), millis);
            } else {
                OraxenHostingService.logger.error("上传资源包 {} 至 {} 失败，耗时 {} ms",
                        file.getName(), provider.getProviderName(), millis);
                OraxenHostingService.logger.error("禁用 {} ", provider.getProviderName());
                iterator.remove();
            }
        }
        sha1 = Utils.getFileSHA1(file);
        if (sha1 != null) {
            packUUID = UUID.nameUUIDFromBytes(sha1.getBytes());
        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getPackURL() {
        if (currProvider == null) {
            OraxenHostingService.logger.error("获取资源包url失败，没有指定对象存储服务！");
            return null;
        }
        try {
            String url = currProvider.getFileUrl();
            if (url != null && !url.isEmpty()) {
//                OraxenHostingService.logger.info("get {} pack url => {}", currProvider.getProviderName(), url);
                return url;
            }
        } catch (Exception e) {
            OraxenHostingService.logger.error("获取资源包url错误", e);
        }
        return null;
    }

    @Override
    public String getMinecraftPackURL() {
        return getPackURL();
    }

    @Override
    public byte[] getSHA1() {
        int len = this.sha1.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(this.sha1.charAt(i), 16) << 4) + Character.digit(this.sha1.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public String getOriginalSHA1() {
        return sha1;
    }

    @Override
    public UUID getPackUUID() {
        return packUUID;
    }
}
