package fr.itemspawner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemSpawnerServerActions {
    private ItemSpawnerServerActions() {
    }

    public static void giveItem(ServerPlayer player, ItemSpawnerGivePayload payload) {
        if (payload == null || payload.itemId() == null || payload.enchantments() == null) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null || !server.isSingleplayer() || !server.isSingleplayerOwner(player.getGameProfile())) {
            player.sendSystemMessage(Component.translatable("itemspawner.permission.denied"));
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(payload.itemId());
        if (item == null || item == Items.AIR) {
            return;
        }

        int quantity = Mth.clamp(payload.quantity(), 1, 64);
        ItemStack testStack = new ItemStack(item);
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        payload.enchantments().entrySet().stream().limit(64).forEach(entry -> {
            if (entry.getKey() == null || entry.getValue() == null) {
                return;
            }
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(entry.getKey());
            if (enchantment != null && enchantment.canEnchant(testStack)) {
                enchantments.put(enchantment, Mth.clamp(entry.getValue(), 1, enchantment.getMaxLevel()));
            }
        });
        if (enchantments.size() != payload.enchantments().size()) {
            player.sendSystemMessage(Component.translatable("itemspawner.enchantment.incompatible"));
        }

        for (int remaining = quantity; remaining > 0; ) {
            int stackSize = Math.min(remaining, item.getMaxStackSize());
            ItemStack stack = new ItemStack(item, stackSize);
            if (payload.mine3x3()) {
                ItemSpawnerToolData.markMine3x3(stack);
            }
            enchantments.forEach(stack::enchant);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= stackSize;
        }
    }
}