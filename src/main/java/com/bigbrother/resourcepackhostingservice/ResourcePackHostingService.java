package com.bigbrother.resourcepackhostingservice;

import com.bigbrother.resourcepackhostingservice.listener.PackListener;
import com.bigbrother.resourcepackhostingservice.provider.StorageProvider;
import com.bigbrother.resourcepackhostingservice.command.BaseCommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class ResourcePackHostingService extends JavaPlugin {
    public static FileConfiguration config;
    public static FileConfiguration messages;
    public static ComponentLogger logger;
    private PackListener packListener;

    @Override
    public void onEnable() {
        // Plugin startup logic
        // 初始化变量
        logger = this.getComponentLogger();

        // 生成配置文件
        saveResource("config.yml", /* replace */ false);
        saveResource("messages.yml", /* replace */ false);

        // 加载配置
        this.reload();

        // 注册指令
        Objects.requireNonNull(this.getCommand("basecommand")).setExecutor(new BaseCommand(this));
    }

    public void regListener() {
        if (this.packListener != null) {
            packListener.disableListener();
        }
        packListener = new PackListener();
        this.getServer().getPluginManager().registerEvents(packListener, this);
    }

    public void reload() {
        // 加载配置
        config = this.getConfig();
        messages = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "messages.yml"));
        UploadManager uploadManager = OraxenPlugin.get().getUploadManager();
        if (uploadManager != null && uploadManager.getHostingProvider() instanceof OraxenHostingService hostingService) {
            hostingService.loadConfig();
        }
        // 注册监听器
        this.regListener();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (OraxenPlugin.get().getUploadManager().getHostingProvider() instanceof OraxenHostingService biliOraxenHostingService) {
            biliOraxenHostingService.getEnabledStorageProvider().forEach((StorageProvider::close));
        }
    }
}
