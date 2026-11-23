package com.hezaerd.memoryleak.bukkit;

import com.hezaerd.memoryleak.core.LeakCommands;
import com.hezaerd.memoryleak.core.LeakManager;
import com.hezaerd.memoryleak.core.LeakStatus;
import com.hezaerd.memoryleak.core.Reply;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MemoryLeakPlugin extends JavaPlugin {
    private final LeakManager leak = LeakManager.INSTANCE;
    private BossBar bar;

    @Override
    public void onEnable() {
        bar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        if (isFolia()) {
            try {
                Class<?> schedulerType = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Consumer<Object> task = ignored -> tick();
                schedulerType.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class)
                        .invoke(scheduler, this, task, 1L, 1L);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not schedule Memory Leak on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        }
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false,
                    MemoryLeakPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void onDisable() {
        if (bar != null) {
            bar.removeAll();
        }
        leak.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Reply reply = LeakCommands.execute(leak, args);
        sender.sendMessage(color(reply.tone()) + reply.text());
        if (reply.notifyAdmins() && !(sender instanceof ConsoleCommandSender)) {
            getLogger().info(reply.text());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return LeakCommands.complete(args);
    }

    private void tick() {
        LeakStatus status = leak.tick();
        if (!status.leaking()) {
            if (!bar.getPlayers().isEmpty()) {
                bar.removeAll();
            }
            return;
        }

        bar.setTitle(status.barTitle());
        bar.setColor(BarColor.valueOf(status.barColor().name()));
        bar.setProgress(status.progress());
        List<Player> viewers = bar.getPlayers();
        for (Player player : viewers) {
            if (!player.isOnline() || !player.hasPermission(LeakCommands.PERMISSION)) {
                bar.removePlayer(player);
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!viewers.contains(player) && player.hasPermission(LeakCommands.PERMISSION)) {
                bar.addPlayer(player);
            }
        }
    }

    private static ChatColor color(Reply.Tone tone) {
        switch (tone) {
            case SUCCESS: return ChatColor.GREEN;
            case WARNING: return ChatColor.YELLOW;
            case ERROR: return ChatColor.RED;
            default: return ChatColor.GRAY;
        }
    }
}
