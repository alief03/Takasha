package net.sakura.touhou.compat.tlmo.beacon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.sakura.touhou.compat.enchant.MaidEnchantBridge;
import net.sakura.touhou.entity.EntityMaid;
import net.sakura.touhou.registry.ModItems;

import java.util.List;

/**
 * BeaconEffectGeneratorBauble
 * Menghasilkan aura efek beacon dari item Mobile Beacon saat berada di maid.
 * Sesuai logika com.mastermarisa.maidbeacon.item.bauble.BeaconEffectGeneratorBauble.
 */
public class BeaconEffectGeneratorBauble {

    private static final double EFFECT_RANGE = 16.0;

    public static boolean hasBeacon(EntityMaid maid) {
        if (maid == null) return false;
        var inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ModItems.MOBILE_BEACON)) {
                return true;
            }
        }
        return false;
    }

    public static void tickBeaconAura(EntityMaid maid) {
        if (maid == null || maid.level().isClientSide() || !(maid.level() instanceof ServerLevel level)) {
            return;
        }

        // Jalankan tiap 80 tick (4 detik)
        if (maid.tickCount % 80 != 0) {
            return;
        }

        if (!hasBeacon(maid)) {
            return;
        }

        // Terapkan efek aura ke maid dan entitas sekutu
        AABB box = maid.getBoundingBox().inflate(EFFECT_RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, entity -> {
            if (entity == maid) return true;
            if (entity instanceof Player player && maid.isOwnedBy(player)) return true;
            return MaidEnchantBridge.isFriendly(maid, entity);
        });

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0, true, true));
        }

        // Efek partikel aura beacon lembut
        level.sendParticles(ParticleTypes.END_ROD, maid.getX(), maid.getY() + 1.8, maid.getZ(), 4, 0.2, 0.1, 0.2, 0.02);
    }
}
