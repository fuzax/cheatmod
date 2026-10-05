package fr.itemspawner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.util.Prediction;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemSpawnerServerActions {
    private ItemSpawnerServerActions() {
    }

    public static void giveItem(MinecraftServer server, ServerPlayer player, ItemSpawnerGivePayload payload) {
        if (payload == null || payload.itemId() == null || payload.enchantments() == null) {
            return;
        }

        if (!server.isSingleplayer() || !server.isSingleplayerOwner(player.nameAndId())) {
            player.sendSystemMessage(Component.translatable("itemspawner.permission.denied"));
            return;
        }

        Item item = BuiltInRegistries.ITEM.getValue(payload.itemId());
        if (item == null || item == Items.AIR) {
            return;
        }

        int quantity = Mth.clamp(payload.quantity(), 1, 64);
        ItemStack testStack = new ItemStack(item);
        Registry<Enchantment> enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<Holder<Enchantment>, Integer> enchantments = new LinkedHashMap<>();
        payload.enchantments().entrySet().stream().limit(64).forEach(entry -> {
            if (entry.getKey() == null || entry.getValue() == null) {
                return;
            }
            Holder<Enchantment> enchantment = enchantmentRegistry.get(entry.getKey()).orElse(null);
            if (enchantment != null && enchantment.value().canEnchant(testStack)) {
                enchantments.put(enchantment, Mth.clamp(entry.getValue(), 1, enchantment.value().getMaxLevel()));
            }
        });
        if (enchantments.size() != payload.enchantments().size()) {
            player.sendSystemMessage(Component.translatable("itemspawner.enchantment.incompatible"));
        }

        for (int remaining = quantity; remaining > 0; ) {
            int stackSize = Math.min(remaining, testStack.getMaxStackSize());
            ItemStack stack = new ItemStack(item, stackSize);
            if (payload.mine3x3()) {
                ItemSpawnerToolData.markMine3x3(stack);
            }
            enchantments.forEach(stack::enchant);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false, Prediction.DELAYED);
            }
            remaining -= stackSize;
        }
    }
}