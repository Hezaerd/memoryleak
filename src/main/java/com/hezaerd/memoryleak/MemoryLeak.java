package com.hezaerd.memoryleak;

import com.hezaerd.memoryleak.core.LeakCommands;
import com.hezaerd.memoryleak.core.LeakManager;
import com.hezaerd.memoryleak.core.LeakStatus;
import com.hezaerd.memoryleak.core.Reply;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.world.BossEvent;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

//? if >=26.1 {
import net.minecraft.util.RandomSource;
//?}

//? if fabric {
/*import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
*///?} else if neoforge {
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
//?} else {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
*///?}

//? if fabric {
/*public final class MemoryLeak implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(MemoryLeak::tick);
    }
*///?} else if neoforge {
@Mod(MemoryLeak.MOD_ID)
public final class MemoryLeak {
    public MemoryLeak() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tick(event.getServer()));
    }
//?} else {
/*@Mod(MemoryLeak.MOD_ID)
public final class MemoryLeak {
    public MemoryLeak() {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                tick(event.getServer());
            }
        });
    }
*///?}

    static final String MOD_ID = "ml";
    private static final int BOSS_BAR_SCALE = 1000;
    private static final LeakManager LEAK = LeakManager.INSTANCE;

    //? if >=1.21.11 {
    private static final Identifier BOSS_BAR = Identifier.fromNamespaceAndPath(MOD_ID, "memory_leak");
    //?} else if >=1.21 {
    /*private static final ResourceLocation BOSS_BAR = ResourceLocation.fromNamespaceAndPath(MOD_ID, "memory_leak");
    *///?} else {
    /*private static final ResourceLocation BOSS_BAR = new ResourceLocation(MOD_ID, "memory_leak");
    *///?}

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(LeakCommands.NAME)
                .requires(MemoryLeak::isAdmin)
                .then(Commands.literal("start")
                        .then(Commands.argument("megabytes", IntegerArgumentType.integer(1, LeakManager.MAX_MEGABYTES))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, LeakManager.MAX_SECONDS))
                                        .executes(context -> respond(context.getSource(), LeakCommands.start(LEAK,
                                                IntegerArgumentType.getInteger(context, "megabytes"),
                                                IntegerArgumentType.getInteger(context, "seconds")))))))
                .then(Commands.literal("stop")
                        .executes(context -> respond(context.getSource(), LeakCommands.stop(LEAK))))
                .then(Commands.literal("clear")
                        .executes(context -> respond(context.getSource(), LeakCommands.clear(LEAK))))
                .then(Commands.literal("status")
                        .executes(context -> respond(context.getSource(), LeakCommands.status(LEAK)))));
    }

    private static boolean isAdmin(CommandSourceStack source) {
        //? if >=1.21.11 {
        return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
        //?} else {
        /*return source.hasPermission(4);
        *///?}
    }

    private static int respond(CommandSourceStack source, Reply reply) {
        ChatFormatting color = switch (reply.tone()) {
            case SUCCESS -> ChatFormatting.GREEN;
            case WARNING -> ChatFormatting.YELLOW;
            case INFO -> ChatFormatting.GRAY;
            case ERROR -> ChatFormatting.RED;
        };
        Component message = Component.literal(reply.text()).withStyle(color);
        if (reply.succeeded()) {
            source.sendSuccess(() -> message, reply.notifyAdmins());
            return 1;
        }
        source.sendFailure(message);
        return 0;
    }

    private static void tick(MinecraftServer server) {
        LeakStatus status = LEAK.tick();
        CustomBossEvents events = server.getCustomBossEvents();
        CustomBossEvent bar = events.get(BOSS_BAR);
        if (!status.leaking()) {
            if (bar != null) {
                bar.removeAllPlayers();
                events.remove(bar);
            }
            return;
        }
        Component title = Component.literal(status.barTitle());
        if (bar == null) {
            //? if >=26.1 {
            bar = events.create(RandomSource.create(), BOSS_BAR, title);
            //?} else {
            /*bar = events.create(BOSS_BAR, title);
            *///?}
            bar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
        }
        bar.setName(title);
        bar.setMax(BOSS_BAR_SCALE);
        bar.setValue((int) Math.round(status.progress() * BOSS_BAR_SCALE));
        bar.setColor(switch (status.barColor()) {
            case GREEN -> BossEvent.BossBarColor.GREEN;
            case YELLOW -> BossEvent.BossBarColor.YELLOW;
            case RED -> BossEvent.BossBarColor.RED;
        });
        bar.setPlayers(server.getPlayerList().getPlayers().stream()
                //? if >=1.21.11 {
                .filter(player -> player.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                //?} else {
                /*.filter(player -> isAdmin(player.createCommandSourceStack()))
                *///?}
                .toList());
    }
}
