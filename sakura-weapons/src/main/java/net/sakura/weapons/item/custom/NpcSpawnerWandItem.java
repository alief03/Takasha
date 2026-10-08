package net.sakura.weapons.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.sakura.weapons.entity.TakashaNpcEntity;
import net.sakura.weapons.registry.ModEntities;

import java.util.List;

public class NpcSpawnerWandItem extends Item {

    public NpcSpawnerWandItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos spawnPos = clickedFace == Direction.UP ? clickedPos.above() : clickedPos.relative(clickedFace);

        // Check if player has permission to build/interact here (grief prevention / claims)
        if (!level.mayInteract(player, spawnPos)) {
            return InteractionResult.FAIL;
        }

        // Chunk / Area limit protection: prevent DoS/lag from spamming NPCs (max 16 in 32 block area)
        AABB limitBox = new AABB(spawnPos).inflate(16.0);
        List<TakashaNpcEntity> nearby = level.getEntitiesOfClass(TakashaNpcEntity.class, limitBox);
        boolean isOpOrCreative = player.isCreative();
        if (player instanceof ServerPlayer sp && sp.level().getServer() != null) {
            isOpOrCreative = isOpOrCreative || sp.level().getServer().getPlayerList().isOp(sp.nameAndId());
        }
        if (nearby.size() >= 16 && !isOpOrCreative) {
            player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.limit_reached"));
            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            TakashaNpcEntity npc = new TakashaNpcEntity(ModEntities.TAKASHA_NPC, serverLevel);
            double x = spawnPos.getX() + 0.5;
            double y = spawnPos.getY();
            double z = spawnPos.getZ() + 0.5;
            float yaw = player.getYRot() + 180.0f; // Face the spawning player

            npc.snapTo(x, y, z, yaw, 0.0f);
            npc.setYHeadRot(yaw);
            npc.setYBodyRot(yaw);
            npc.setYawRotation(yaw);
            npc.lockCurrentAsNewOrigin();
            npc.setOwner(player);
            npc.setPermissionMode(TakashaNpcEntity.PERM_PROTECTED_VIEW);
            npc.setShowName(true);
            npc.setCustomName(Component.literal(player.getName().getString() + "'s Mannequin"));

            serverLevel.addFreshEntity(npc);
            serverLevel.playSound(null, spawnPos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);

            if (!player.isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
