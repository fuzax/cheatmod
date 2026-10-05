package fr.itemspawner.fabric;

import fr.itemspawner.ItemSpawnerGivePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class FabricClientNetwork {
    private FabricClientNetwork() {
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