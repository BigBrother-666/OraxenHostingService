package com.bilicraft.oraxenhostingservice;

import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class BiliHostingService implements HostingProvider {
    private String sha1;
    private UUID packUUID;

    private final String bucketName;
    private final String key;
    private final Long expireTime;
    private final TencentCosClient tencentCosClient;

    public BiliHostingService() {
        String secretId = OraxenHostingService.config.getString("secret-id");
        String secretKey = OraxenHostingService.config.getString("secret-key");
        String regionName = OraxenHostingService.config.getString("region-name");
        if (secretId == null || secretId.trim().isEmpty()) {
            secretId = System.getenv("SECRET_ID");
        } else {
            OraxenHostingService.logger.warning("secret-id获取失败");
        }
        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = System.getenv("secret-key");
        } else {
            OraxenHostingService.logger.warning("secret-key获取失败");
        }

        bucketName = OraxenHostingService.config.getString("bucket-name");
        key = OraxenHostingService.config.getString("key");
        expireTime = OraxenHostingService.config.getLong("expire-time");
        tencentCosClient = new TencentCosClient(secretId, secretKey, regionName);
    }

    @Override
    public boolean uploadPack(File file) {
        tencentCosClient.uploadFile(file, bucketName, key);
        sha1 = getFileSHA1(file);
        if (sha1 != null) {
            packUUID = UUID.nameUUIDFromBytes(sha1.getBytes());
        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getPackURL() {
        return tencentCosClient.getObjectUrl(bucketName, key, expireTime);
    }

    @Override
    public String getMinecraftPackURL() {
        return tencentCosClient.getObjectUrl(bucketName, key, expireTime);
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

    public static String getFileSHA1(File file) {
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

            // 读取文件并更新到 MessageDigest 中
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                sha1Digest.update(buffer, 0, bytesRead);
            }
            fis.close();

            // 计算哈希值
            byte[] sha1Bytes = sha1Digest.digest();

            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : sha1Bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            OraxenHostingService.logger.warning("计算资源包sha1失败！");
            OraxenHostingService.logger.warning(e.toString());
            return null;
        }
    }
}
