package fr.itemspawner.forge;

import fr.itemspawner.ItemSpawnerGivePayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;

public final class ForgeClientNetwork {
    private ForgeClientNetwork() {
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