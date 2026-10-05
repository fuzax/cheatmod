package fr.itemspawner.neoforge;

import fr.itemspawner.ItemSpawnerGivePayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public final class NeoForgeClientNetwork {
    private NeoForgeClientNetwork() {
    }

    public static void sendToServer(
            Identifier itemId,
            int quantity,
            Map<Identifier, Integer> enchantments,
            boolean mine3x3
    ) {
        PacketDistributor.sendToServer(new ItemSpawnerGivePayload(itemId, quantity, enchantments, mine3x3));
    }
}