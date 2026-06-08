package com.bigbrother.resourcepackhostingservice.command;

import com.bigbrother.resourcepackhostingservice.OraxenHostingService;
import com.bigbrother.resourcepackhostingservice.ResourcePackHostingService;
import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BaseCommand implements TabExecutor {
    private final ResourcePackHostingService plugin;

    public BaseCommand(ResourcePackHostingService plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equals("reload")) {    // 重载插件的配置文件
            if (!sender.hasPermission("rphs.command.reload")) {
                sender.sendMessage("您没有权限执行此命令！");
                return false;
            }
            plugin.reload();
            sender.sendMessage("配置文件已重载！");
            return true;
        } else if (args[0].equals("packurl")) {
            if (!sender.hasPermission("rphs.command.packurl")) {
                sender.sendMessage("您没有权限执行此命令！");
                return false;
            }

            if (OraxenPlugin.get().getUploadManager().getHostingProvider() instanceof OraxenHostingService hostingService) {
                sender.sendMessage(
                        MiniMessage.miniMessage().deserialize(ResourcePackHostingService.messages.getString("download-pack-button", "<green><underlined><click:open_url:%s>[点击下载最新资源包]").formatted(hostingService.getPackURL()))
                );
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender.hasPermission("rphs.command.reload")) {
            completions.add("reload");
        }
        if (sender.hasPermission("rphs.command.packurl")) {
            completions.add("packurl");
        }
        return completions;
    }
}
