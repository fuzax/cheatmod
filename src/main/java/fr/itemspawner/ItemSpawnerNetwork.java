package fr.itemspawner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ItemSpawner.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemSpawnerNetwork {
    private static final String PROTOCOL_VERSION = "4";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ItemSpawner.MOD_ID, "give_item"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        // The actual message registration happens during FMLCommonSetupEvent.
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        INSTANCE.registerMessage(0, GiveItemPacket.class, GiveItemPacket::encode, GiveItemPacket::decode, ItemSpawnerNetwork::handleMessage);
    }

    private static void handleMessage(GiveItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet == null || packet.itemId() == null || packet.enchantments() == null) {
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null || !server.isSingleplayer() || !server.isSingleplayerOwner(player.getGameProfile())) {
                player.sendSystemMessage(Component.translatable("itemspawner.permission.denied"));
                return;
            }

            Item item = BuiltInRegistries.ITEM.get(packet.itemId());
            if (item == null || item == Items.AIR) {
                return;
            }

            int quantity = Mth.clamp(packet.quantity(), 1, 64);
            ItemStack testStack = new ItemStack(item);
            Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
            packet.enchantments().entrySet().stream().limit(64).forEach(entry -> {
                if (entry == null || entry.getKey() == null) {
                    return;
                }
                Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(entry.getKey());
                if (enchantment != null && enchantment.canEnchant(testStack)) {
                    enchantments.put(enchantment, Mth.clamp(entry.getValue(), 1, enchantment.getMaxLevel()));
                }
            });
            if (enchantments.size() != packet.enchantments().size()) {
                player.sendSystemMessage(Component.translatable("itemspawner.enchantment.incompatible"));
            }

            for (int remaining = quantity; remaining > 0; ) {
                int stackSize = Math.min(remaining, item.getMaxStackSize());
                ItemStack stack = new ItemStack(item, stackSize);
                if (packet.mine3x3()) {
                    stack.getOrCreateTag().putBoolean("itemspawner:mine_3x3", true);
                }
                enchantments.forEach(stack::enchant);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                remaining -= stackSize;
            }
        });
        context.setPacketHandled(true);
    }

    public static void sendToServer(ResourceLocation itemId, int quantity, Map<ResourceLocation, Integer> enchantments, boolean mine3x3) {
        INSTANCE.sendToServer(new GiveItemPacket(itemId, quantity, enchantments, mine3x3));
    }

    public record GiveItemPacket(ResourceLocation itemId, int quantity, Map<ResourceLocation, Integer> enchantments, boolean mine3x3) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(itemId);
            buffer.writeVarInt(quantity);
            int count = Math.min(64, enchantments.size());
            buffer.writeVarInt(count);
            int written = 0;
            for (Map.Entry<ResourceLocation, Integer> entry : enchantments.entrySet()) {
                if (written++ >= count) {
                    break;
                }
                buffer.writeResourceLocation(entry.getKey());
                buffer.writeVarInt(entry.getValue());
            }
            buffer.writeBoolean(mine3x3);
        }

        public static GiveItemPacket decode(FriendlyByteBuf buffer) {
            ResourceLocation itemId = buffer.readResourceLocation();
            int quantity = buffer.readVarInt();
            int count = Mth.clamp(buffer.readVarInt(), 0, 64);
            Map<ResourceLocation, Integer> enchantments = new LinkedHashMap<>();
            for (int index = 0; index < count; index++) {
                enchantments.put(buffer.readResourceLocation(), buffer.readVarInt());
            }
            return new GiveItemPacket(itemId, quantity, enchantments, buffer.readBoolean());
        }
    }
}
