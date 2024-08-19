package com.bilicraft.oraxenhostingservice;

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

public class TencentCosClient {
    private final COSClient cosClient;

    /**
     * @param secretId   用户secretId
     * @param secretKey  用户secretKey
     * @param regionName bucket的地域
     */
    public TencentCosClient(String secretId, String secretKey, String regionName) {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 生成 cos 客户端。
        this.cosClient = new COSClient(cred, clientConfig);
    }

    /**
     * 上传文件到COS
     *
     * @param localFile  要上传的文件
     * @param bucketName 指定文件将要存放的存储桶，存储桶的命名格式为 BucketName-APPID
     * @param key       指定文件上传到 COS 上的路径（对象键）
     */
    public void uploadFile(File localFile, String bucketName, String key) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
    }

    /**
     * 获取文件临时直链链接
     *
     * @param bucketName 存储桶的命名格式为 BucketName-APPID
     * @param key        对象键，对象在存储桶中的唯一标识
     * @param expireTime 链接过期时间（分钟）
     * @return 直链链接
     */
    public String getObjectUrl(String bucketName, String key, long expireTime) {
        // 设置签名过期时间(可选), 若未进行设置则默认使用 ClientConfig 中的签名过期时间(1小时)
        // 这里设置签名在半个小时后过期
        Date expirationDate = new Date(System.currentTimeMillis() + expireTime * 60 * 1000);

        // 填写本次请求的参数，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的参数
        Map<String, String> params = new HashMap<>();
        params.put("param1", "rbb");

        // 填写本次请求的头部，需与实际请求相同，能够防止用户篡改此签名的 HTTP 请求的头部
        Map<String, String> headers = new HashMap<>();
        headers.put("header1", "rls");

        // 请求的 HTTP 方法，上传请求用 PUT，下载请求用 GET，删除请求用 DELETE
        HttpMethodName method = HttpMethodName.GET;

        URL url = cosClient.generatePresignedUrl(bucketName, key, expirationDate, method, headers, params);
        return url.toString();
    }
}
