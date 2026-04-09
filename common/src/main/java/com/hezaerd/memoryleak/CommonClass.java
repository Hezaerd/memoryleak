package com.hezaerd.memoryleak;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommonClass {
	public static final String MOD_ID = "ml";
	public static final String MOD_NAME = "MemoryLeak";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

	public static void init() {
		LOG.info("{} initialized", MOD_NAME);
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(LeakCommand.register());
	}

	public static void tick(MinecraftServer server) {
		LeakManager.INSTANCE.tick(server);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

}