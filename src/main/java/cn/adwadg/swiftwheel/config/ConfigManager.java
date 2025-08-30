// file: config/ConfigManager.java
package cn.adwadg.swiftwheel.config;

import cn.adwadg.swiftwheel.SwiftWheel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = SwiftWheel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigManager {
    public static ModConfig.ClientConfig CLIENT;

    public static void initialize() {
        CLIENT = ModConfig.CLIENT;
    }

    public static double getWheelScale() {
        return CLIENT.wheelScale.get();
    }

    public static double getSelectionScale() {
        return CLIENT.selectionScale.get();
    }

    public static List<String> getCategories() {
        return (List<String>) CLIENT.categories.get();
    }

    public static int getMaxCategories() {
        return CLIENT.maxCategories.get();
    }

    public static int getMaxCategoriesPerPage() {
        return CLIENT.maxCategoriesPerPage.get();
    }

    public static boolean areAnimationsEnabled() {
        return CLIENT.enableAnimations.get();
    }

    public static double getPrimaryAnimationDuration() {
        return CLIENT.primaryAnimationDuration.get();
    }

    public static double getSecondaryAnimationDuration() {
        return CLIENT.secondaryAnimationDuration.get();
    }

    public static ModConfig.AnimationType getPrimaryAnimationType() {
        return CLIENT.primaryAnimationType.get();
    }

    public static ModConfig.AnimationType getSecondaryAnimationType() {
        return CLIENT.secondaryAnimationType.get();
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ModConfig.CLIENT_SPEC) {
            initialize();
            SwiftWheel.LOGGER.info("Swift Wheel config loaded");
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ModConfig.CLIENT_SPEC) {
            SwiftWheel.LOGGER.info("Swift Wheel config reloaded");
        }
    }
}
