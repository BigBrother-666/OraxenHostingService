package com.bigbrother.resourcepackhostingservice;

import com.bigbrother.resourcepackhostingservice.listener.PackListener;
import com.bigbrother.resourcepackhostingservice.provider.StorageProvider;
import com.bigbrother.resourcepackhostingservice.command.CommandReload;
import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class ResourcePackHostingService extends JavaPlugin {
    public static FileConfiguration config;
    public static ComponentLogger logger;
    private PackListener packListener;

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
        // 注册监听器
        regListener();
    }

    public void regListener() {
        if (this.packListener != null) {
            packListener.disableListener();
        }
        packListener = new PackListener();
        this.getServer().getPluginManager().registerEvents(packListener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (OraxenPlugin.get().getUploadManager().getHostingProvider() instanceof OraxenHostingService biliOraxenHostingService) {
            biliOraxenHostingService.getEnabledStorageProvider().forEach((StorageProvider::close));
        }
    }
}
