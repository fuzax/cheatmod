package fr.itemspawner.neoforge;

import fr.itemspawner.ItemSpawnerNetwork;
import fr.itemspawner.ItemSpawnerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "itemspawner", value = Dist.CLIENT)
public final class ItemSpawnerNeoForgeClient {
    private static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.itemspawner.open",
            75,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("itemspawner", "main"))
    );

    private ItemSpawnerNeoForgeClient() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        ItemSpawnerNetwork.setSender(NeoForgeClientNetwork::sendToServer);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (OPEN_MENU.consumeClick() && minecraft.hasSingleplayerServer() && minecraft.player != null) {
            minecraft.setScreenAndShow(new ItemSpawnerScreen());
        }
    }
}