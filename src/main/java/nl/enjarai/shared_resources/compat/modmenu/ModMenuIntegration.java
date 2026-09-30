//? if fabric {
package nl.enjarai.shared_resources.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import nl.enjarai.shared_resources.config.SharedResourcesConfig;
import nl.enjarai.shared_resources.config.SharedResourcesConfigScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> SharedResourcesConfigScreen.create(SharedResourcesConfig.CONFIG, parent);
    }
}
//?}
