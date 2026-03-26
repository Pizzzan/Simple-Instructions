package com.yasha.simple_instructions;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleInstructions implements ModInitializer {
	public static final String MOD_ID = "simple_instructions";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Simple Instructions");
	}
}
