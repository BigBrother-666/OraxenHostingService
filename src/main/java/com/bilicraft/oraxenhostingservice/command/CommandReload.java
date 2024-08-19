package com.bilicraft.oraxenhostingservice.command;

import com.bilicraft.oraxenhostingservice.OraxenHostingService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class CommandReload implements CommandExecutor {
    private final JavaPlugin plugin;

    public CommandReload(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length > 0 && args[0].equals("reload")) {    // 重载插件的配置文件
            if (!commandSender.hasPermission("ohs.command.reload")){
                commandSender.sendMessage("您没有权限执行此命令！");
            }
            plugin.reloadConfig();
            OraxenHostingService.config = plugin.getConfig();
            commandSender.sendMessage("配置文件已重载！");
            return true;
        }
//        commandSender.sendMessage("格式错误，请使用 /ohs reload 重载配置文件");
        return false;
    }
}
