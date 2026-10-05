package fr.itemspawner.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public final class ItemSpawnerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemSpawnerNetwork.register();
        PlayerBlockBreakEvents.BEFORE.register(ItemSpawnerToolEvents::onBlockBreak);
    }
}