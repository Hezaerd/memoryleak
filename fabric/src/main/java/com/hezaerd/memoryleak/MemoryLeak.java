package com.hezaerd.memoryleak;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class MemoryLeak implements ModInitializer {
    
    @Override
    public void onInitialize() {
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register(((dispatcher, buildContext, selection) -> {
            CommonClass.registerCommands(dispatcher);
        }));

        ServerTickEvents.END_SERVER_TICK.register(CommonClass::tick);
    }
}
