package fr.itemspawner.forge;

import fr.itemspawner.ItemSpawnerGivePayload;
import fr.itemspawner.ItemSpawnerServerActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.network.RegisterPayloadHandlersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.registration.PayloadRegistrar;

@Mod("itemspawner")
public final class ItemSpawnerForge {
    public ItemSpawnerForge(IEventBus modEventBus) {
        modEventBus.addListener(this::registerPayloads);
        MinecraftForge.EVENT_BUS.addListener(ItemSpawnerForge::onBlockBreak);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ItemSpawnerGivePayload.TYPE, ItemSpawnerGivePayload.STREAM_CODEC, (payload, context) -> {
            ServerPlayer player = context.player();
            context.enqueueWork(() -> ItemSpawnerServerActions.giveItem(player.server, player, payload));
        });
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        ItemSpawnerForgeToolEvents.onBlockBreak(event);
    }
}