package fr.itemspawner.fabric;

import fr.itemspawner.ItemSpawnerToolData;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ItemSpawnerToolEvents {
    private ItemSpawnerToolEvents() {
    }

    public static boolean onBlockBreak(
            Level world,
            net.minecraft.world.entity.player.Player player,
            BlockPos origin,
            BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity
    ) {
        if (!(world instanceof ServerLevel level)
                || !(player instanceof ServerPlayer serverPlayer)
                || !level.getServer().isSingleplayer()
                || !level.getServer().isSingleplayerOwner(serverPlayer.nameAndId())) {
            return true;
        }

        ItemStack tool = serverPlayer.getMainHandItem();
        if (!ItemSpawnerToolData.isMine3x3(tool)) {
            return true;
        }

        var look = serverPlayer.getLookAngle();
        Direction facing = Direction.getNearest(
            (int) Math.round(look.x * 1000),
            (int) Math.round(look.y * 1000),
            (int) Math.round(look.z * 1000),
            null
        );
        Direction firstAxis = facing.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction secondAxis = switch (facing.getAxis()) {
            case X, Y -> Direction.SOUTH;
            case Z -> Direction.EAST;
        };

        for (int first = -1; first <= 1 && !tool.isEmpty(); first++) {
            for (int second = -1; second <= 1 && !tool.isEmpty(); second++) {
                if (first == 0 && second == 0) {
                    continue;
                }

                BlockPos target = origin.relative(firstAxis, first).relative(secondAxis, second);
                BlockState targetState = level.getBlockState(target);
                if (targetState.isAir()
                        || targetState.getDestroySpeed(level, target) < 0
                        || !level.getWorldBorder().isWithinBounds(target)) {
                    continue;
                }

                Block.dropResources(targetState, level, target, level.getBlockEntity(target), serverPlayer, tool);
                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                tool.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }
}