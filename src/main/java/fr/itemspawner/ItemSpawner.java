package fr.itemspawner;

import net.minecraftforge.fml.common.Mod;

@Mod(ItemSpawner.MOD_ID)
public class ItemSpawner {
    public static final String MOD_ID = "itemspawner";

    public ItemSpawner() {
        ItemSpawnerNetwork.register();
    }
}
