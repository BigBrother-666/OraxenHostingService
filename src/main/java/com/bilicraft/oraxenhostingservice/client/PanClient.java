package com.bilicraft.oraxenhostingservice.client;

import com.bilicraft.oraxenhostingservice.OraxenHostingService;
import com.bilicraft.oraxenhostingservice.entity.PanEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class PanClient implements Client {
    //    private final String CLIENT_ID = OraxenHostingService.config.getString("pan.client-id");
//    private final String CLIENT_SECRET = OraxenHostingSvice.config.getString("pan.client-secret");
    private final String clientId = "";
    private final String clientSecret = "";
    private final Integer parentFileId = 8187024;
    private String accessToken = null;

    //    private static final String BASE_URL = OraxenHostingService.config.getString("pan.base-url");
    private final String BASE_URL = "https://open-api.123pan.com";
    private final String ACCESS_TOKEN_URL = BASE_URL + "/api/v1/access_token";
    private final String CREATE_DIR_URL = BASE_URL + "/upload/v1/file/mkdir";
    private final String CREATE_FILE_URL = BASE_URL + "/upload/v1/file/create";
    private final String GET_UPLOAD_URL = BASE_URL + "/upload/v1/file/get_upload_url";
    private final String CHECK_COMPLETE_URL = BASE_URL + "/upload/v1/file/upload_complete";

    private final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private final MediaType MEDIA_TYPE_OCTET_STREAM = MediaType.parse("application/octet-stream");

    public void getAccessToken() {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        // 构造json
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("clientID", clientId);
        jsonObject.put("clientSecret", clientSecret);
        try {
            // 构造body
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(jsonObject));
            // 构造请求
            Request request = new Request.Builder()
                    .url(ACCESS_TOKEN_URL)
                    .addHeader("Platform", "open_platform")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
//                if (panEntity.code == 0) {
//                    OraxenHostingService.logger.info("获取AccessToken成功");
//                } else {
//                    OraxenHostingService.logger.warning("获取AccessToken失败：" + panEntity.message);
//                }
                accessToken = (String) panEntity.data.get("accessToken");
            } else {
                OraxenHostingService.logger.warning(ACCESS_TOKEN_URL + "请求失败：" + response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.warning("解析json时出错：" + e);
        } catch (IOException e) {
            OraxenHostingService.logger.warning("获取AccessToken时发生错误：" + e);
        }
    }

    @Override
    public void uploadFile(File localFile) {
        if (accessToken == null) {
            getAccessToken();
        }
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        // 构造json
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("parentFileID", parentFileId);
        jsonObject.put("filename", localFile.getName());
        jsonObject.put("etag", getFileMD5(localFile));
        jsonObject.put("size", localFile.length());
        try {
            // 构造body
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(jsonObject));
            // 构造请求
            Request request = new Request.Builder()
                    .url(CREATE_FILE_URL)
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
                if (panEntity.code == 0) {
                    if ((Boolean)panEntity.data.get("reuse")){
                        System.out.println("文件秒传成功");
                    } else {
                        // 分块上传
                        sliceUpload(localFile,
                                (String) panEntity.data.get("preuploadID"),
                                (Integer) panEntity.data.get("sliceSize"));
                    }
                } else {
                    System.out.println("创建文件失败：" + panEntity.message);
                }
            } else {
                OraxenHostingService.logger.warning(CREATE_FILE_URL + "请求失败：" + response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.warning("解析json时出错：" + e);
        } catch (IOException e) {
            OraxenHostingService.logger.warning("创建文件时发生错误：" + e);
        }
    }

    @Override
    public String getFileUrl() {
        if (accessToken == null) {
            getAccessToken();
        }
        return "";
    }

    private String getFileMD5(File file) {
        try {
            // 创建MessageDigest实例，用于计算MD5
            MessageDigest digest = MessageDigest.getInstance("MD5");
            // 读取文件内容并更新到MessageDigest
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
            OraxenHostingService.logger.warning("计算文件md5时发生错误：" + e);
        }
        return null;
    }

    /**
     * 分片上传文件
     * @param file 文件
     * @param preuploadID 预上传ID
     * @param sliceSize 分片大小（Byte）
     */
    private void sliceUpload(File file, String preuploadID, Integer sliceSize) {
        // 获取文件的总字节数
        long fileSize = file.length();
        // 计算总的分片数
        int totalSlices = (int) Math.ceil((double) fileSize / sliceSize);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[sliceSize];
            int bytesRead;
            // 分片索引
            int sliceIndex = 1;
            // 循环读取文件并上传每个分片
            while ((bytesRead = fis.read(buffer)) != -1) {
                // 如果读取的字节数小于缓冲区大小，则需要调整缓冲区大小
                if (bytesRead < sliceSize) {
                    byte[] tempBuffer = new byte[bytesRead];
                    System.arraycopy(buffer, 0, tempBuffer, 0, bytesRead);
                    buffer = tempBuffer;
                }
                // 上传分片
                sliceUpload(buffer, sliceIndex, preuploadID);
                // 更新
                sliceIndex++;
            }
            // 检查上传是否完成
            checkUploadSuccess(preuploadID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传单个分片
     * @param slice 分片数据
     * @param sliceIndex 分片id
     * @param preuploadID 预上传ID
     */
    private void sliceUpload(byte[] slice, int sliceIndex, String preuploadID) {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        // 构造json
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("preuploadID", preuploadID);
        jsonObject.put("sliceNo", sliceIndex);
        try {
            // ----------------------- 1.获取分片上传地址 -----------------------
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(jsonObject));
            Request request = new Request.Builder()
                    .url(GET_UPLOAD_URL)
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                OraxenHostingService.logger.warning(GET_UPLOAD_URL + "请求失败：" + response.code());
                return;
            }
            PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
            String presignedURL = (String) panEntity.data.get("presignedURL");

            // ----------------------- 2.上传分片 -----------------------
            body = RequestBody.create(MEDIA_TYPE_OCTET_STREAM, slice);
            request = new Request.Builder()
                .url(presignedURL)
                .addHeader("Platform", "open_platform")
                .put(body)
                .build();
            response = client.newCall(request).execute();
            System.out.println("upload resp -> " + response.body().string());
            if (!response.isSuccessful()) {
                OraxenHostingService.logger.warning(presignedURL + "请求失败：" + response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.warning("解析json时出错：" + e);
        } catch (IOException e) {
            OraxenHostingService.logger.warning("分片上传文件时发生错误：" + e);
        }
    }

    /**
     * 检查上传是否完成，上传未完成则等待
     * @param preuploadID 预上传ID
     */
    private void checkUploadSuccess(String preuploadID) {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        // 构造json
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("preuploadID", preuploadID);
        try {
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(jsonObject));
            Request request = new Request.Builder()
                    .url(CHECK_COMPLETE_URL)
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            System.out.println("check resp -> " + response.body().string());
            if (!response.isSuccessful()) {
                OraxenHostingService.logger.warning(GET_UPLOAD_URL + "请求失败：" + response.code());
                return;
            }

        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.warning("解析json时出错：" + e);
        } catch (IOException e) {
            OraxenHostingService.logger.warning("分片上传文件时发生错误：" + e);
        }
    }

    public static void main(String[] args) {
        PanClient client = new PanClient();
        client.getAccessToken();
        client.uploadFile(new File("/home/dyh/IdeaProjects/OraxenHostingService/pack.zip"));
    }
}
