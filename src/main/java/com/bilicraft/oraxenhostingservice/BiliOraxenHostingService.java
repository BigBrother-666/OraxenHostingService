package com.bilicraft.oraxenhostingservice;

import com.bilicraft.oraxenhostingservice.client.Client;
import com.bilicraft.oraxenhostingservice.client.PanClient;
import com.bilicraft.oraxenhostingservice.client.TencentCosClient;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class BiliOraxenHostingService implements HostingProvider {
    private Client client;
    private String sha1;
    private UUID packUUID;


    public BiliOraxenHostingService() {
        getConfig();
    }

    private void getConfig(){
        String selectClient = OraxenHostingService.config.getString("client");
        if (selectClient != null && selectClient.equals("pan")){
            client = new PanClient();
        } else if (selectClient != null && selectClient.equals("tencent-cos")){
            client = new TencentCosClient();
        } else {
            OraxenHostingService.logger.warning("client配置错误");
        }
    }

    @Override
    public boolean uploadPack(File file) {
        // 保证重载ohs后，oraxen按照新配置上传
        getConfig();
        client.uploadFile(file);
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
        return client.getFileUrl();
    }

    @Override
    public String getMinecraftPackURL() {
        return client.getFileUrl();
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
