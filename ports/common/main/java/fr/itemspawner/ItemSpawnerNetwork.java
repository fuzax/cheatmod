package fr.itemspawner;

import net.minecraft.resources.Identifier;

import java.util.Map;

public final class ItemSpawnerNetwork {
    private static Sender sender = (itemId, quantity, enchantments, mine3x3) -> {
    };

    private ItemSpawnerNetwork() {
    }

    public static void setSender(Sender sender) {
        ItemSpawnerNetwork.sender = sender;
    }

    public static void sendToServer(
            Identifier itemId,
            int quantity,
            Map<Identifier, Integer> enchantments,
            boolean mine3x3
    ) {
        sender.send(itemId, quantity, enchantments, mine3x3);
    }

    @FunctionalInterface
    public interface Sender {
        void send(Identifier itemId, int quantity, Map<Identifier, Integer> enchantments, boolean mine3x3);
    }
}