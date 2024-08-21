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
import java.util.*;

public class PanClient implements Client {
    private final String clientId = OraxenHostingService.config.getString("pan.client-id");
    private final String clientSecret = OraxenHostingService.config.getString("pan.client-secret");
    private final Integer parentFileId = OraxenHostingService.config.getInt("pan.parent-file-id");;
    private Integer fileId = null;
    private String accessToken = null;

    private static final String BASE_URL = OraxenHostingService.config.getString("pan.base-url");
    private final String ACCESS_TOKEN_URL = BASE_URL + "/api/v1/access_token";
//    private final String CREATE_DIR_URL = BASE_URL + "/upload/v1/file/mkdir";
    private final String CREATE_FILE_URL = BASE_URL + "/upload/v1/file/create";
    private final String GET_UPLOAD_URL = BASE_URL + "/upload/v1/file/get_upload_url";
    private final String CHECK_COMPLETE_URL = BASE_URL + "/upload/v1/file/upload_complete";
    private final String GET_FILE_LIST_URL = BASE_URL + "/api/v2/file/list";
    private final String CHECK_COMPLETE_ASYNC_URL = BASE_URL + "/upload/v1/file/upload_async_result";
    private final String TRASH_URL = BASE_URL + "/api/v1/file/trash";
    private final String DELETE_URL = BASE_URL + "/api/v1/file/delete";
    private final String DIRECT_LINK_URL = BASE_URL + "/api/v1/direct-link/url";

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
                if (panEntity.code != 0) {
                    OraxenHostingService.logger.error("获取AccessToken失败：{}", panEntity.message);
                }
                accessToken = (String) panEntity.data.get("accessToken");
            } else {
                OraxenHostingService.logger.error("{}请求失败：{}", ACCESS_TOKEN_URL, response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException e) {
            OraxenHostingService.logger.error("获取AccessToken时发生错误：{}", String.valueOf(e));
        }
    }

    @Override
    public void uploadFile(File localFile) {
        if (accessToken == null) {
            getAccessToken();
        }
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // --------------------------- 查找是否有同名资源包 ---------------------------
            getFileId(localFile.getName());

            // --------------------------- 如果有，彻底删除同名资源包 ---------------------------
            if (fileId != null) {
                deleteFile();
            }

            // --------------------------- 远程创建资源包 ---------------------------
            Map<String, Object> jsonObject = new HashMap<>();
            jsonObject.put("parentFileID", parentFileId);
            jsonObject.put("filename", localFile.getName());
            jsonObject.put("etag", getFileMD5(localFile));
            jsonObject.put("size", localFile.length());
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(jsonObject));
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
                    if ((Boolean) panEntity.data.get("reuse")) {
                        OraxenHostingService.logger.info("资源包秒传成功");
                        fileId = (Integer) panEntity.data.get("fileID");
                    } else {
                        // 分块上传
                        sliceUpload(localFile,
                                (String) panEntity.data.get("preuploadID"),
                                (Integer) panEntity.data.get("sliceSize"));
                    }
                } else {
                    OraxenHostingService.logger.error("创建资源包失败：{}", panEntity.message);
                }
            } else {
                OraxenHostingService.logger.error("{}请求失败：{}", CREATE_FILE_URL, response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException e) {
            OraxenHostingService.logger.error("远程创建资源包时发生错误：{}", String.valueOf(e));
        }
    }

    @Override
    public String getFileUrl() {
        if (accessToken == null) {
            getAccessToken();
        }
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(DIRECT_LINK_URL).newBuilder();
            urlBuilder.addQueryParameter("fileID", fileId.toString());
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
                if (panEntity.code == 0) {
                    return (String) panEntity.data.get("url");
                } else {
                    OraxenHostingService.logger.error("获取直链失败：{}", panEntity.message);
                }
            } else {
                OraxenHostingService.logger.error("{}请求失败：{}", urlBuilder.build(), response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException e) {
            OraxenHostingService.logger.error("获取直链时发生错误：{}", String.valueOf(e));
        }
        return null;
    }

    /**
     * 删除云盘中的资源包
     */
    private void deleteFile() {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("fileIDs", new int[]{fileId});
        try {
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(jsonObject));
            Request request = new Request.Builder()
                    .url(TRASH_URL)
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            // 放入回收站
            if (response.isSuccessful()) {
                PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
                if (panEntity.code != 0) {
                    OraxenHostingService.logger.error("删除旧资源包失败：{}", panEntity.message);
                }
            } else {
                OraxenHostingService.logger.error("{}请求失败：{}", TRASH_URL, response.code());
            }
            // 彻底删除
            request = new Request.Builder()
                    .url(DELETE_URL)
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
                if (panEntity.code == 0) {
                    OraxenHostingService.logger.info("删除旧资源包成功");
                } else {
                    OraxenHostingService.logger.error("删除旧资源包失败：{}", panEntity.message);
                }
            } else {
                OraxenHostingService.logger.error("{}请求失败：{}", DELETE_URL, response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException e) {
            OraxenHostingService.logger.error("删除文件时发生错误：{}", String.valueOf(e));
        }


    }

    /**
     * 查询云盘资源包信息
     *
     * @param fileName 云盘资源包文件名
     */
    private void getFileId(String fileName) {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(GET_FILE_LIST_URL).newBuilder();
            urlBuilder.addQueryParameter("parentFileId", parentFileId.toString());
            urlBuilder.addQueryParameter("limit", "100");
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
                if (panEntity.code == 0) {
                    ArrayList<LinkedHashMap<String, Object>> fileList = (ArrayList<LinkedHashMap<String, Object>>) panEntity.data.get("fileList");
                    for (LinkedHashMap<String, Object> file : fileList) {
                        if (Objects.equals(file.get("filename"), fileName) && (Integer) file.get("trashed") == 0) {
                            fileId = (Integer) file.get("fileId");
                            break;
                        }
                    }
                } else {
                    OraxenHostingService.logger.error("查询远程资源包失败：{}", panEntity.message);
                }
            } else {
                OraxenHostingService.logger.error("{}请求失败：{}", urlBuilder.build(), response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException e) {
            OraxenHostingService.logger.error("获取云盘资源包信息时发生错误：{}", String.valueOf(e));
        }
    }

    private String getFileMD5(File file) {
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
            OraxenHostingService.logger.error("计算资源包md5时发生错误：{}", String.valueOf(e));
        }
        return null;
    }

    /**
     * 分片上传资源包
     *
     * @param file        资源包
     * @param preuploadID 预上传ID
     * @param sliceSize   分片大小（Byte）
     */
    private void sliceUpload(File file, String preuploadID, Integer sliceSize) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[sliceSize];
            int bytesRead;
            // 分片索引
            int sliceIndex = 1;
            // 循环读取资源包并上传每个分片
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
            OraxenHostingService.logger.error("资源包分片时发生错误：{}", String.valueOf(e));
        }
    }

    /**
     * 上传单个分片
     *
     * @param slice       分片数据
     * @param sliceIndex  分片id
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
                OraxenHostingService.logger.error("{}请求失败：{}", GET_UPLOAD_URL, response.code());
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
            if (!response.isSuccessful()) {
                OraxenHostingService.logger.error("{}请求失败：{}", presignedURL, response.code());
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException e) {
            OraxenHostingService.logger.error("分片上传资源包时发生错误：{}", String.valueOf(e));
        }
    }

    /**
     * 检查上传是否完成，上传未完成则等待
     *
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
            PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
            if (!response.isSuccessful()) {
                OraxenHostingService.logger.error("{}请求失败：{}", CHECK_COMPLETE_URL, response.code());
            } else if (panEntity.code == 0) {
                if ((Boolean) panEntity.data.get("completed")) {
                    fileId = (Integer) panEntity.data.get("fileID");
                    OraxenHostingService.logger.info("成功上传资源包到云盘");
                } else {
                    // 资源包还未上传完成，需要异步查询
                    checkUploadSuccessAsync(preuploadID);
                }
            } else {
                OraxenHostingService.logger.error("分片上传资源包失败：{}", panEntity.message);
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException e) {
            OraxenHostingService.logger.error("检查上传结果时发生错误：{}", String.valueOf(e));
        }
    }

    private void checkUploadSuccessAsync(String preuploadID) {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();
        // 构造json
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("preuploadID", preuploadID);
        try {
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, objectMapper.writeValueAsString(jsonObject));
            Request request = new Request.Builder()
                    .url(CHECK_COMPLETE_ASYNC_URL)
                    .addHeader("Platform", "open_platform")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();
            // 最大轮询次数
            int maxCnt = OraxenHostingService.config.getInt("pan.upload-time-out");
            while (maxCnt > 0) {
                Response response = client.newCall(request).execute();
                PanEntity panEntity = objectMapper.readValue(response.body().string(), PanEntity.class);
                if (panEntity.code == 0 && (Boolean) panEntity.data.get("completed")) {
                    fileId = (Integer) panEntity.data.get("fileID");
                    OraxenHostingService.logger.info("成功上传资源包到云盘");
                    break;
                } else {
                    // 资源包还未上传完成，继续轮询
                    Thread.sleep(1000);
                }
                maxCnt--;
            }
            if (maxCnt == 0) {
                OraxenHostingService.logger.warn("上传资源包超时");
            }
        } catch (JsonProcessingException e) {
            OraxenHostingService.logger.error("解析json时出错：{}", String.valueOf(e));
        } catch (IOException | InterruptedException e) {
            OraxenHostingService.logger.error("异步检查上传结果时发生错误：{}", String.valueOf(e));
        }
    }
}
