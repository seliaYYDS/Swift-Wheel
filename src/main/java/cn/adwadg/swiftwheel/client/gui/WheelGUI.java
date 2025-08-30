package cn.adwadg.swiftwheel.client.gui;

import cn.adwadg.swiftwheel.config.ConfigManager;
import cn.adwadg.swiftwheel.config.ModConfig;
import cn.adwadg.swiftwheel.utils.ItemCategorizer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Matrix4f;

import java.util.*;

public class WheelGUI implements IGuiOverlay {
    private static final Minecraft mc = Minecraft.getInstance();
    private boolean isWheelOpen = false;
    private long openTime = 0;
    private Vec2 selectionDirection = Vec2.ZERO;
    private String selectedCategory = "";
    private String previousSelectedCategory = "";
    private int currentPage = 0;
    private boolean isItemListMode = false;
    int centerX = 0;
    int centerY = 0;

    // 动画状态
    private float animationProgress = 0f;
    private float scale = 0f;
    private float opacity = 0f;

    // 本地常量
    private static final int WHEEL_RADIUS = 100;
    private static final int MAX_ITEMS_PER_PAGE = 8;
    private static final float GAP_ANGLE = (float) Math.toRadians(5);

    private static final float SELECTION_SCALE_BOOST = 0.3f; // 统一增加30%的缩放
    private static final float SELECTION_ELEVATION = 8f; // 统一抬升高度

    private boolean isItemPreviewVisible = false;
    private ItemStack previewItem = ItemStack.EMPTY;
    private long previewShowTime = 0;
    private static final long PREVIEW_DURATION = 3000; // 3秒显示时间

    private int categoryCurrentPage = 0;
    private int categoryTotalPages = 1;
    private int getMaxCategoriesPerPage() {
        return ConfigManager.getMaxCategoriesPerPage();
    }

    private Map<ItemCategorizer.Category, List<ItemStack>> categorizedItems = new EnumMap<>(ItemCategorizer.Category.class);
    private Map<String, Integer> selectedItemIndices = new HashMap<>();

    private int getWheelRadius() {
        return (int) (100 * ConfigManager.getWheelScale());
    }

    private float getSelectionScale() {
        return (float) ConfigManager.getSelectionScale();
    }

    private float getPrimaryAnimationDuration() {
        return (float) ConfigManager.getPrimaryAnimationDuration();
    }

