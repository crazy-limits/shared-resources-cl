//? if fabric {
package com.crazylimits.shared_resources.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.crazylimits.shared_resources.config.SharedResourcesConfig;
import com.crazylimits.shared_resources.config.SharedResourcesConfigScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> SharedResourcesConfigScreen.create(SharedResourcesConfig.CONFIG, parent);
    }
}
//?}
