package com.bilicraft.oraxenhostingservice.command;

import com.bilicraft.oraxenhostingservice.BiliOraxenHostingService;
import com.bilicraft.oraxenhostingservice.OraxenHostingService;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandReload implements CommandExecutor {
    private final OraxenHostingService plugin;

    public CommandReload(OraxenHostingService plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length > 0 && args[0].equals("reload")) {    // 重载插件的配置文件
            if (!commandSender.hasPermission("ohs.command.reload")){
                commandSender.sendMessage("您没有权限执行此命令！");
                return false;
            }
            plugin.reloadConfig();
            OraxenHostingService.config = plugin.getConfig();
            plugin.regListener();
            if (OraxenPlugin.get().getUploadManager().getHostingProvider() instanceof BiliOraxenHostingService hostingService) {
                hostingService.loadConfig();
            }
            commandSender.sendMessage("配置文件已重载！");
            return true;
        }
        return false;
    }
}
