package fr.itemspawner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ItemSpawner.MOD_ID)
public class ItemSpawnerToolEvents {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !level.getServer().isSingleplayer()
                || !level.getServer().isSingleplayerOwner(player.getGameProfile())) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (!tool.getOrCreateTag().getBoolean("itemspawner:mine_3x3")) {
            return;
        }

        var look = player.getLookAngle();
        Direction facing = Direction.getNearest(look.x, look.y, look.z);
        Direction firstAxis = facing.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction secondAxis = switch (facing.getAxis()) {
            case X, Y -> Direction.SOUTH;
            case Z -> Direction.EAST;
        };

        BlockPos origin = event.getPos();
        for (int first = -1; first <= 1 && !tool.isEmpty(); first++) {
            for (int second = -1; second <= 1 && !tool.isEmpty(); second++) {
                if (first == 0 && second == 0) {
                    continue;
                }

                BlockPos target = origin.relative(firstAxis, first).relative(secondAxis, second);
                BlockState state = level.getBlockState(target);
                if (state.isAir() || state.getDestroySpeed(level, target) < 0 || !level.getWorldBorder().isWithinBounds(target)) {
                    continue;
                }

                Block.dropResources(state, level, target, level.getBlockEntity(target), player, tool);
                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                tool.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            }
        }
    }
}