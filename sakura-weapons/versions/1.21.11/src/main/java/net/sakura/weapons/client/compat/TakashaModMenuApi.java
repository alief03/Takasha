package net.sakura.weapons.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sakura.weapons.client.gui.TakashaDashboardScreen;

@Environment(EnvType.CLIENT)
public class TakashaModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TakashaDashboardScreen::new;
    }
}
