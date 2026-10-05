package fr.itemspawner.fabric;

import fr.itemspawner.ItemSpawnerGivePayload;
import fr.itemspawner.ItemSpawnerServerActions;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class ItemSpawnerNetwork {
    private ItemSpawnerNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ItemSpawnerGivePayload.TYPE, ItemSpawnerGivePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ItemSpawnerGivePayload.TYPE, (payload, context) ->
                context.server().execute(() -> ItemSpawnerServerActions.giveItem(context.player(), payload)));
    }

    public static void sendToServer(
            Identifier itemId,
            int quantity,
            Map<Identifier, Integer> enchantments,
            boolean mine3x3
    ) {
        ClientPlayNetworking.send(new ItemSpawnerGivePayload(itemId, quantity, enchantments, mine3x3));
    }
}