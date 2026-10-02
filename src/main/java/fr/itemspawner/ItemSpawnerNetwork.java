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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ItemSpawner.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemSpawnerNetwork {
    private static final String PROTOCOL_VERSION = "3";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ItemSpawner.MOD_ID, "give_item"),
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
            if (player == null) {
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null || (!server.isSingleplayerOwner(player.getGameProfile()) && !player.hasPermissions(2))) {
                player.sendSystemMessage(Component.translatable("itemspawner.permission.denied"));
                return;
            }

            Item item = BuiltInRegistries.ITEM.get(packet.itemId());
            if (item == null || item == Items.AIR) {
                return;
            }

            int maxQuantity = Math.min(64, item.getMaxStackSize());
            int quantity = Mth.clamp(packet.quantity(), 1, 64);
            Enchantment enchantment = packet.enchantmentId() == null
                    ? null
                    : BuiltInRegistries.ENCHANTMENT.get(packet.enchantmentId());
            ItemStack testStack = new ItemStack(item);
            if (packet.enchantmentId() != null && (enchantment == null || !enchantment.canEnchant(testStack))) {
                return;
            }
            int enchantmentLevel = enchantment == null
                    ? 0
                    : Mth.clamp(packet.enchantmentLevel(), 1, enchantment.getMaxLevel());

            for (int remaining = quantity; remaining > 0; ) {
                int stackSize = Math.min(remaining, item.getMaxStackSize());
                ItemStack stack = new ItemStack(item, stackSize);
                if (enchantment != null) {
                    stack.enchant(enchantment, enchantmentLevel);
                }
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                remaining -= stackSize;
            }
        });
        context.setPacketHandled(true);
    }

    public static void sendToServer(ResourceLocation itemId, int quantity, ResourceLocation enchantmentId, int enchantmentLevel) {
        INSTANCE.sendToServer(new GiveItemPacket(itemId, quantity, enchantmentId, enchantmentLevel));
    }

    public record GiveItemPacket(ResourceLocation itemId, int quantity, ResourceLocation enchantmentId, int enchantmentLevel) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(itemId);
            buffer.writeVarInt(quantity);
            buffer.writeBoolean(enchantmentId != null);
            if (enchantmentId != null) {
                buffer.writeResourceLocation(enchantmentId);
                buffer.writeVarInt(enchantmentLevel);
            }
        }

        public static GiveItemPacket decode(FriendlyByteBuf buffer) {
            ResourceLocation itemId = buffer.readResourceLocation();
            int quantity = buffer.readVarInt();
            ResourceLocation enchantmentId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            int enchantmentLevel = enchantmentId == null ? 0 : buffer.readVarInt();
            return new GiveItemPacket(itemId, quantity, enchantmentId, enchantmentLevel);
        }
    }
}
