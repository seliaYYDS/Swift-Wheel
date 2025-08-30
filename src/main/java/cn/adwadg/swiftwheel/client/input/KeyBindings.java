// file: client/input/KeyBindings.java
package cn.adwadg.swiftwheel.client.input;

import cn.adwadg.swiftwheel.SwiftWheel;
import cn.adwadg.swiftwheel.SwiftWheelClient;
import cn.adwadg.swiftwheel.client.gui.WheelGUI;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class KeyBindings {
    public static final KeyMapping OPEN_WHEEL_KEY = new KeyMapping(
            "key.swiftwheel.open_wheel",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "category.swiftwheel.general"
    );

    private static boolean sensitivityReduced = false;
    private static double originalSensitivity = 0.5;
    private static final double SLOW_DOWN_FACTOR = 0.3;
    private static boolean wasKeyPressedLastTick = false;
    // 中键状态跟踪
    private static boolean wasMiddleButtonPressed = false;
    private static long middleButtonPressTime = 0;
    private static final long MIDDLE_BUTTON_HOLD_THRESHOLD = 200; // 200ms 按住时间
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            handleWheelState();
        }
    }

    private static void handleWheelState() {
        Minecraft mc = Minecraft.getInstance();
        WheelGUI wheelGUI = SwiftWheelClient.getWheelGUI();
        // 直接检查按键状态，而不是依赖事件
        boolean isKeyPressed = OPEN_WHEEL_KEY.isDown();
        if (isKeyPressed && !wasKeyPressedLastTick) {
            // 按键刚刚被按下
            if (!wheelGUI.isWheelOpen()) {
                wheelGUI.openWheel();
                originalSensitivity = mc.options.sensitivity().get();
                setMouseSensitivityReduced(true);
            }
        } else if (!isKeyPressed && wasKeyPressedLastTick) {
            // 按键刚刚被释放
            if (wheelGUI.isWheelOpen()) {
                if (wheelGUI.isItemListMode()) {
                    selectSpecificItem();
                } else {
                    selectBestItem();
                }
                wheelGUI.closeWheel();
                setMouseSensitivityReduced(false);
            }
        }
        wasKeyPressedLastTick = isKeyPressed;
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        WheelGUI wheelGUI = SwiftWheelClient.getWheelGUI();
        if (wheelGUI.isWheelOpen()) {
            // 使用正确的取消方法
            if (event.getAction() == GLFW.GLFW_PRESS) {
                // 取消所有鼠标按键的默认行为
                if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT ||
                        event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT ||
                        event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    event.setCanceled(true);
                }

                // 处理中键按下
                if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    wasMiddleButtonPressed = true;
                    middleButtonPressTime = System.currentTimeMillis();
                }
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                // 处理中键释放
                if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                    long pressDuration = System.currentTimeMillis() - middleButtonPressTime;

                    if (pressDuration < MIDDLE_BUTTON_HOLD_THRESHOLD) {
                        // 短按中键 - 显示物品特写
                        if (wheelGUI.isItemListMode()) {
                            wheelGUI.toggleItemPreview();
                        }
                    }
                    wasMiddleButtonPressed = false;
                    event.setCanceled(true);
                }
                // 只在释放时处理选择
                else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT ||
                        event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    handleWheelSelection(event.getButton());
                    event.setCanceled(true); // 阻止释放事件传播
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        WheelGUI wheelGUI = SwiftWheelClient.getWheelGUI();
        if (wheelGUI.isWheelOpen()) {
            double scrollDelta = event.getScrollDelta();

            if (wheelGUI.isItemListMode()) {
                // 物品列表模式：滚轮切换物品页面
                if (scrollDelta > 0) {
                    wheelGUI.previousPage();
                } else if (scrollDelta < 0) {
                    wheelGUI.nextPage();
                }
            } else {
                // 分类模式：滚轮切换分类页面
                if (scrollDelta > 0) {
                    wheelGUI.previousCategoryPage();
                } else if (scrollDelta < 0) {
                    wheelGUI.nextCategoryPage();
                }
            }
            event.setCanceled(true);
        }
    }

    private static void setMouseSensitivityReduced(boolean reduced) {
        Minecraft mc = Minecraft.getInstance();
        if (reduced && !sensitivityReduced) {
            // 修改鼠标灵敏度
            double reducedSensitivity = originalSensitivity * SLOW_DOWN_FACTOR;
            mc.options.sensitivity().set(reducedSensitivity);
            sensitivityReduced = true;
        } else if (!reduced && sensitivityReduced) {
            // 恢复原始灵敏度
            mc.options.sensitivity().set(originalSensitivity);
            sensitivityReduced = false;
        }
    }

    private static void handleWheelSelection(int mouseButton) {
        WheelGUI wheelGUI = SwiftWheelClient.getWheelGUI();
        Minecraft mc = Minecraft.getInstance();
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (wheelGUI.isItemListMode()) {
                selectSpecificItem();
            } else {
                selectBestItem();
            }
            wheelGUI.closeWheel();
            setMouseSensitivityReduced(false);
        } else if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (wheelGUI.isItemListMode()) {
                wheelGUI.exitItemListMode();
            } else {
                wheelGUI.enterItemListMode();
            }
        }
    }

    private static void selectBestItem() {
        WheelGUI wheelGUI = SwiftWheelClient.getWheelGUI();
        Minecraft mc = Minecraft.getInstance();
        String category = wheelGUI.getSelectedCategory();

        if (!category.isEmpty() && mc.player != null) {
            ItemStack bestItem = wheelGUI.getBestItemForCategory(category);
            if (!bestItem.isEmpty()) {
                switchToItem(mc.player, bestItem);
            }
        }
    }

    private static void selectSpecificItem() {
        WheelGUI wheelGUI = SwiftWheelClient.getWheelGUI();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null) {
            ItemStack selectedItem = wheelGUI.getSelectedItem();
            if (!selectedItem.isEmpty()) {
                switchToItem(mc.player, selectedItem);
            }
        }
    }
    private static void switchToItem(Player player, ItemStack targetItem) {
        Inventory inventory = player.getInventory();

        // 1. 首先查找目标物品在背包中的位置
        int targetSlot = -1;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(stack, targetItem) && !stack.isEmpty()) {
                targetSlot = i;
                break;
            }
        }

        if (targetSlot == -1) {
            // 物品不在背包中（理论上不应该发生）
            return;
        }

        // 2. 检查目标物品是否已经在快捷栏
        if (targetSlot < 9) {
            // 已经在快捷栏，直接选择
            inventory.selected = targetSlot;
            if (player instanceof LocalPlayer) {
                ((LocalPlayer) player).connection.send(new ServerboundSetCarriedItemPacket(targetSlot));
            }
            return;
        }

        // 3. 目标物品在背包中，需要移动到快捷栏
        int currentSelected = inventory.selected;

        ItemStack currentItem = inventory.getItem(currentSelected);
        SwiftWheel.LOGGER.debug("current:{},{}",currentSelected,currentItem);

        int emptySlot = findEmptyHotbarSlot(inventory);
        if (emptySlot != -1) {
            // 有空位，移动到空位
            SwiftWheel.LOGGER.debug("nemp,empslot:{}",emptySlot);
            moveItemToSlot(player, targetSlot, emptySlot);
            inventory.selected = emptySlot;
            SwiftWheel.LOGGER.debug("nemp,after:{}",inventory.selected);
        } else {
            // 没有空位，交换当前物品和目标物品
            SwiftWheel.LOGGER.debug("nemphb,current:{}",currentSelected);
            swapItems(player, targetSlot, currentSelected);
            inventory.selected = currentSelected;
        }
    }
    private static int findEmptyHotbarSlot(Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
    private static void moveItemToSlot(Player player, int sourceSlot, int destSlot) {
        if (player instanceof LocalPlayer) {
            LocalPlayer localPlayer = (LocalPlayer) player;

            // 首先确保目标槽位为空
            if (player.getInventory().getItem(destSlot).isEmpty()) {
                // 使用快速移动（模拟Shift+点击）
                localPlayer.connection.send(new ServerboundContainerClickPacket(
                        player.containerMenu.containerId,
                        player.containerMenu.getStateId(),
                        sourceSlot,
                        0,  // 按钮
                        ClickType.QUICK_MOVE,  // Shift点击
                        ItemStack.EMPTY,
                        new Int2ObjectArrayMap<>()
                ));
            }
        }
    }
    private static void swapItems(Player player, int slot1, int slot2) {
        if (player instanceof LocalPlayer) {
            LocalPlayer localPlayer = (LocalPlayer) player;
            SwiftWheel.LOGGER.debug("swap,{}->{}",slot1,slot2);
            // 交换两个槽位的物品
            localPlayer.connection.send(new ServerboundPickItemPacket(slot1));
            localPlayer.connection.send(new ServerboundPickItemPacket(slot2));
        }
    }
}
