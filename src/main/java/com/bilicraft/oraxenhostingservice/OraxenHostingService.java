package com.bilicraft.oraxenhostingservice;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class OraxenHostingService extends JavaPlugin {
    public static FileConfiguration config;
    public static Logger logger;

    @Override
    public void onEnable() {
        // Plugin startup logic
        // 获取logger
        logger = getLogger();
        // 生成配置文件
        saveResource("config.yml", /* replace */ false);
        // 获取配置文件
        config = this.getConfig();
        logger.info("加载配置文件成功！");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
