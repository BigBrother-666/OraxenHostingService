package com.bigbrother.resourcepackhostingservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static String getFileMD5(File file) {
        try {
            // 创建MessageDigest实例，用于计算MD5
            MessageDigest digest = MessageDigest.getInstance("MD5");
            // 读取资源包内容并更新到MessageDigest
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesRead);
                }
            }
            // 计算哈希值
            byte[] bytes = digest.digest();
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            ResourcePackHostingService.logger.error("计算资源包md5时发生错误：{}", String.valueOf(e));
        }
        return null;
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
            ResourcePackHostingService.logger.error("计算资源包sha1失败！");
            ResourcePackHostingService.logger.error(e.toString());
            return null;
        }
    }
}