    private float getSecondaryAnimationDuration() {
        return (float) ConfigManager.getSecondaryAnimationDuration();
    }



    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!isWheelOpen && animationProgress <= 0) return;

        // 更新动画
        updateAnimation(partialTick);
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        float animatedRadius = WHEEL_RADIUS * scale;
        float animatedOpacity = opacity;

        // 只在轮盘完全打开时更新选择
        if (isWheelOpen && animationProgress >= 0.5f) {
            updateSelection(screenWidth, screenHeight);
        }

        // 先渲染物品特写（在最上层）
        if (isItemPreviewVisible) {
            renderItemPreview(guiGraphics, screenWidth, screenHeight, partialTick);
        }

        // 然后渲染轮盘背景
        renderWheelBackground(guiGraphics, centerX, centerY, animatedRadius, animatedOpacity);
        if (isItemListMode) {
            // 检查是否有物品，如果没有则显示提示
            List<ItemStack> items = getItemsForCategory(selectedCategory);
            if (items.isEmpty()) {
                renderEmptyItemListHint(guiGraphics, centerX, centerY, animatedOpacity);
            } else {
                renderItemList(guiGraphics, centerX, centerY, animatedRadius, animatedOpacity);
            }
        } else {
            // 检查是否有分类，如果没有则显示提示
            List<String> categories = getCategories();
            if (categories.isEmpty()) {
                renderEmptyCategoryHint(guiGraphics, centerX, centerY, animatedOpacity);
            } else {
                renderCategories(guiGraphics, centerX, centerY, animatedRadius, animatedOpacity);
            }
        }
        renderSelectionIndicator(guiGraphics, centerX, centerY, animatedRadius, animatedOpacity);
        renderPageInfo(guiGraphics, screenWidth, screenHeight);
        renderCategoryPageInfo(guiGraphics, screenWidth, screenHeight);
    }

    private float itemListAnimationProgress = 0f;
    private float itemListScale = 0f;
    private float itemListOpacity = 0f;

    private void updateAnimation(float partialTick) {
        if (!ConfigManager.areAnimationsEnabled()) {
            // 禁用动画时直接设置最终状态
            if (isWheelOpen) {
                scale = 1.0f;
                opacity = 1.0f;
                if (isItemListMode) {
                    itemListScale = 1.0f;
                    itemListOpacity = 1.0f;
                }
            } else {
                scale = 0f;
                opacity = 0f;
                itemListScale = 0f;
                itemListOpacity = 0f;
            }
            return;
        }

        long currentTime = System.currentTimeMillis();
        float elapsed = currentTime - openTime;

        if (isWheelOpen) {
            float primaryDuration = getPrimaryAnimationDuration();
            animationProgress = Math.min(1.0f, elapsed / primaryDuration);
            scale = calculateAnimation(animationProgress, ConfigManager.getPrimaryAnimationType());
            opacity = easeOutCubic(animationProgress);
            // 二级轮盘动画
            if (isItemListMode) {
                float secondaryDuration = getSecondaryAnimationDuration();
                itemListAnimationProgress = Math.min(1.0f, elapsed / secondaryDuration);
                itemListScale = calculateAnimation(itemListAnimationProgress, ConfigManager.getSecondaryAnimationType());
                itemListOpacity = easeOutCubic(itemListAnimationProgress);
            } else {
                itemListAnimationProgress = 0f;
                itemListScale = 0f;
                itemListOpacity = 0f;
            }
        } else {
            float primaryDuration = getPrimaryAnimationDuration();
            animationProgress = Math.max(0.0f, 1.0f - (elapsed / primaryDuration));
            scale = calculateAnimation(animationProgress, ConfigManager.getPrimaryAnimationType());
            opacity = easeInCubic(animationProgress);
            // 二级轮盘关闭动画
            if (isItemListMode) {
                float secondaryDuration = getSecondaryAnimationDuration();
                itemListAnimationProgress = Math.max(0.0f, 1.0f - (elapsed / secondaryDuration));
                itemListScale = calculateAnimation(itemListAnimationProgress, ConfigManager.getSecondaryAnimationType());
                itemListOpacity = easeInCubic(itemListAnimationProgress);
            }

            if (animationProgress <= 0) {
                resetSelectionState();
            }
        }
    }

    private void renderEmptyItemListHint(GuiGraphics guiGraphics, int centerX, int centerY, float opacity) {
        Component hintText = Component.translatable("swiftwheel.hint.empty_items");
        int textWidth = mc.font.width(hintText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - mc.font.lineHeight / 2;

        int color = 0xAAAAAA | ((int)(opacity * 255) << 24);

        // 添加背景以提高可读性
        int padding = 6;
        int bgColor = 0x000000 | ((int)(opacity * 100) << 24);
        guiGraphics.fill(textX - padding, textY - padding,
                textX + textWidth + padding, textY + mc.font.lineHeight + padding,
                bgColor);

        guiGraphics.drawString(mc.font, hintText, textX, textY, color, false);

        // 添加返回提示
        Component returnHint = Component.translatable("swiftwheel.hint.return_to_categories");
        int returnWidth = mc.font.width(returnHint);
        int returnX = centerX - returnWidth / 2;
        int returnY = centerY + 20;
        int returnColor = 0x888888 | ((int)(opacity * 200) << 24);

        guiGraphics.drawString(mc.font, returnHint, returnX, returnY, returnColor, true);
    }

    private void renderEmptyCategoryHint(GuiGraphics guiGraphics, int centerX, int centerY, float opacity) {
        Component hintText = Component.translatable("swiftwheel.hint.empty_categories");
        int textWidth = mc.font.width(hintText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - mc.font.lineHeight / 2;

        int color = 0xAAAAAA | ((int)(opacity * 255) << 24);

        // 添加背景以提高可读性
        int padding = 6;
        int bgColor = 0x000000 | ((int)(opacity * 100) << 24);
        guiGraphics.fill(textX - padding, textY - padding,
                textX + textWidth + padding, textY + mc.font.lineHeight + padding,
                bgColor);

        guiGraphics.drawString(mc.font, hintText, textX, textY, color, false);

        // 添加配置提示
        Component configHint = Component.translatable("swiftwheel.hint.configure_categories");
        int configWidth = mc.font.width(configHint);
        int configX = centerX - configWidth / 2;
        int configY = centerY + 20;
        int configColor = 0x888888 | ((int)(opacity * 200) << 24);

        guiGraphics.drawString(mc.font, configHint, configX, configY, configColor, true);
    }

    private void renderCategoryPageInfo(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!isItemListMode) {
            // 渲染分类页码
            if (categoryTotalPages > 1) {
                String pageText = (categoryCurrentPage + 1) + "/" + categoryTotalPages;
                int color = (0xFFFFFF | ((int)(opacity * 255) << 24));
                // 计算中心位置
                int centerX = screenWidth / 2;
                int centerY = screenHeight / 2;
                int textWidth = mc.font.width(pageText);

                // 在轮盘下方显示页码
                int textX = centerX - textWidth / 2;
                int textY = centerY + WHEEL_RADIUS + 20; // 轮盘半径 + 偏移量

                // 添加背景以提高可读性
                int padding = 2;
                int bgColor = (0x000000 | ((int)(opacity * 128) << 24));
                guiGraphics.fill(textX - padding, textY - padding,
                        textX + textWidth + padding, textY + mc.font.lineHeight + padding,
                        bgColor);
                guiGraphics.drawString(mc.font, pageText, textX, textY, color, false);
            }
            // 渲染分类页面切换提示
            if (categoryTotalPages > 1) {
                Component hint = Component.translatable("swiftwheel.tooltip.category_page");
                int hintWidth = mc.font.width(hint);
                int hintX = screenWidth / 2 - hintWidth / 2;
                int hintY = screenHeight - 40;

                int hintColor = 0xAAAAAA | ((int)(opacity * 255) << 24);
                guiGraphics.drawString(mc.font, hint, hintX, hintY, hintColor, true);
            }
        } else {
            // 渲染物品页面切换提示
            int totalPages = (int) Math.ceil((double) getItemsForCategory(selectedCategory).size() / MAX_ITEMS_PER_PAGE);
            if (totalPages > 1) {
                Component hint = Component.translatable("swiftwheel.tooltip.item_page");
                int hintWidth = mc.font.width(hint);
                int hintX = screenWidth / 2 - hintWidth / 2;
                int hintY = screenHeight - 40;

                int hintColor = 0xAAAAAA | ((int)(opacity * 255) << 24);
                guiGraphics.drawString(mc.font, hint, hintX, hintY, hintColor, true);
            }
        }
    }

    private void renderItemPreview(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
        if (previewItem.isEmpty()) return;
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - previewShowTime;

        // 检查是否应该关闭预览
        if (elapsed > PREVIEW_DURATION) {
            isItemPreviewVisible = false;
            return;
        }
        // 计算淡入淡出效果
        float fadeProgress = Math.min(1.0f, elapsed / 500f); // 500ms 淡入
        float fadeOutProgress = Math.max(0.0f, 1.0f - (elapsed - (PREVIEW_DURATION - 500f)) / 500f); // 最后500ms淡出
        float alpha = Math.min(fadeProgress, fadeOutProgress);
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        // 渲染半透明背景 - 使用更高的Z层
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 300); // 提高Z层确保在最前面
        guiGraphics.fill(0, 0, screenWidth, screenHeight, (int)(0x90 * alpha) << 24);
        poseStack.popPose();
        // 渲染放大的物品图标 - 使用更高的Z层
        poseStack.pushPose();
        poseStack.translate(centerX, centerY - 30, 400); // 更高的Z层
        poseStack.scale(3.0f, 3.0f, 1.0f);
        poseStack.translate(-8, -8, 0);
        guiGraphics.renderItem(previewItem, 0, 0);
        guiGraphics.renderItemDecorations(mc.font, previewItem, 0, 0);
        poseStack.popPose();
        // 渲染物品名称 - 使用更高的Z层
        Component name = previewItem.getHoverName();
        int nameWidth = mc.font.width(name);
        int nameX = centerX - nameWidth / 2;
        int nameY = centerY + 40;

        // 名称背景
        poseStack.pushPose();
        poseStack.translate(0, 0, 400);
        guiGraphics.fill(nameX - 5, nameY - 2, nameX + nameWidth + 5, nameY + mc.font.lineHeight + 2,
                (int)(0xB0 * alpha) << 24);

        // 名称文本
        int nameColor = 0xFFFFFF | ((int)(alpha * 255) << 24);
        guiGraphics.drawString(mc.font, name, nameX, nameY, nameColor, true);
        poseStack.popPose();
        // 渲染物品信息 - 使用更高的Z层
        renderItemTooltip(guiGraphics, centerX, centerY + 60, alpha);
        // 添加按键提示
        Component hint = Component.translatable("swiftwheel.preview.close");
        int hintWidth = mc.font.width(hint);
        int hintX = centerX - hintWidth / 2;
        int hintY = screenHeight - 30;

        poseStack.pushPose();
        poseStack.translate(0, 0, 400);
        guiGraphics.drawString(mc.font, hint, hintX, hintY, 0xAAAAAA | ((int)(alpha * 255) << 24), true);
        poseStack.popPose();
    }
    private void renderItemTooltip(GuiGraphics guiGraphics, int x, int y, float alpha) {
        List<Component> tooltipLines = previewItem.getTooltipLines(
                mc.player, TooltipFlag.Default.NORMAL
        );
        if (tooltipLines.size() > 1) {
            // 移除第一行（名称），因为我们已经单独显示了
            tooltipLines = tooltipLines.subList(1, Math.min(tooltipLines.size(), 6)); // 限制显示行数
            int maxWidth = 0;
            for (Component line : tooltipLines) {
                maxWidth = Math.max(maxWidth, mc.font.width(line));
            }
            int padding = 4;
            int bgX = x - maxWidth / 2 - padding;
            int bgY = y - padding;
            int bgWidth = maxWidth + padding * 2;
            int bgHeight = tooltipLines.size() * mc.font.lineHeight + padding * 2;
            // 使用更高的Z层渲染背景
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(0, 0, 400);

            // 背景
            guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, (int)(0xB0 * alpha) << 24);
            // 文本
            for (int i = 0; i < tooltipLines.size(); i++) {
                Component line = tooltipLines.get(i);
                int lineX = x - mc.font.width(line) / 2;
                int lineY = y + i * mc.font.lineHeight;
                int lineColor = 0xFFFFFF | ((int)(alpha * 255) << 24);
                guiGraphics.drawString(mc.font, line, lineX, lineY, lineColor, false);
            }

            poseStack.popPose();
        }
    }

    public void toggleItemPreview() {
        if (isItemListMode && !selectedCategory.isEmpty()) {
            ItemStack selectedItem = getSelectedItem();
            if (!selectedItem.isEmpty()) {
                isItemPreviewVisible = !isItemPreviewVisible;
                previewItem = selectedItem.copy();
                previewShowTime = System.currentTimeMillis();

                // 播放音效
                if (isItemPreviewVisible) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(
                            SoundEvents.ITEM_FRAME_ADD_ITEM, 1.0f
                    ));
                } else {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(
                            SoundEvents.ITEM_FRAME_REMOVE_ITEM, 1.0f
                    ));
                }
            }
        }
    }

    public void closeItemPreview() {
        isItemPreviewVisible = false;
    }

    private float calculateAnimation(float progress, ModConfig.AnimationType type) {
        switch (type) {
            case LINEAR:
                return progress;
            case EASE_IN_OUT:
                return easeInOutCubic(progress);
            case EASE_OUT_BACK:
                return easeOutBack(progress);
            case EASE_IN_BACK:
                return easeInBack(progress);
            case EASE_OUT_CUBIC:
                return easeOutCubic(progress);
            case EASE_IN_CUBIC:
                return easeInCubic(progress);
            case BOUNCE:
                return bounce(progress);
            default:
                return easeOutBack(progress);
        }
    }

    private void resetSelectionState() {
        selectionDirection = Vec2.ZERO;
        selectedCategory = "";
        selectedItemIndex = -1;
        previousSelectedItemIndex = -1;
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return (float) (1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2));
    }

    private float easeInBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return (float) (c3 * x * x * x - c1 * x * x);
    }

    private float easeOutCubic(float x) {
        return (float) (1 - Math.pow(1 - x, 3));
    }

    private float easeInCubic(float x) {
        return x * x * x;
    }

    private float easeInOutCubic(float x) {
        return x < 0.5 ? 4 * x * x * x : (float) (1 - Math.pow(-2 * x + 2, 3) / 2);
    }

    private float bounce(float x) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2 / d1) {
            return n1 * (x -= 1.5f / d1) * x + 0.75f;
        } else if (x < 2.5 / d1) {
            return n1 * (x -= 2.25f / d1) * x + 0.9375f;
        } else {
            return n1 * (x -= 2.625f / d1) * x + 0.984375f;
        }
    }

    private void updateSelection(int screenWidth, int screenHeight) {
        float mouseX = (float) mc.mouseHandler.xpos();
        float mouseY = (float) mc.mouseHandler.ypos();

        float dirX = mouseX - centerX;
        float dirY = mouseY - centerY;

        // 只在鼠标移动时更新选择方向
        if (dirX != 0 || dirY != 0) {
            selectionDirection = new Vec2(dirX, dirY);
        }

        if (isItemListMode) {
            calculateSelectedItem(centerX, centerY);
        } else {
            String newCategory = calculateSelectedCategory(centerX, centerY);
            if (!newCategory.equals(selectedCategory)) {
                previousSelectedCategory = selectedCategory;
                selectedCategory = newCategory;
                if (!selectedCategory.isEmpty()) {
                    playSelectionSound();
                }
            }
        }
    }

    private String calculateSelectedCategory(int centerX, int centerY) {
        List<String> categories = getCategories();
        if (categories.isEmpty() || selectionDirection.equals(Vec2.ZERO)) return "";

        double distance = Math.sqrt(selectionDirection.x * selectionDirection.x + selectionDirection.y * selectionDirection.y);
        if (distance < 20) return "";

        double angle = Math.atan2(selectionDirection.y, selectionDirection.x);
        if (angle < 0) angle += 2 * Math.PI;

        int categoryCount = categories.size();
        float angleStep = (float) (2 * Math.PI / categoryCount);

        for (int i = 0; i < categoryCount; i++) {
            float startAngle = i * angleStep;
            float endAngle = startAngle + angleStep;

            if (angle >= startAngle && angle <= endAngle) {
                return categories.get(i);
            }
        }

        return "";
    }

    private int previousSelectedItemIndex = -1;
    private int selectedItemIndex = -1;
    private void calculateSelectedItem(int centerX, int centerY) {
        List<ItemStack> items = getItemsForCategory(selectedCategory);
        if (items.isEmpty() || selectionDirection.equals(Vec2.ZERO)) {
            selectedItemIndex = -1;
            return;
        }

        double distance = Math.sqrt(selectionDirection.x * selectionDirection.x + selectionDirection.y * selectionDirection.y);
        if (distance < 20) {
            selectedItemIndex = -1;
            return;
        }

        double angle = Math.atan2(selectionDirection.y, selectionDirection.x);
        if (angle < 0) angle += 2 * Math.PI;

        int itemsPerPage = MAX_ITEMS_PER_PAGE;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        float angleStep = (float) (2 * Math.PI / itemsPerPage);

        for (int i = startIndex; i < endIndex; i++) {
            float startAngle = (i - startIndex) * angleStep;
            float endAngle = startAngle + angleStep;

            if (angle >= startAngle && angle <= endAngle) {
                if (i != selectedItemIndex) {
                    previousSelectedItemIndex = selectedItemIndex;
                    selectedItemIndex = i;
                    selectedItemIndices.put(selectedCategory, selectedItemIndex);
                    if (selectedItemIndex != -1) {
                        playSelectionSound();
                    }
                }
                return;
            }
        }

        selectedItemIndex = -1;
    }

    private void renderWheelBackground(GuiGraphics guiGraphics, int centerX, int centerY, float radius, float opacity) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        poseStack.scale(scale, scale, 1.0f);
        // 只渲染一个简单的圆形背景
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        // 中心点
        buffer.vertex(matrix, 0, 0, 0).color(0.1f, 0.1f, 0.1f, 0.4f * opacity).endVertex();

        // 边缘点
        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = (float) (radius * Math.cos(angle));
            float y = (float) (radius * Math.sin(angle));
            buffer.vertex(matrix, x, y, 0).color(0.2f, 0.2f, 0.2f, 0.3f * opacity).endVertex();
        }
        tessellator.end();
        poseStack.popPose();
    }

    private void renderCategories(GuiGraphics guiGraphics, int centerX, int centerY, float radius, float opacity) {
        List<String> categories = getCategories();
        int categoryCount = categories.size();
        if (categoryCount == 0) {
            renderEmptyCategoryHint(guiGraphics, centerX, centerY, opacity);
            return;
        }

        float angleStep = (float) (2 * Math.PI / categoryCount);

        for (int i = 0; i < categoryCount; i++) {
            float angle = i * angleStep;
            float midAngle = angle + angleStep / 2;

            boolean isSelected = categories.get(i).equals(selectedCategory);

            // 统一使用相同的选中动画效果
            float itemScale = isSelected ?
                    getSelectionScale() * (1.0f + SELECTION_SCALE_BOOST) : 1.0f;

            // 计算位置 - 选中项抬升
            float elevation = isSelected ? SELECTION_ELEVATION : 0f;
            float iconRadius = radius * 0.7f;
            int x = centerX + (int) (iconRadius * Math.cos(midAngle) * scale);
            int y = centerY + (int) (iconRadius * Math.sin(midAngle) * scale - elevation);

            // 渲染分类图标和文字（不渲染扇形背景）
            renderCategoryIcon(guiGraphics, categories.get(i), x, y, scale * itemScale, opacity);
            renderCategoryText(guiGraphics, categories.get(i), x, y, opacity, isSelected);

            // 只在选中时渲染边缘指示器
            if (isSelected) {
                renderSelectionIndicator(guiGraphics, centerX, centerY, radius, opacity);
            }
        }
    }

    private void renderSector(GuiGraphics guiGraphics, int centerX, int centerY, float baseRadius,
                              float startAngle, float endAngle, float brightness, float opacity, float scaleFactor) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        // 应用扇形单独的缩放
        float scaledRadius = baseRadius * scaleFactor;
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        // 中心点
        buffer.vertex(matrix, 0, 0, 0).color(brightness, brightness, brightness, opacity * 0.8f).endVertex();
        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            float x = (float) (scaledRadius * Math.cos(angle) * scale);
            float y = (float) (scaledRadius * Math.sin(angle) * scale);
            buffer.vertex(matrix, x, y, 0).color(brightness, brightness, brightness, opacity * 0.6f).endVertex();
        }
        tessellator.end();

        // 添加选中边框效果
        if (scaleFactor > 1.0f && ConfigManager.areAnimationsEnabled()) {
            renderSectorBorder(guiGraphics, centerX, centerY, baseRadius, startAngle, endAngle,
                    opacity, scaleFactor);
        }

        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private void renderSectorBorder(GuiGraphics guiGraphics, int centerX, int centerY, float baseRadius,
                                    float startAngle, float endAngle, float opacity, float scaleFactor) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        float scaledRadius = baseRadius * scaleFactor;
        float borderWidth = 2.0f;
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        // 渲染边框
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        int borderColor = 0xFFFFFF | ((int)(opacity * 200) << 24);
        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;

            // 外边缘
            float outerX = (float) (scaledRadius * Math.cos(angle) * scale);
            float outerY = (float) (scaledRadius * Math.sin(angle) * scale);
            buffer.vertex(matrix, outerX, outerY, 0).color(borderColor).endVertex();

            // 内边缘
            float innerX = (float) ((scaledRadius - borderWidth) * Math.cos(angle) * scale);
            float innerY = (float) ((scaledRadius - borderWidth) * Math.sin(angle) * scale);
            buffer.vertex(matrix, innerX, innerY, 0).color(borderColor).endVertex();
        }
        tessellator.end();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private void renderItemList(GuiGraphics guiGraphics, int centerX, int centerY, float radius, float opacity) {
        List<ItemStack> items = getItemsForCategory(selectedCategory);
        if (items.isEmpty()) {
            renderEmptyItemListHint(guiGraphics, centerX, centerY, opacity);
            return;
        }

        float animatedRadius = radius * itemListScale;
        float animatedOpacity = opacity * itemListOpacity;
        int itemsPerPage = MAX_ITEMS_PER_PAGE;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        float angleStep = (float) (2 * Math.PI / itemsPerPage);

        // 删除扇形背景渲染，只渲染物品

        // 渲染物品图标
        for (int i = startIndex; i < endIndex; i++) {
            ItemStack item = items.get(i);
            float angle = (i - startIndex) * angleStep;
            float midAngle = angle + angleStep / 2;
            float itemRadius = animatedRadius * 0.7f;

            boolean isSelected = (i == selectedItemIndex);

            // 统一使用相同的选中动画效果
            float itemScale = isSelected ?
                    getSelectionScale() * (1.0f + SELECTION_SCALE_BOOST) : 1.0f;

            // 计算位置 - 选中物品抬升
            float elevation = isSelected ? SELECTION_ELEVATION : 0f;
            int x = centerX + (int) (itemRadius * Math.cos(midAngle) * itemListScale);
            int y = centerY + (int) (itemRadius * Math.sin(midAngle) * itemListScale - elevation);

            renderItemWithCount(guiGraphics, item, x, y, itemScale * itemListScale,
                    animatedOpacity, isSelected);

            // 只在选中时渲染边缘指示器
            if (isSelected) {
                renderSelectionIndicator(guiGraphics, centerX, centerY, radius,animatedOpacity);
            }
        }
    }
    // 添加新的选择指示器渲染

    private void renderItemWithCount(GuiGraphics guiGraphics, ItemStack stack, int x, int y,
                                     float scale, float opacity, boolean isSelected) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        // 统一选中动画效果
        if (isSelected && ConfigManager.areAnimationsEnabled()) {
            // 上下浮动
            float bounce = (float) Math.sin(System.currentTimeMillis() / 150f) * 3f;
            poseStack.translate(0, bounce, 0);

            // 轻微旋转
            float rotation = (System.currentTimeMillis() % 2000) / 2000f * 10f - 5f;
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));

            // 脉动缩放
            float pulse = 1.0f + (float) Math.sin(System.currentTimeMillis() / 100f) * 0.1f;
            scale *= pulse;
        }

        poseStack.scale(scale, scale, 1.0f);
        poseStack.translate(-8, -8, 0);

        ItemStack displayStack = stack.copy();
        displayStack.setCount(1);

        // 渲染物品
        guiGraphics.renderItem(displayStack, 0, 0);
        guiGraphics.renderItemDecorations(mc.font, displayStack, 0, 0);

        // 显示数量
        if (stack.getCount() > 1) {
            String countText = formatLargeNumber(stack.getCount());
            int textX = 8 - mc.font.width(countText) / 2;
            int textY = 20;

            // 数量文字动画
            if (isSelected && ConfigManager.areAnimationsEnabled()) {
                textY += (float) Math.sin(System.currentTimeMillis() / 150f) * 2f;
            }

            int color = (0xFFFFFF | ((int)(opacity * 255) << 24));

            if (isSelected) {
                // 选中时数量文字更明显
                int glowColor = 0xFFFF00 | ((int)(opacity * 255) << 24);
                guiGraphics.drawString(mc.font, countText, textX + 1, textY + 1,
                        0x000000 | ((int)(opacity * 200) << 24), false);
                guiGraphics.drawString(mc.font, countText, textX, textY, glowColor, false);
            } else {
                guiGraphics.drawString(mc.font, countText, textX + 1, textY + 1,
                        0x000000 | ((int)(opacity * 128) << 24), false);
                guiGraphics.drawString(mc.font, countText, textX, textY, color, false);
            }
        }
        poseStack.popPose();
    }

    private String formatLargeNumber(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 1000000) {
            return String.format("%.1fk", count / 1000.0);
        } else {
            return String.format("%.1fM", count / 1000000.0);
        }
    }

    private void renderCategoryIcon(GuiGraphics guiGraphics, String category, int x, int y, float scale, float opacity) {
        ItemStack iconStack = getIconForCategory(category);
        if (!iconStack.isEmpty()) {
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(x, y, 0);

            // 添加选中动画效果
            boolean isSelected = category.equals(selectedCategory);
            if (isSelected) {
                // 旋转动画
                float rotation = (System.currentTimeMillis() % 2000) / 2000f * 360f;
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));

                // 轻微缩放脉冲
                float pulse = 1.0f + (float) Math.sin(System.currentTimeMillis() / 200f) * 0.1f;
                scale *= pulse;
            }

            poseStack.scale(scale, scale, 1.0f);
            poseStack.translate(-8, -8, 0);
            guiGraphics.renderItem(iconStack, 0, 0);
            guiGraphics.renderItemDecorations(mc.font, iconStack, 0, 0);
            poseStack.popPose();
        }
    }

    private void renderCategoryText(GuiGraphics guiGraphics, String category, int x, int y,
                                    float opacity, boolean isSelected) {
        String displayName = getCategoryDisplayName(category);
        int textX = x - mc.font.width(displayName) / 2;
        int textY = y + 20;

        // 统一选中动画效果
        if (isSelected && ConfigManager.areAnimationsEnabled()) {
            // 文字上下浮动
            textY += (float) Math.sin(System.currentTimeMillis() / 150f) * 2f;

            // 文字颜色脉冲
            float pulse = 0.6f + (float) Math.sin(System.currentTimeMillis() / 100f) * 0.4f;
            int pulseColor = (int)(pulse * 255) << 16 | (int)(pulse * 255) << 8 | 255;
            guiGraphics.drawString(mc.font, displayName, textX, textY,
                    pulseColor | ((int)(opacity * 255) << 24), true);
        } else {
            int color = (0xFFFFFF | ((int)(opacity * 255) << 24));
            guiGraphics.drawString(mc.font, displayName, textX, textY, color, true);
        }
    }

    private void updateCategorizedItems() {
        Player player = mc.player;
        if (player != null) {
            Inventory inventory = player.getInventory();
            List<ItemStack> allItems = new ArrayList<>();

            // 获取背包所有物品
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    allItems.add(stack);
                }
            }

            categorizedItems = ItemCategorizer.categorizeInventory(allItems);
        }
    }



    private void renderSelectionIndicator(GuiGraphics guiGraphics, int centerX, int centerY, float radius, float opacity) {
        // 如果选择方向为零（中心位置），不渲染指示器
        //if (selectionDirection.equals(Vec2.ZERO)) return;

        float distance = (float) Math.min(Math.sqrt(selectionDirection.x * selectionDirection.x + selectionDirection.y * selectionDirection.y), radius);
        double angle = Math.atan2(selectionDirection.y, selectionDirection.x);

        int indicatorX = centerX + (int) (distance * Math.cos(angle) * scale);
        int indicatorY = centerY + (int) (distance * Math.sin(angle) * scale);

        int dotSize = 4;
        guiGraphics.fill(indicatorX - dotSize, indicatorY - dotSize,
                indicatorX + dotSize, indicatorY + dotSize,
                0xFFFFFFFF | ((int)(opacity * 255) << 24));
    }

    private void renderPageInfo(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (isItemListMode) {
            int totalPages = (int) Math.ceil((double) getItemsForCategory(selectedCategory).size() / MAX_ITEMS_PER_PAGE);
            if (totalPages > 1) {
                String pageText = (currentPage + 1) + "/" + totalPages;
                int color = (0xFFFFFF | ((int)(opacity * 255) << 24));

                // 计算中心位置
                int centerX = screenWidth / 2;
                int centerY = screenHeight / 2;
                int textWidth = mc.font.width(pageText);

                // 在轮盘下方显示页码
                int textX = centerX - textWidth / 2;
                int textY = centerY + WHEEL_RADIUS + 20; // 轮盘半径 + 偏移量

                // 添加背景以提高可读性
                int padding = 2;
                int bgColor = (0x000000 | ((int)(opacity * 128) << 24));
                guiGraphics.fill(textX - padding, textY - padding,
                        textX + textWidth + padding, textY + mc.font.lineHeight + padding,
                        bgColor);

                guiGraphics.drawString(mc.font, pageText, textX, textY, color, false);
            }
        }
    }


    private void playSelectionSound() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8f));
    }

    private List<String> getCategories() {
        List<String> configuredCategories = ConfigManager.getCategories();
        int maxCategories = ConfigManager.getMaxCategories();

        // 限制分类数量
        if (configuredCategories.size() > maxCategories) {
            configuredCategories = configuredCategories.subList(0, maxCategories);
        }

        // 计算总页数
        categoryTotalPages = (int) Math.ceil((double) configuredCategories.size() / getMaxCategoriesPerPage());
        // 确保当前页码在有效范围内
        if (categoryCurrentPage >= categoryTotalPages) {
            categoryCurrentPage = Math.max(0, categoryTotalPages - 1);
        }
        if (categoryCurrentPage < 0) {
            categoryCurrentPage = 0;
        }
        // 返回当前页的分类
        int startIndex = categoryCurrentPage * getMaxCategoriesPerPage();
        int endIndex = Math.min(startIndex + getMaxCategoriesPerPage(), configuredCategories.size());
        if (startIndex >= configuredCategories.size()) {
            return Collections.emptyList();
        }
        return configuredCategories.subList(startIndex, endIndex);
    }

    public void nextCategoryPage() {
        if (!isItemListMode && categoryTotalPages > 1) {
            int newPage = (categoryCurrentPage + 1) % categoryTotalPages;
            if (newPage != categoryCurrentPage) {
                categoryCurrentPage = newPage;
                playPageTurnSound();
            }
        }
    }
    public void previousCategoryPage() {
        if (!isItemListMode && categoryTotalPages > 1) {
            int newPage = (categoryCurrentPage - 1 + categoryTotalPages) % categoryTotalPages;
            if (newPage != categoryCurrentPage) {
                categoryCurrentPage = newPage;
                playPageTurnSound();
            }
        }
    }
    private void playPageTurnSound() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.BOOK_PAGE_TURN, 0.8f
        ));
    }

    private ItemStack getIconForCategory(String category) {
        ItemCategorizer.Category cat = ItemCategorizer.Category.fromName(category);
        return new ItemStack(cat.getIconItem());
    }

    private String getCategoryDisplayName(String category) {
        ItemCategorizer.Category cat = ItemCategorizer.Category.fromName(category);
        // 使用翻译键而不是硬编码文本
        return Component.translatable("swiftwheel.category." + cat.getName()).getString();
    }

    private List<ItemStack> getItemsForCategory(String category) {
        ItemCategorizer.Category cat = ItemCategorizer.Category.fromName(category);
        return categorizedItems.getOrDefault(cat, Collections.emptyList());
    }
    public void openWheel() {
        centerX = (int) mc.mouseHandler.xpos();
        centerY = (int) mc.mouseHandler.ypos();
        isWheelOpen = true;
        isItemListMode = false;
        currentPage = 0;
        categoryCurrentPage = 0; // 重置分类页码
        openTime = System.currentTimeMillis();
        updateCategorizedItems(); // 更新分类物品
        resetSelectionState();
        playOpenSound();
    }

    public void closeWheel() {
        isWheelOpen = false;
        openTime = System.currentTimeMillis();
        closeItemPreview(); // 关闭物品特写
        resetSelectionState();
        playCloseSound();
    }

    public void enterItemListMode() {
        if (!selectedCategory.isEmpty()) {
            List<ItemStack> items = getItemsForCategory(selectedCategory);
            if (items.isEmpty()) {
                // 如果分类为空，播放错误音效并阻止进入
                playErrorSound();

            }

            isItemListMode = true;
            currentPage = 0;
            itemListAnimationProgress = 0f;
            itemListScale = 0f;
            itemListOpacity = 0f;
            openTime = System.currentTimeMillis();
            // 播放进入二级菜单音效
            playSecondaryOpenSound();
        }
    }
    private void playSecondaryOpenSound() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK, 1.2f));
    }
    private void playErrorSound() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f
        ));
    }

    private void playOpenSound() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_TOAST_IN, 1.2f));
    }

    private void playCloseSound() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_TOAST_OUT, 1.2f));
    }

    public void exitItemListMode() {
        isItemListMode = false;
        currentPage = 0;
        categoryCurrentPage = 0; // 重置分类页码
        closeItemPreview(); // 关闭物品特写
    }

    public void nextPage() {
        if (isItemListMode) {
            int totalPages = (int) Math.ceil((double) getItemsForCategory(selectedCategory).size() / MAX_ITEMS_PER_PAGE);
            if (totalPages > 0) {
                currentPage = (currentPage + 1) % totalPages;
            }
        }
    }

    public void previousPage() {
        if (isItemListMode) {
            int totalPages = (int) Math.ceil((double) getItemsForCategory(selectedCategory).size() / MAX_ITEMS_PER_PAGE);
            if (totalPages > 0) {
                currentPage = (currentPage - 1 + totalPages) % totalPages;
            }
        }
    }

    public boolean isWheelOpen() {
        return isWheelOpen || animationProgress > 0;
    }

    public boolean isItemListMode() {
        return isItemListMode;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public ItemStack getSelectedItem() {
        if (isItemListMode && !selectedCategory.isEmpty()) {
            List<ItemStack> items = getItemsForCategory(selectedCategory);
            int selectedIndex = selectedItemIndices.getOrDefault(selectedCategory, 0);
            if (!items.isEmpty() && selectedIndex < items.size()) {
                return items.get(selectedIndex);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getBestItemForCategory(String category) {
        ItemCategorizer.Category cat = ItemCategorizer.Category.fromName(category);
        List<ItemStack> items = categorizedItems.get(cat);
        if (items == null || items.isEmpty()) return ItemStack.EMPTY;

        // 如果是背包分类，返回第一个物品
        if (cat == ItemCategorizer.Category.INVENTORY) {
            return items.isEmpty() ? ItemStack.EMPTY : items.get(0);
        }

        switch (cat) {
            case WEAPONS:
                return ItemCategorizer.selectBestWeapon(items);
            case PICKAXES:
            case AXES:
            case SHOVELS:
            case HOES:
                return ItemCategorizer.selectBestTool(items, cat.getClass());
            case FOOD:
                return ItemCategorizer.selectMostFood(items);
            case BLOCKS:
            case FUNCTIONAL_BLOCKS:
                return ItemCategorizer.selectMostBlocks(items);
            default:
                return items.get(0);
        }
    }

    public Vec2 getSelectionDirection() {
        return selectionDirection;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
