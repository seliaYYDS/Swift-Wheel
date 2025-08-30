// file: SwiftWheel.java
package cn.adwadg.swiftwheel;

import cn.adwadg.swiftwheel.config.ConfigRegistration;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(SwiftWheel.MODID)
public class SwiftWheel {
    public final static String MODID = "swiftwheel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SwiftWheel() {
        // 注册配置 - 必须在构造函数中调用
        ConfigRegistration.register();
        SwiftWheel.LOGGER.info("Config registration called in constructor");

        if (FMLEnvironment.dist.isClient()) {
            SwiftWheel.LOGGER.info("Initializing client side");
            SwiftWheelClient.init();
        }

        LOGGER.info("Swift Wheel mod initialized");
    }
}
