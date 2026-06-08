package com.bigbrother.resourcepackhostingservice.listener;

import com.bigbrother.resourcepackhostingservice.OraxenHostingService;
import com.bigbrother.resourcepackhostingservice.ResourcePackHostingService;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PackListener implements Listener {
    private final Map<UUID, Integer> currProviderMap;

    public PackListener() {
        this.currProviderMap = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        UploadManager uploadManager = OraxenPlugin.get().getUploadManager();
        switch (event.getStatus()) {
            case INVALID_URL:
            case FAILED_DOWNLOAD:
            case FAILED_RELOAD:
                // 重发pack
                if (uploadManager.getHostingProvider() instanceof OraxenHostingService hostingService) {
                    UUID uuid = event.getPlayer().getUniqueId();
                    // 所有provider都尝试过了
                    if (currProviderMap.containsKey(uuid) && currProviderMap.get(uuid) + 1 >= hostingService.getEnabledStorageProvider().size()) {
                        currProviderMap.remove(uuid);
                        // 提供手动下载url
                        event.getPlayer().sendMessage(
                                MiniMessage.miniMessage().deserialize(ResourcePackHostingService.messages.getString("detect-pack-fail", "<yellow>检测到资源包下载/加载失败，请手动下载安装资源包"))
                        );
                        event.getPlayer().sendMessage(
                                MiniMessage.miniMessage().deserialize(ResourcePackHostingService.messages.getString("download-pack-button", "<green><underlined><click:open_url:%s>[点击下载最新资源包]").formatted(hostingService.getPackURL()))
                        );
                        return;
                    }

                    // 1. 切换 provider
                    int providerIdx = hostingService.setProvider(currProviderMap.getOrDefault(uuid, 0) + 1);
                    currProviderMap.put(uuid, providerIdx);

                    // 2. 发送资源包
                    uploadManager.getSender().sendPack(event.getPlayer());

                    // 3. 恢复默认 provider
                    hostingService.setProvider(0);
                }
                break;
        }
    }

    public void disableListener() {
        PlayerResourcePackStatusEvent.getHandlerList().unregister(this);
    }
}
