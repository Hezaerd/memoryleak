package com.hezaerd.memoryleak.sponge;

import com.google.inject.Inject;
import com.hezaerd.memoryleak.core.LeakCommands;
import com.hezaerd.memoryleak.core.LeakManager;
import com.hezaerd.memoryleak.core.LeakStatus;
import com.hezaerd.memoryleak.core.Reply;
import java.util.HashSet;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.manager.CommandFailedRegistrationException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("memoryleak")
public final class MemoryLeakPlugin {
    private final PluginContainer container;
    private final Logger logger;
    private final LeakManager leak = LeakManager.INSTANCE;
    private final BossBar bar = BossBar.bossBar(Component.empty(), 0, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private final Set<ServerPlayer> viewers = new HashSet<>();
    private ScheduledTask task;

    @Inject
    public MemoryLeakPlugin(PluginContainer container, Logger logger) {
        this.container = container;
        this.logger = logger;
    }

    @Listener
    public void onRegisterCommands(RegisterCommandEvent<Command.Parameterized> event)
            throws CommandFailedRegistrationException {
        var args = Parameter.remainingJoinedStrings()
                .key("arguments")
                .optional()
                .completer((context, input) -> LeakCommands.complete(new String[]{input}).stream()
                        .map(CommandCompletion::of).toList())
                .build();
        var command = Command.builder()
                .permission(LeakCommands.PERMISSION)
                .addParameter(args)
                .executor(context -> {
                    String input = context.one(args).orElse("").trim();
                    Reply reply = LeakCommands.execute(leak, input.isEmpty() ? new String[0] : input.split("\\s+"));
                    Component message = Component.text(reply.text(), color(reply.tone()));
                    if (!reply.succeeded()) {
                        return CommandResult.error(message);
                    }
                    context.sendMessage(message);
                    if (reply.notifyAdmins() && context.cause().subject() != Sponge.systemSubject()) {
                        logger.info(reply.text());
                    }
                    return CommandResult.success();
                })
                .build();
        event.register(container, command, LeakCommands.NAME);
    }

    @Listener
    public void onStarted(StartedEngineEvent<Server> event) {
        Server server = event.engine();
        task = server.scheduler().submit(Task.builder()
                .plugin(container)
                .interval(Ticks.of(1))
                .execute(() -> tick(server))
                .build());
    }

    @Listener
    public void onStopping(StoppingEngineEvent<Server> event) {
        if (task != null) {
            task.cancel();
        }
        hideBar();
        leak.clear();
    }

    private void tick(Server server) {
        LeakStatus status = leak.tick();
        if (!status.leaking()) {
            hideBar();
            return;
        }

        bar.name(Component.text(status.barTitle()));
        bar.color(BossBar.Color.valueOf(status.barColor().name()));
        bar.progress((float) status.progress());
        viewers.removeIf(player -> {
            if (player.isOnline() && player.hasPermission(LeakCommands.PERMISSION)) {
                return false;
            }
            player.hideBossBar(bar);
            return true;
        });
        for (ServerPlayer player : server.onlinePlayers()) {
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
