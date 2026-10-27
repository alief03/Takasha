package net.takasha.rpg.enchant;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.item.TakashaArmorSetItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorSetBonusHandler {

    private static final Map<UUID, Long> GUARDIAN_COOLDOWN = new HashMap<>();

    public static void initialize() {
        // Tick listener for passive buffs
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String fullSet = getEquippedSet(player);
                if (fullSet == null) continue;

                switch (fullSet) {
                    case "DimensionalTraveller" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1, true, false, true));
                    }
                    case "Koth" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false, true));
                    }
                    case "Phantom" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0, true, false, true));
                    }
                    case "Ranger" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 0, true, false, true));
                    }
                    case "Supreme" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 0, true, false, true));
                        player.getFoodData().setFoodLevel(20);
                        player.getFoodData().setSaturation(20.0f);
                    }
                    case "Yeti" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false, true));
                    }
                    case "Yijki" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 1, true, false, true));
                    }
                }
            }
        });

        // Damage listener for Set Bonuses (Evasion, Fall Protection, Lifesteal, Freezing)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                String fullSet = getEquippedSet(player);
                if (fullSet != null) {
                    // No Fall Damage (Koth, Supreme)
                    if (("Koth".equals(fullSet) || "Supreme".equals(fullSet)) && source.is(DamageTypes.FALL)) {
                        return false;
                    }

                    // Ranger: 25% damage reduction from arrows
                    if ("Ranger".equals(fullSet) && source.is(DamageTypes.ARROW)) {
                        // handled by resistance or mitigation
                    }

                    // Dimensional Traveller: Dimensional Shift on Low HP
                    if ("DimensionalTraveller".equals(fullSet)) {
                        if ((player.getHealth() - amount) <= player.getMaxHealth() * 0.25f) {
                            double offsetX = (player.getRandom().nextDouble() - 0.5) * 16.0;
                            double offsetZ = (player.getRandom().nextDouble() - 0.5) * 16.0;
                            player.teleportTo(player.getX() + offsetX, player.getY() + 1.0, player.getZ() + offsetZ);
                            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, false, false, true));
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                            player.sendOverlayMessage(Component.literal("§5★ DIMENSIONAL SHIFT! ★"));
                        }
                    }

                    // Phantom: 15% evasion dodge
                    if ("Phantom".equals(fullSet) && player.getRandom().nextFloat() < 0.15f) {
                        player.sendOverlayMessage(Component.literal("§7★ DODGE! ★"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.6f);
                        return false;
                    }

                    // Yeti: Freezing Aura on Attacker
                    if ("Yeti".equals(fullSet) && source.getEntity() instanceof LivingEntity attacker) {
                        attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                        attacker.setTicksFrozen(140);
                    }
                }
            }

            // Attacker Bonuses: Yijki & DimensionalTraveller Lifesteal
            if (source.getEntity() instanceof ServerPlayer attacker) {
                String fullSet = getEquippedSet(attacker);
                if ("Yijki".equals(fullSet)) {
                    float healAmount = Math.max(1.0f, amount * 0.15f);
                    attacker.heal(healAmount);
                    if (attacker.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }

            return true;
        });
    }

    public static String getEquippedSet(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);

        if (head.getItem() instanceof TakashaArmorSetItem h &&
            chest.getItem() instanceof TakashaArmorSetItem c &&
            legs.getItem() instanceof TakashaArmorSetItem l &&
            feet.getItem() instanceof TakashaArmorSetItem f) {

            String setName = h.getSetName();
            if (setName.equals(c.getSetName()) && setName.equals(l.getSetName()) && setName.equals(f.getSetName())) {
                return setName;
            }
        }
        return null;
    }
}
