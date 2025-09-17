package com.pythoncraft.collector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import com.pythoncraft.collector.command.CollectorCommand;
import com.pythoncraft.collector.command.CollectorTabCompleter;
import com.pythoncraft.gamelib.Chat;
import com.pythoncraft.gamelib.GameLib;
import com.pythoncraft.gamelib.Logger;
import com.pythoncraft.gamelib.Score;
import com.pythoncraft.gamelib.Timer;


public class PluginMain extends JavaPlugin implements Listener {

    static PluginMain instance;
    public static PluginMain getInstance() { return instance; }

    public static int defaultTime = 15 * 60; // in seconds
    public static int interval;

    public static HashMap<Attribute, Double> attributes = new HashMap<>();

    private File configFile;
    private FileConfiguration config;

    public static int gameType = 0; // 0 = items, 1 = deaths

    public static int currentGame = -1;
    public static int nextGame = 0;
    public static int gap = 6000;
    public static int prepareTime = 5;
    public static HashSet<String> avoidedBiomes = new HashSet<>();

    public static BossBar bossBar;
    public Score objective;

    public static World world;
    public static boolean gameRunning = false;
    public static boolean preparing = false;
    public static HashSet<Player> playersInGame = new HashSet<>();

    public static HashMap<Player, HashSet<Material>> items = new HashMap<>();
    public static HashMap<Player, HashSet<String>> deaths = new HashMap<>();
    public static HashMap<Player, Integer> scores = new HashMap<>();

    public Timer timer;

    @Override
    public void onEnable() {
        instance = this;

        Bukkit.getPluginManager().registerEvents(this, this);

        this.configFile = new File(getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(this.configFile);

        world = Bukkit.getWorld("world");

        this.loadConfig();

        getLogger().info("Collector plugin enabled!");

        this.getCommand("collector").setExecutor(new CollectorCommand());
        this.getCommand("collector").setTabCompleter(new CollectorTabCompleter());

        attributes.put(Attribute.MOVEMENT_SPEED, 0.1);
        attributes.put(Attribute.ATTACK_DAMAGE, 1.0);
        attributes.put(Attribute.JUMP_STRENGTH, 0.42);
    }

    @Override
    public void onDisable() {
        stopGame();
    }

    public void startGame(int time) {
        getLogger().log(Level.INFO, "Starting Collector with interval {0} seconds.", time);
        if (world == null) {return;}

        currentGame = nextGame;

        nextGame = findNextGame(currentGame + 1);
        int x = nextGame * gap;
        int z = 0;
        int y = world.getHighestBlockYAt(x, z);

        config.set("last-game", currentGame);
        try {config.save(configFile);} catch (IOException e) {e.printStackTrace();}

        if (world.getBlockAt(x, y, z).getType().equals(Material.WATER) && world.getBlockAt(x, y - 1, z).getType().equals(Material.WATER)) {
            world.getBlockAt(x, y + 1, z).setType(Material.LILY_PAD);
        }

        playersInGame.clear();
        items.clear();
        deaths.clear();
        scores.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * prepareTime, 0, false, false));
            p.setGameMode(GameMode.ADVENTURE);
            for (Attribute attribute : attributes.keySet()) {p.getAttribute(attribute).setBaseValue(0);}
            
