package fr.itemspawner.neoforge;

import fr.itemspawner.ItemSpawnerGivePayload;
import fr.itemspawner.ItemSpawnerServerActions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.common.NeoForge;

@Mod("itemspawner")
public final class ItemSpawnerNeoForge {
    public ItemSpawnerNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(ItemSpawnerNeoForge::onBlockBreak);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ItemSpawnerGivePayload.TYPE, ItemSpawnerGivePayload.STREAM_CODEC, (payload, context) -> {
            ServerPlayer player = context.player();
            context.enqueueWork(() -> ItemSpawnerServerActions.giveItem(player.server, player, payload));
        });
    }

    private static void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        ItemSpawnerNeoForgeToolEvents.onBlockBreak(event);
    }
}