package com.bigbrother.resourcepackhostingservice.provider;

import com.bigbrother.resourcepackhostingservice.ResourcePackHostingService;
import com.bigbrother.resourcepackhostingservice.Utils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TencentCosStorageProvider extends StorageProvider {
    private COSClient cosStorageProvider;
    // 存储桶名称 BucketName-APPID
    private final String bucketName;
    // 对象键，对象在存储桶中的唯一标识
    private final String objectKey;

    public TencentCosStorageProvider(String providerName) {
        super(ResourcePackHostingService.config.getLong(providerName + ".expire-time"), providerName);
        // 用户secretId
        String secretId = ResourcePackHostingService.config.getString(providerName + ".secret-id");
        // 用户secretKey
        String secretKey = ResourcePackHostingService.config.getString(providerName + ".secret-key");
        // bucket的地域
        String regionName = ResourcePackHostingService.config.getString(providerName + ".region-name");

        bucketName = ResourcePackHostingService.config.getString(providerName + ".bucket-name");
        objectKey = ResourcePackHostingService.config.getString(providerName + ".object-key");

        COSCredentials cred;
        if (secretId != null && secretKey != null) {
            cred = new BasicCOSCredentials(secretId, secretKey);
            Region region = new Region(regionName);
            ClientConfig clientConfig = new ClientConfig(region);
            clientConfig.setHttpProtocol(HttpProtocol.https);
            // 生成 cos 客户端。
            this.cosStorageProvider = new COSClient(cred, clientConfig);
        } else {
            ResourcePackHostingService.logger.error("secretId 或 secretKey 为空");
        }
    }


    @Override
    public boolean uploadFile(File localFile) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, localFile);
        PutObjectResult putObjectResult = cosStorageProvider.putObject(putObjectRequest);
        return putObjectResult.getETag().equals(Utils.getFileMD5(localFile));
    }

    /**
     * 获取文件临时直链链接
     *
     * @return 直链链接
     */
    @Override
    public String generatePresignedUrl() {
        // 设置签名过期时间(可选), 若未进行设置则默认使用 ClientConfig 中的签名过期时间(1小时)
        // 这里设置签名在半个小时后过期
        Date expirationDate = new Date(System.currentTimeMillis() + this.getUrlExpireMinutes() * 60 * 1000);

        // 填写本次请求的参数，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的参数
        Map<String, String> params = new HashMap<>();
        params.put("param1", "rbb");

        // 填写本次请求的头部，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的头部
        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "rls");

        // 请求的 HTTP 方法，上传请求用 PUT，下载请求用 GET，删除请求用 DELETE
        HttpMethodName method = HttpMethodName.GET;

        URL url = cosStorageProvider.generatePresignedUrl(bucketName, objectKey, expirationDate, method, headers, params);
        return url.toString();
    }

    @Override
    public void close() {
        if (cosStorageProvider != null) {
            cosStorageProvider.shutdown();
        }
    }
}
