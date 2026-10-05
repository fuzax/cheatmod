package fr.itemspawner.fabric;

import fr.itemspawner.ItemSpawnerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ItemSpawnerFabricClient implements ClientModInitializer {
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("itemspawner", "main")
        );
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.itemspawner.open",
                GLFW.GLFW_KEY_K,
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