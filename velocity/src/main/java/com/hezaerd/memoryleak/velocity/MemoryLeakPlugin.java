package com.hezaerd.memoryleak.velocity;

import com.google.inject.Inject;
import com.hezaerd.memoryleak.core.LeakCommands;
import com.hezaerd.memoryleak.core.LeakManager;
import com.hezaerd.memoryleak.core.LeakStatus;
import com.hezaerd.memoryleak.core.Reply;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

@Plugin(id = "memoryleak", name = "Memory Leak", version = "${version}")
public final class MemoryLeakPlugin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final LeakManager leak = LeakManager.INSTANCE;
    private final BossBar bar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private final Set<Player> viewers = new HashSet<>();
    private ScheduledTask task;

    @Inject
    public MemoryLeakPlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public synchronized void onInitialize(ProxyInitializeEvent event) {
        var commands = proxy.getCommandManager();
        commands.register(commands.metaBuilder(LeakCommands.NAME).plugin(this).build(), new SimpleCommand() {
            @Override
            public boolean hasPermission(Invocation invocation) {
                return invocation.source().hasPermission(LeakCommands.PERMISSION);
            }

            @Override
            public void execute(Invocation invocation) {
                Reply reply = LeakCommands.execute(leak, invocation.arguments());
                invocation.source().sendMessage(Component.text(reply.text(), color(reply.tone())));
                if (reply.notifyAdmins() && invocation.source() != proxy.getConsoleCommandSource()) {
                    logger.info(reply.text());
                }
            }

            @Override
            public List<String> suggest(Invocation invocation) {
                return LeakCommands.complete(invocation.arguments());
            }
        });
        task = proxy.getScheduler().buildTask(this, this::tick).repeat(Duration.ofMillis(50)).schedule();
    }

    @Subscribe
    public synchronized void onShutdown(ProxyShutdownEvent event) {
        task.cancel();
        task = null;
        hideBar();
        leak.clear();
    }

    private synchronized void tick() {
        if (task == null) {
            return;
        }
        LeakStatus status = leak.tick();
        if (!status.leaking()) {
            hideBar();
            return;
        }

        bar.name(Component.text(status.barTitle()));
        bar.color(BossBar.Color.valueOf(status.barColor().name()));
        bar.progress((float) status.progress());
        viewers.removeIf(player -> {
            if (player.isActive() && player.hasPermission(LeakCommands.PERMISSION)) {
                return false;
            }
            player.hideBossBar(bar);
            return true;
        });
        for (Player player : proxy.getAllPlayers()) {
            if (player.hasPermission(LeakCommands.PERMISSION) && viewers.add(player)) {
                player.showBossBar(bar);
            }
        }
    }

    private void hideBar() {
        viewers.forEach(player -> player.hideBossBar(bar));
        viewers.clear();
    }

    private static NamedTextColor color(Reply.Tone tone) {
        return switch (tone) {
            case SUCCESS -> NamedTextColor.GREEN;
            case WARNING -> NamedTextColor.YELLOW;
            case INFO -> NamedTextColor.GRAY;
            case ERROR -> NamedTextColor.RED;
        };
    }
}