            Location spawn = new Location(world, x + 0.5, y + 1.09375, z + 0.5);
            p.teleport(spawn);
            p.setRespawnLocation(spawn, true);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 0, false, false));

            Inventory i = p.getInventory();
            i.clear();
            i.addItem(getItemStack(Material.COOKED_PORKCHOP, 64));

            playersInGame.add(p);
        }

        this.timer = new Timer(prepareTime * 20, 20, (i) -> {
            // PREPARATION STARTED
            for (Player p : playersInGame) {
                p.sendActionBar(Chat.c("§a§l" + i));
                if (i <= 3) {p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);}
            }
        }, () -> {
            // GAME STARTED
            preparing = false;
            gameRunning = true;

            this.objective = new Score("collector", "§a§lCollector");
            this.objective.resetScores();
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            PluginMain.instance.timer = new Timer(time * 20, 20, (i1) -> {
                // GAME TICK
                int q = i1 % time;
                PluginMain.bossBar.setProgress(1 - (q / (double) time));

                if (gameType != 0) {return;}

                for (Player p : playersInGame) {
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item == null) {continue;}
                        items.putIfAbsent(p, new HashSet<>());
                        if (!items.get(p).contains(item.getType())) {
                            items.get(p).add(item.getType());
                            scores.put(p, items.get(p).size());
                            this.objective.setScore(p.getName(), scores.get(p));
                        }
                    }
                }
            }, () -> {
                // GAME ENDED
                for (Player p : playersInGame) {
                    p.sendMessage("");
                    p.sendMessage(Chat.c("§c§lTime is up!"));
                    p.getInventory().clear();
                    p.setGameMode(GameMode.SPECTATOR);
                    p.clearActivePotionEffects();
                    for (Attribute attribute : attributes.keySet()) {p.getAttribute(attribute).setBaseValue(attributes.get(attribute));}

                    p.sendMessage(Chat.c("Results:"));
                    List<Player> sorted = scores.keySet().stream().sorted((a, b) -> Integer.compare(scores.get(b), scores.get(a))).toList();;
                    for (int j = 0; j < Math.min(5, items.size()); j++) {
                        Player pl = sorted.get(j);
                        int count = scores.get(pl);
                        if (gameType == 1) {
                            p.sendMessage(Chat.c((j + 1) + ". §e§l" + pl.getName() + "§r: §a§l" + count + "§r unique deaths"));
                        } else {
                            p.sendMessage(Chat.c((j + 1) + ". §e§l" + pl.getName() + "§r: §a§l" + count + "§r unique items"));
                        }
                    }
                }

                stopGame();
            });

            PluginMain.instance.timer.start();

            for (Player p : playersInGame) {
                p.sendActionBar(Chat.c("§c§lGO!"));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
                for (Attribute attribute : attributes.keySet()) {
                    p.getAttribute(attribute).setBaseValue(attributes.get(attribute));
                }
                p.setGameMode(GameMode.SURVIVAL);
            }
        });

        Timer.after(prepareTime * 20 + 200, () -> {
            GameLib.forceLoadChunkStop(world, currentGame * gap, 0, 2);
            GameLib.forceLoadChunk(world, nextGame * gap, 0, 2);
        });

        preparing = true;
        gameRunning = false;
        bossBar = setupBossbar();
        bossBar.setVisible(true);
        this.timer.start();
    }

    public int findNextGame(int start) {
        int g = start;
        while (!isSafe(0, world.getHighestBlockYAt(0, g * gap), g * gap)) {g++;}
        return g;
    }

    public static ItemStack getItemStack(Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        item.setAmount(amount);
        return item;
    }

    public void stopGame() {
        getInstance().getLogger().log(Level.INFO, "Stopping Collector.");
        gameRunning = false;
        preparing = false;

        this.objective.setDisplaySlot(null);
        this.objective.unregister();
        this.objective = null;

        if (this.timer != null) {this.timer.cancel();}
        if (bossBar != null) {bossBar.removeAll();}
    }

    public static boolean isSafe(int x, int y, int z) {
        if (avoidedBiomes.isEmpty()) {return true;}

        for (String biome : avoidedBiomes) {
            if (world.getBiome(x, y, z).toString().toUpperCase().contains(biome)) {
                return false;
            }
        }

        return true;
    }

    public static BossBar setupBossbar() {
        BossBar bar = Bukkit.createBossBar(Chat.c("§a§lCollector"), BarColor.GREEN, BarStyle.SOLID);
        bar.setVisible(false);
        bar.setProgress(1);
        for (Player p : Bukkit.getOnlinePlayers()) {bar.addPlayer(p);}

        return bar;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!playersInGame.contains(event.getPlayer())) {return;}
        if (preparing) {
            World w = event.getFrom().getWorld();
            double x = Math.round(event.getFrom().getX() - 0.5) + 0.5;
            double y = event.getTo().getY(); // allow y movement
            double z = Math.round(event.getFrom().getZ() - 0.5) + 0.5;
            float yaw = event.getTo().getYaw();
            float pitch = event.getTo().getPitch();
            event.setTo(new Location(w, x, y, z, yaw, pitch));
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) {return;}

        if (!playersInGame.contains(p)) {return;}
        if (preparing) {event.setCancelled(true);}
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!playersInGame.contains(event.getPlayer())) {return;}
        if (preparing) {event.setCancelled(true);}
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (bossBar != null) {bossBar.addPlayer(player);}

        if (playersInGame.contains(player)) {return;}
        
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(new Location(world, 0.5, 100, currentGame * gap + 0.5));
        player.clearActivePotionEffects();
        for (Attribute attribute : attributes.keySet()) {player.getAttribute(attribute).setBaseValue(attributes.get(attribute));}
        player.getInventory().clear();

        if (gameRunning) {
            player.sendMessage(Chat.c("§c§lA game is currently running. You are in spectator mode."));
            player.sendMessage("You will be teleported to the next game when it starts.");
        } else if (preparing) {
            player.sendMessage(Chat.c("§c§lA game is currently being prepared. You are in spectator mode."));
            player.sendMessage("You will be teleported to the next game when it starts.");
        } else {
            player.sendMessage(Chat.c("§a§lNo game is currently running."));
            player.sendMessage("You can start a new game with §e/collector items§r or §e/collector deaths§r.");
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        bossBar.removePlayer(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!playersInGame.contains(player)) {return;}
        if (!gameRunning) {return;}
        if (gameType != 1) {return;}

        String deathMessage = event.getDeathMessage();
        if (deathMessage != null) {
            deaths.putIfAbsent(player, new HashSet<>());
            if (!deaths.get(player).contains(deathMessage)) {
                deaths.get(player).add(deathMessage);
                scores.put(player, deaths.get(player).size());
                this.objective.setScore(player.getName(), scores.get(player));
            }
        }
    }

    private void loadConfig() {
        gap = this.config.getInt("gap", gap);
        defaultTime = this.config.getInt("default-time", defaultTime);
        prepareTime = this.config.getInt("prepare-time", prepareTime);
        currentGame = this.config.getInt("last-game", currentGame);
        currentGame = findNextGame(currentGame + 1);

        avoidedBiomes.clear();
        for (String biome : this.config.getStringList("avoided-biomes")) {
            avoidedBiomes.add(biome.toUpperCase());
        }
    }
}