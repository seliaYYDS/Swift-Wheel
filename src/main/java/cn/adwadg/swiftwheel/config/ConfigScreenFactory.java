// file: config/ConfigScreenFactory.java
package cn.adwadg.swiftwheel.config;

import cn.adwadg.swiftwheel.SwiftWheel;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ConfigScreenFactory {
    public static void registerConfigScreen(final FMLClientSetupEvent event) {
        try {
            SwiftWheel.LOGGER.info("Registering config screen extension point...");

            // 使用正确的方法注册配置屏幕
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parentScreen) -> {
                        SwiftWheel.LOGGER.info("Config screen requested by Forge");
                        // 返回 null 让 Forge 自动生成配置界面
                        return null;
                    })
            );

            SwiftWheel.LOGGER.info("Config screen factory registered successfully");
        } catch (Exception e) {
            SwiftWheel.LOGGER.error("Failed to register config screen factory", e);
        }
    }
}
