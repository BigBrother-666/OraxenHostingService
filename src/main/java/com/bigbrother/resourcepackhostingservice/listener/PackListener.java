package com.bigbrother.resourcepackhostingservice.listener;

import com.bigbrother.resourcepackhostingservice.OraxenHostingService;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.upload.UploadManager;
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
                // 重发pack
                if (uploadManager.getHostingProvider() instanceof OraxenHostingService hostingProvider) {
                    UUID uuid = event.getPlayer().getUniqueId();
                    // 所有provider都尝试过了
                    if (currProviderMap.containsKey(uuid) && currProviderMap.get(uuid) + 1 >= hostingProvider.getEnabledStorageProvider().size()) {
                        currProviderMap.remove(uuid);
                        return;
                    }

                    // 1. 切换 provider
                    int providerIdx = hostingProvider.setProvider(currProviderMap.getOrDefault(uuid, 0) + 1);
                    currProviderMap.put(uuid, providerIdx);

                    // 2. 发送资源包
                    uploadManager.getSender().sendPack(event.getPlayer());

                    // 3. 恢复默认 provider
                    hostingProvider.setProvider(0);
                }
                break;
        }
    }

    public void disableListener() {
        PlayerResourcePackStatusEvent.getHandlerList().unregister(this);
    }
}
