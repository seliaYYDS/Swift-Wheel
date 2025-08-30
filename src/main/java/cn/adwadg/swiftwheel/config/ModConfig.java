// file: config/ModConfig.java
package cn.adwadg.swiftwheel.config;

import cn.adwadg.swiftwheel.SwiftWheel;
import cn.adwadg.swiftwheel.utils.ItemCategorizer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = SwiftWheel.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfig {
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = specPair.getLeft();
        CLIENT_SPEC = specPair.getRight();
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.DoubleValue wheelScale;
        public final ForgeConfigSpec.DoubleValue selectionScale;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> categories;
        public final ForgeConfigSpec.IntValue maxCategories;
        public final ForgeConfigSpec.IntValue maxCategoriesPerPage;
        public final ForgeConfigSpec.BooleanValue enableAnimations;
        public final ForgeConfigSpec.DoubleValue primaryAnimationDuration;
        public final ForgeConfigSpec.DoubleValue secondaryAnimationDuration;
        public final ForgeConfigSpec.EnumValue<AnimationType> primaryAnimationType;
        public final ForgeConfigSpec.EnumValue<AnimationType> secondaryAnimationType;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("wheel");

            wheelScale = builder
                    .comment("Wheel scale (0.5 - 2.0)")
                    .translation("swiftwheel.config.wheelScale")
                    .defineInRange("wheelScale", 1.0, 0.5, 2.0);

            selectionScale = builder
                    .comment("Selection scale (1.0 - 1.5)")
                    .translation("swiftwheel.config.selectionScale")
                    .defineInRange("selectionScale", 1.15, 1.0, 1.5);

            builder.pop();
            builder.push("categories");

            // 获取所有可用的分类名称
            List<String> allCategories = ItemCategorizer.Category.getAllCategoryNames();
            String availableCategories = String.join(", ", allCategories);

            categories = builder
                    .comment("Category order. Available categories: " + availableCategories)
                    .translation("swiftwheel.config.categories")
                    .defineList("categories",
                            Arrays.asList("weapons", "pickaxes", "axes", "shovels",
                                    "blocks", "food","ranged_weapons", "tools", "potions","functional_blocks", "armor","decorative_blocks","natural_blocks","materials","redstone","transportation","misc", "inventory"),
                            obj -> obj instanceof String && allCategories.contains(obj));

            maxCategories = builder
                    .comment("Max total categories (1-64)")
                    .translation("swiftwheel.config.maxCategories")
                    .defineInRange("maxCategories", 24, 1, 64);

            maxCategoriesPerPage = builder
                    .comment("Max categories per page (4-12)")
                    .translation("swiftwheel.config.maxCategoriesPerPage")
                    .defineInRange("maxCategoriesPerPage", 6, 4, 12);

            builder.pop();
            builder.push("animations");

            enableAnimations = builder
                    .comment("Enable animations")
                    .translation("swiftwheel.config.enableAnimations")
                    .define("enableAnimations", true);

            primaryAnimationDuration = builder
                    .comment("Primary animation duration (ms)")
                    .translation("swiftwheel.config.primaryAnimationDuration")
                    .defineInRange("primaryAnimationDuration", 200.0, 50.0, 1000.0);

            secondaryAnimationDuration = builder
                    .comment("Secondary animation duration (ms)")
                    .translation("swiftwheel.config.secondaryAnimationDuration")
                    .defineInRange("secondaryAnimationDuration", 150.0, 50.0, 1000.0);

            primaryAnimationType = builder
                    .comment("Primary animation type")
                    .translation("swiftwheel.config.primaryAnimationType")
                    .defineEnum("primaryAnimationType", AnimationType.EASE_OUT_BACK);

            secondaryAnimationType = builder
                    .comment("Secondary animation type")
                    .translation("swiftwheel.config.secondaryAnimationType")
                    .defineEnum("secondaryAnimationType", AnimationType.EASE_OUT_BACK);

            builder.pop();
        }
    }

    public enum AnimationType {
        LINEAR,
        EASE_IN_OUT,
        EASE_OUT_BACK,
        EASE_IN_BACK,
        EASE_OUT_CUBIC,
        EASE_IN_CUBIC,
        BOUNCE
    }
}
