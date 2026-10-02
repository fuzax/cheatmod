package fr.itemspawner;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = ItemSpawner.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientModEvents {

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.itemspawner.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.itemspawner"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
    }

    @Mod.EventBusSubscriber(
            modid = ItemSpawner.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE,
            value = Dist.CLIENT
    )
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (OPEN_MENU.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.hasSingleplayerServer() && minecraft.player != null && minecraft.screen == null) {
                    minecraft.setScreen(new ItemSpawnerScreen());
                }
            }
        }
    }
}