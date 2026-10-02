package fr.itemspawner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ItemSpawner.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemSpawnerNetwork {
    private static final String PROTOCOL_VERSION = "1";
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

            Item item = BuiltInRegistries.ITEM.get(packet.itemId());
            if (item == null || item == Items.AIR) {
                return;
            }

            ItemStack stack = new ItemStack(item, 1);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        });
        context.setPacketHandled(true);
    }

    public static void sendToServer(ResourceLocation itemId) {
        INSTANCE.sendToServer(new GiveItemPacket(itemId));
    }

    public record GiveItemPacket(ResourceLocation itemId) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(itemId);
        }

        public static GiveItemPacket decode(FriendlyByteBuf buffer) {
            return new GiveItemPacket(buffer.readResourceLocation());
        }
    }
}
