package com.pythoncraft.collector.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CollectorTabCompleter implements TabCompleter {
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {return completions;}

        if (args.length == 1) {
            completions.add("items");
            completions.add("deaths");
            completions.add("stop");
        }

        if (args.length == 2) {
            completions.add("300");
            completions.add("600");
            completions.add("900");
            completions.add("1200");
        }

        return completions;
    }
}