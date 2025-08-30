// file: config/ConfigRegistration.java
package cn.adwadg.swiftwheel.config;

import cn.adwadg.swiftwheel.SwiftWheel;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ConfigRegistration {
    public static void register() {
        try {
            SwiftWheel.LOGGER.info("Registering client config...");

            ModLoadingContext.get().registerConfig(
                    ModConfig.Type.CLIENT,
                    cn.adwadg.swiftwheel.config.ModConfig.CLIENT_SPEC,
                    "swiftwheel-client.toml"
            );

            SwiftWheel.LOGGER.info("Config registered successfully - spec: {}", cn.adwadg.swiftwheel.config.ModConfig.CLIENT_SPEC != null ? "not null" : "null");
        } catch (Exception e) {
            SwiftWheel.LOGGER.error("Failed to register config", e);
            e.printStackTrace();
        }
    }
}
