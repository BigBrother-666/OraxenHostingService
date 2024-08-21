package com.bilicraft.oraxenhostingservice;

import com.bilicraft.oraxenhostingservice.command.CommandReload;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class OraxenHostingService extends JavaPlugin {
    public static FileConfiguration config;
    public static ComponentLogger logger;

    @Override
    public void onEnable() {
        // Plugin startup logic
        // 初始化变量
        logger = this.getComponentLogger();
        // 生成配置文件
        saveResource("config.yml", /* replace */ false);
        // 获取配置文件
        config = this.getConfig();
        logger.info("加载配置文件成功！");
        // 注册指令
        Objects.requireNonNull(this.getCommand("reloadconfig")).setExecutor(new CommandReload(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
