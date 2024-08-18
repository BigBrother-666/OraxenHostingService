package com.bilicraft.oraxenhostingservice;

import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;

import java.io.File;
import java.util.UUID;

public class BiliHostingService implements HostingProvider {
    private String sha1 = "e1e33d104cb988d50ef30914b7aebb4138490139";
    @Override
    public boolean uploadPack(File file) {
        return true;
    }

    @Override
    public String getPackURL() {
        return "http://pack-1309360946.cos.ap-beijing.myqcloud.com/pack.zip";
    }

    @Override
    public String getMinecraftPackURL() {
        return "http://pack-1309360946.cos.ap-beijing.myqcloud.com/pack.zip";
    }

    @Override
    public byte[] getSHA1() {
        int len = this.sha1.length();
        byte[] data = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(this.sha1.charAt(i), 16) << 4) + Character.digit(this.sha1.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public String getOriginalSHA1() {
        return sha1;
    }

    @Override
    public UUID getPackUUID() {
        return UUID.fromString("ffe4e17b-591c-1807-0a5a-d49fc0824b11");
    }


}
