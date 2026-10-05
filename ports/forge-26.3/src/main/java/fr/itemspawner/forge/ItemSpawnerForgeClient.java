package fr.itemspawner.forge;

import fr.itemspawner.ItemSpawnerNetwork;
import fr.itemspawner.ItemSpawnerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "itemspawner", value = Dist.CLIENT)
public final class ItemSpawnerForgeClient {
    private static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.itemspawner.open",
            75,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("itemspawner", "main"))
    );

    private ItemSpawnerForgeClient() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        ItemSpawnerNetwork.setSender(ForgeClientNetwork::sendToServer);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (OPEN_MENU.consumeClick() && minecraft.hasSingleplayerServer() && minecraft.player != null) {
            minecraft.setScreenAndShow(new ItemSpawnerScreen());
        }
    }
}