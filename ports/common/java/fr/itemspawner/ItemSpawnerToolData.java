package fr.itemspawner;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ItemSpawnerToolData {
    private static final String MINE_3X3_KEY = "itemspawner:mine_3x3";

    private ItemSpawnerToolData() {
    }

    public static void markMine3x3(ItemStack stack) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
                customData -> customData.update(tag -> tag.putBoolean(MINE_3X3_KEY, true)));
    }

    public static boolean isMine3x3(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.getBoolean(MINE_3X3_KEY);
    }
}