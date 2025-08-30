// file: util/ItemCategorizer.java
package cn.adwadg.swiftwheel.utils;

import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potions;

import java.util.*;
import java.util.stream.Collectors;

public class ItemCategorizer {
    public enum Category {
        WEAPONS("weapons", Items.DIAMOND_SWORD),
        PICKAXES("pickaxes", Items.DIAMOND_PICKAXE),
        AXES("axes", Items.DIAMOND_AXE),
        SHOVELS("shovels", Items.DIAMOND_SHOVEL),
        HOES("hoes", Items.DIAMOND_HOE),
        BLOCKS("blocks", Items.STONE),
        FUNCTIONAL_BLOCKS("functional_blocks", Items.CRAFTING_TABLE),
        DECORATIVE_BLOCKS("decorative_blocks", Items.OAK_PLANKS),
        NATURAL_BLOCKS("natural_blocks", Items.GRASS_BLOCK),
        FOOD("food", Items.APPLE),
        POTIONS("potions", Items.POTION),
        ARMOR("armor", Items.DIAMOND_CHESTPLATE),
        TOOLS("tools", Items.SHEARS),
        RANGED_WEAPONS("ranged_weapons", Items.BOW),
        MATERIALS("materials", Items.IRON_INGOT),
        REDSTONE("redstone", Items.REDSTONE),
        TRANSPORTATION("transportation", Items.MINECART),
        INVENTORY("inventory", Items.CHEST),
        MISC("misc", Items.TORCH);
        private final String name;
        private final Item iconItem;
        Category(String name, Item iconItem) {
            this.name = name;
            this.iconItem = iconItem;
        }
        public String getName() {
            return name;
        }
        public Item getIconItem() {
            return iconItem;
        }
        public static Category fromName(String name) {
            for (Category category : values()) {
                if (category.name.equals(name)) {
                    return category;
                }
            }
            return WEAPONS;
        }
        public static List<String> getAllCategoryNames() {
            return Arrays.stream(values())
                    .map(Category::getName)
                    .collect(Collectors.toList());
        }
    }
    public static Category getCategoryForItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        // 武器分类
        if (item instanceof SwordItem || item instanceof TridentItem) {
            return Category.WEAPONS;
        }
        // 远程武器
        else if (item instanceof BowItem || item instanceof CrossbowItem) {
            return Category.RANGED_WEAPONS;
        }
        // 工具分类
        else if (item instanceof PickaxeItem) {
            return Category.PICKAXES;
        }
        else if (item instanceof AxeItem) {
            return Category.AXES;
        }
        else if (item instanceof ShovelItem) {
            return Category.SHOVELS;
        }
        else if (item instanceof HoeItem) {
            return Category.HOES;
        }
        // 其他工具
        else if (item instanceof ShearsItem || item instanceof FlintAndSteelItem ||
                item instanceof FishingRodItem || item instanceof LeadItem) {
            return Category.TOOLS;
        }
        // 盔甲分类
        else if (item instanceof ArmorItem) {
            return Category.ARMOR;
        }
        // 食物分类
        else if (item.getFoodProperties() != null) {
            return Category.FOOD;
        }
        // 药水分类
        else if (item instanceof PotionItem ||
                BuiltInRegistries.ITEM.getKey(item).getPath().contains("potion") ||
                item instanceof TippedArrowItem) {
            return Category.POTIONS;
        }
        // 材料分类
        else if (item instanceof TieredItem || item instanceof ArmorMaterial ||
                item instanceof DyeItem || isMaterialItem(item)) {
            return Category.MATERIALS;
        }
        // 红石相关
        else if (isRedstoneItem(item)) {
            return Category.REDSTONE;
        }
        // 运输工具
        else if (item instanceof MinecartItem || item instanceof BoatItem ||
                item instanceof SaddleItem) {
            return Category.TRANSPORTATION;
        }
        // 方块分类
        else if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) item;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());

            // 功能性方块
            if (isFunctionalBlock(id)) {
                return Category.FUNCTIONAL_BLOCKS;
            }
            // 装饰性方块
            else if (isDecorativeBlock(id)) {
                return Category.DECORATIVE_BLOCKS;
            }
            // 红石方块
            else if (isRedstoneBlock(id)) {
                return Category.REDSTONE;
            }
            return Category.BLOCKS;
        }
        // 其他物品归为杂项
        return Category.MISC;
    }
    private static boolean isMaterialItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        return path.contains("ingot") || path.contains("gem") || path.contains("nugget") ||
                path.contains("dust") || path.contains("crystal") || path.contains("shard") ||
                path.contains("rod") || path.contains("plate") || path.contains("wire");
    }
    private static boolean isRedstoneItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        return path.contains("redstone") || path.contains("comparator") ||
                path.contains("repeater") || path.contains("observer") ||
                path.contains("sensor") || path.contains("detector");
    }
    private static boolean isFunctionalBlock(ResourceLocation id) {
        String path = id.getPath();
        return path.contains("crafting_table") || path.contains("furnace") ||
                path.contains("chest") || path.contains("bed") || path.contains("door") ||
                path.contains("anvil") || path.contains("enchanting_table") ||
                path.contains("brewing_stand") || path.contains("beacon") ||
                path.contains("hopper") || path.contains("dropper") || path.contains("dispenser") ||
                path.contains("jukebox") || path.contains("lectern") || path.contains("loom") ||
                path.contains("smithing_table") || path.contains("stonecutter") ||
                path.contains("grindstone") || path.contains("composter");
    }
    private static boolean isDecorativeBlock(ResourceLocation id) {
        String path = id.getPath();
        return path.contains("carpet") || path.contains("banner") || path.contains("painting") ||
                path.contains("flower_pot") || path.contains("item_frame") || path.contains("glow_item_frame") ||
                path.contains("sign") || path.contains("bed") || path.contains("head") || path.contains("skull") ||
                path.contains("glass") || path.contains("stained_glass") || path.contains("pane") ||
                path.contains("wool") || path.contains("concrete") || path.contains("terracotta") ||
                path.contains("candle") || path.contains("lantern") || path.contains("torch");
    }
    private static boolean isRedstoneBlock(ResourceLocation id) {
        String path = id.getPath();
        return path.contains("redstone") || path.contains("piston") || path.contains("sticky_piston") ||
                path.contains("observer") || path.contains("dispenser") || path.contains("dropper") ||
                path.contains("hopper") || path.contains("comparator") || path.contains("repeater") ||
                path.contains("daylight_detector") || path.contains("tripwire") || path.contains("target");
    }
    public static Map<Category, List<ItemStack>> categorizeInventory(List<ItemStack> inventory) {
        Map<Category, List<ItemStack>> categorized = new EnumMap<>(Category.class);
        // 初始化所有分类
        for (Category category : Category.values()) {
            categorized.put(category, new ArrayList<>());
        }
        // 为背包分类添加所有物品（不分类）
        List<ItemStack> inventoryItems = new ArrayList<>();
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                // 添加到背包分类（原始物品，不合并）
                inventoryItems.add(stack.copy());

                // 同时进行正常分类
                Category category = getCategoryForItem(stack);
                if (category != null) {
                    if (!stack.isStackable() || hasSpecialProperties(stack)) {
                        categorized.get(category).add(stack.copy());
                    } else {
                        mergeStackableItem(categorized, category, stack);
                    }
                }
            }
        }

        // 设置背包分类
        categorized.put(Category.INVENTORY, inventoryItems);
        // 移除空分类（除了背包分类）
        categorized.entrySet().removeIf(entry ->
                entry.getValue().isEmpty() && entry.getKey() != Category.INVENTORY
        );
        return categorized;
    }

    private static void mergeStackableItem(Map<Category, List<ItemStack>> categorized, Category category, ItemStack stack) {
        List<ItemStack> items = categorized.get(category);

        // 如果是不可堆叠物品，直接添加不合并
        if (!stack.isStackable() || hasSpecialProperties(stack)) {
            items.add(stack.copy());
            return;
        }
        // 查找是否已有相同物品（只合并完全相同的可堆叠物品）
        for (ItemStack existing : items) {
            if (ItemStack.isSameItemSameTags(existing, stack) &&
                    existing.isStackable() &&
                    !hasSpecialProperties(existing)) {
                // 合并数量
                existing.grow(stack.getCount());
                return;
            }
        }
        // 没有找到相同物品，添加新物品
        items.add(stack.copy());
    }
    // 增强特殊属性检测
    private static boolean hasSpecialProperties(ItemStack stack) {
        return !stack.isStackable() ||
                stack.isEnchanted() ||
                stack.isDamaged() ||
                stack.hasCustomHoverName() ||
                (stack.getTag() != null && !stack.getTag().isEmpty()) ||
                // 检查耐久度差异
                (stack.getItem().canBeDepleted() && stack.getDamageValue() > 0) ||
                // 检查药水效果
                PotionUtils.getPotion(stack) != Potions.EMPTY;
    }

    // 选择最优物品的方法
    public static ItemStack selectBestWeapon(List<ItemStack> weapons) {
        return weapons.stream()
                .max(Comparator.comparingDouble(stack -> {
                    if (stack.getItem() instanceof SwordItem) {
                        return ((SwordItem) stack.getItem()).getDamage();
                    }
                    return 0;
                }))
                .orElse(ItemStack.EMPTY);
    }

    public static ItemStack selectBestTool(List<ItemStack> tools, Class<?> toolType) {
        return tools.stream()
                .max(Comparator.comparingDouble(stack -> {
                    if (stack.getItem() instanceof TieredItem) {
                        TieredItem tieredItem = (TieredItem) stack.getItem();
                        Tiers tier = (Tiers) tieredItem.getTier();
                        return tier.getLevel();
                    }
                    return 0;
                }))
                .orElse(ItemStack.EMPTY);
    }

    public static ItemStack selectMostFood(List<ItemStack> foods) {
        return foods.stream()
                .max(Comparator.comparingDouble(stack -> {
                    FoodProperties food = stack.getItem().getFoodProperties();
                    if (food != null) {
                        return food.getNutrition() + food.getSaturationModifier();
                    }
                    return 0;
                }))
                .orElse(ItemStack.EMPTY);
    }

    public static ItemStack selectMostBlocks(List<ItemStack> blocks) {
        return blocks.stream()
                .max(Comparator.comparingInt(ItemStack::getCount))
                .orElse(ItemStack.EMPTY);
    }
}
