// file: SwiftWheelClient.java
package cn.adwadg.swiftwheel;

import cn.adwadg.swiftwheel.client.gui.WheelGUI;
import cn.adwadg.swiftwheel.client.input.KeyBindings;
import cn.adwadg.swiftwheel.config.ConfigScreenFactory;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Arrays;
import java.util.List;

public class SwiftWheelClient {
    private static final WheelGUI wheelGUI = new WheelGUI();

    public static void init() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册客户端设置事件
        //modEventBus.addListener(SwiftWheelClient::clientSetup);
        modEventBus.addListener(SwiftWheelClient::registerOverlays);
        modEventBus.addListener(SwiftWheelClient::registerKeybinds);

        MinecraftForge.EVENT_BUS.register(KeyBindings.class);
        MinecraftForge.EVENT_BUS.register(wheelGUI);
    }


    private static void registerOverlays(final RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("swiftwheel", wheelGUI);
    }

    private static void registerKeybinds(final RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_WHEEL_KEY);
    }

    public static WheelGUI getWheelGUI() {
        return wheelGUI;
    }

    public static List<KeyMapping> getKeyMappings() {
        return Arrays.asList(KeyBindings.OPEN_WHEEL_KEY);
    }
}
