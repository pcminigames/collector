package com.pythoncraft.collector.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.pythoncraft.collector.PluginMain;
import com.pythoncraft.gamelib.Chat;

public class CollectorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null || !(sender instanceof Player)) {return false;}

        Player player = (Player) sender;

        if (args.length > 2) {return false;}

        int time = PluginMain.defaultTime;
        int gameType = 0;

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("deaths")) {
                gameType = 1;
            } else if (args[0].equalsIgnoreCase("items")) {
                gameType = 0;
            } else if (args[0].equalsIgnoreCase("stop")) {
                if (!PluginMain.gameRunning) {player.sendMessage(Chat.c("§c§lNo game is currently running.")); return true;}
                PluginMain.getInstance().stopGame();
                player.sendMessage(Chat.c("§a§lGame stopped."));
                return true;
            } else if (!args[0].matches("\\d+") && !args[0].equalsIgnoreCase("stop")) {
                player.sendMessage(Chat.c("§c§lInvalid game type. Use either 'items' or 'deaths'."));
                return true;
            }
        }

        if (args.length == 2) {
            if (!args[1].matches("\\d+")) {
                player.sendMessage(Chat.c("§c§lInvalid time format. Please enter a number."));
                return true;
            }

            time = Integer.parseInt(args[1]);
        }

        if (PluginMain.preparing) {player.sendMessage(Chat.c("§c§lA game is already being prepared.")); return true;}
        if (PluginMain.gameRunning) {PluginMain.getInstance().stopGame();}
        PluginMain.gameType = gameType;
        PluginMain.getInstance().startGame(time);

        return true;
    }
}