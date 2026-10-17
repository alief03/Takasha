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
        // Tick listener for passive buffs (Paladin, Phantom, Berserker)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String fullSet = getEquippedSet(player);
                if (fullSet == null) continue;

                switch (fullSet) {
                    case "Paladin" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false, true));
                    }
                    case "Phantom" -> {
                        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, true, false, true));
                    }
                    case "Berserker" -> {
                        if (player.getHealth() <= player.getMaxHealth() * 0.5f) {
                            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1, true, false, true));
                        } else {
                            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0, true, false, true));
                        }
                    }
                }
            }
        });

        // Damage listener for Necromancer, Guardian, Voidwalker
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                String fullSet = getEquippedSet(player);
                if (fullSet != null) {
                    // Voidwalker: Immune to void & chance of evasion teleport
                    if ("Voidwalker".equals(fullSet)) {
                        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
                            player.teleportTo(player.getX(), 120.0, player.getZ());
                            player.sendSystemMessage(Component.literal("§d[Voidwalker] §fKekuatan Void menyelamatkanmu dari jurang kehampaan!"));
                            return false;
                        }
                        if (player.getRandom().nextFloat() < 0.20f) {
                            // 20% evasion teleport
                            double offsetX = (player.getRandom().nextDouble() - 0.5) * 8.0;
                            double offsetZ = (player.getRandom().nextDouble() - 0.5) * 8.0;
                            player.teleportTo(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                            player.sendOverlayMessage(Component.literal("§d★ EVASION! ★"));
                            return false;
                        }
                    }

                    // Guardian: Cheat Death
                    if ("Guardian".equals(fullSet) && (player.getHealth() - amount) <= 0.0f) {
                        long now = System.currentTimeMillis();
                        long lastUsed = GUARDIAN_COOLDOWN.getOrDefault(player.getUUID(), 0L);
                        if (now - lastUsed > 300_000L) { // 5 minutes cooldown
                            GUARDIAN_COOLDOWN.put(player.getUUID(), now);
                            player.setHealth(player.getMaxHealth() * 0.4f);
                            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 3, false, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 2, false, false, true));

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

                            if (player.level() instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                        player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                            }

                            player.sendSystemMessage(Component.literal("§6§l[Guardian Set] §eFortress Aegis menahan serangan mematikan!"));
                            return false;
                        }
                    }

                    // Necromancer: Wither attacker
                    if ("Necromancer".equals(fullSet) && source.getEntity() instanceof LivingEntity attacker) {
                        attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                    }
                }
            }

            // Necromancer Lifesteal on Attack
            if (source.getEntity() instanceof ServerPlayer attacker) {
                String fullSet = getEquippedSet(attacker);
                if ("Necromancer".equals(fullSet)) {
                    float healAmount = amount * 0.15f;
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
