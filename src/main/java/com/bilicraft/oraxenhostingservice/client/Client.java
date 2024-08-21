package com.bilicraft.oraxenhostingservice.client;

import java.io.File;

public interface Client {
    /**
     * 上传资源包
     * @param localFile 资源包文件
     */
    void uploadFile(File localFile);

    /**
     * 获取资源包直链
     * @return 直链链接
     */
    String getFileUrl();
}
