package com.hezaerd.memoryleak.bungee;

import com.hezaerd.memoryleak.core.LeakCommands;
import com.hezaerd.memoryleak.core.LeakManager;
import com.hezaerd.memoryleak.core.Reply;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public final class MemoryLeakPlugin extends Plugin {
    private final LeakManager leak = LeakManager.INSTANCE;
    private ScheduledTask task;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new LeakCommand());
        task = getProxy().getScheduler().schedule(this, leak::tick, 50, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onDisable() {
        task.cancel();
        leak.clear();
    }

    private final class LeakCommand extends Command implements TabExecutor {
        LeakCommand() {
            super(LeakCommands.NAME, LeakCommands.PERMISSION);
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            Reply reply = LeakCommands.execute(leak, args);
            TextComponent message = new TextComponent(reply.text());
            message.setColor(color(reply.tone()));
            sender.sendMessage(message);
            if (reply.notifyAdmins() && sender != getProxy().getConsole()) {
                getLogger().info(reply.text());
            }
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return LeakCommands.complete(args);
        }
    }

    private static ChatColor color(Reply.Tone tone) {
        switch (tone) {
            case SUCCESS: return ChatColor.GREEN;
            case WARNING: return ChatColor.YELLOW;
            case ERROR: return ChatColor.RED;
            default: return ChatColor.AQUA;
        }
    }
}
