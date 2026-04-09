package com.hezaerd.memoryleak;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(CommonClass.MOD_ID)
public class MemoryLeak {

    public MemoryLeak(IEventBus eventBus) {
        CommonClass.init();

        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommonClass.registerCommands(event.getDispatcher());
    }
    private void onServerTick(ServerTickEvent.Post event) {
        CommonClass.tick(event.getServer());
    }
}