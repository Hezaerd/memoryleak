package com.hezaerd.memoryleak;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;

public final class LeakCommand {
    private LeakCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("leak")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.literal("start")
                        .then(Commands.argument("megabytes", IntegerArgumentType.integer(1, 10000))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                        .executes(LeakCommand::startLeak))))
                .then(Commands.literal("stop")
                        .executes(LeakCommand::stopLeak))
                .then(Commands.literal("clear")
                        .executes(LeakCommand::clearLeak))
                .then(Commands.literal("status")
                        .executes(LeakCommand::statusLeak));
    }
    private static int startLeak(CommandContext<CommandSourceStack> context) {
        int megabytes = IntegerArgumentType.getInteger(context, "megabytes");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        try {
            MinecraftServer server = context.getSource().getServer();
            LeakManager.INSTANCE.start(megabytes, seconds, server);
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format(
                            "Started leaking %d MB over %d seconds",
                            megabytes,
                            seconds
                    )).withStyle(ChatFormatting.GREEN),
                    true
            );
            return 1;
        } catch (IllegalArgumentException | IllegalStateException e) {
            context.getSource().sendFailure(
                    Component.literal(e.getMessage()).withStyle(ChatFormatting.RED)
            );
            return 0;
        }
    }
    private static int stopLeak(CommandContext<CommandSourceStack> context) {
        LeakManager.INSTANCE.stop();
        context.getSource().sendSuccess(
                () -> Component.literal("Stopped memory leak").withStyle(ChatFormatting.YELLOW),
                true
        );
        return 1;
    }
    private static int clearLeak(CommandContext<CommandSourceStack> context) {
        LeakManager.INSTANCE.clear();
        context.getSource().sendSuccess(
                () -> Component.literal("Cleared all leaked memory").withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }
    private static int statusLeak(CommandContext<CommandSourceStack> context) {
        LeakStatus status = LeakManager.INSTANCE.getStatus();
        if (status.leaking()) {
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format(
                            "Leak in progress: %d/%d MB (%.1f%%) - %.1f seconds remaining",
                            status.allocatedMegabytes(),
                            status.targetMegabytes(),
                            status.progress() * 100.0,
                            status.remainingSeconds()
                    )).withStyle(ChatFormatting.YELLOW),
                    false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal(String.format(
                            "No active leak. Total leaked: %d MB",
                            status.allocatedMegabytes()
                    )).withStyle(ChatFormatting.GRAY),
                    false
            );
        }
        return 1;
    }
}
