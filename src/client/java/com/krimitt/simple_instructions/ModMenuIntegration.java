package com.krimitt.simple_instructions;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.krimitt.simple_instructions.tutorial.PlaqueEditorScreen;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return PlaqueEditorScreen::new;
	}
}
