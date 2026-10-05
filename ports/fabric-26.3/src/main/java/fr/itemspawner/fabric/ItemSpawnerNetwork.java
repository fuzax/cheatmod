package fr.itemspawner.fabric;

import fr.itemspawner.ItemSpawnerGivePayload;
import fr.itemspawner.ItemSpawnerServerActions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ItemSpawnerNetwork {
    private ItemSpawnerNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ItemSpawnerGivePayload.TYPE, ItemSpawnerGivePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ItemSpawnerGivePayload.TYPE, (payload, context) ->
            context.server().execute(() -> ItemSpawnerServerActions.giveItem(context.server(), context.player(), payload)));
    }
}