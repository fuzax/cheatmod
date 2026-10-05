package fr.itemspawner.fabric;

import fr.itemspawner.ItemSpawnerScreen;
import fr.itemspawner.ItemSpawnerNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ItemSpawnerFabricClient implements ClientModInitializer {
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        ItemSpawnerNetwork.setSender(FabricClientNetwork::sendToServer);
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("itemspawner", "main")
        );
        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.itemspawner.open",
            75,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openMenu.consumeClick()
                    && client.hasSingleplayerServer()
                    && client.player != null) {
                client.setScreenAndShow(new ItemSpawnerScreen());
            }
        });
    }
}