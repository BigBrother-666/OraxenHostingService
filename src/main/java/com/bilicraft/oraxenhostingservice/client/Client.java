package com.bilicraft.oraxenhostingservice.client;

import java.io.File;

public interface Client {
    void uploadFile(File localFile);
    String getFileUrl();
}
