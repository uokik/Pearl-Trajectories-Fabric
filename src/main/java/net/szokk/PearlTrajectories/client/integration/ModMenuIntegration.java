package net.szokk.PearlTrajectories.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.szokk.PearlTrajectories.client.gui.PearlTrajectoriesConfigScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PearlTrajectoriesConfigScreen::new;
    }
}
