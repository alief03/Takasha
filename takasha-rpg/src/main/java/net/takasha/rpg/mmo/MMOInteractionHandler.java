package net.takasha.rpg.mmo;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.mythic.MythicSkillExecutor;
import net.takasha.rpg.mythic.SkillAuraManager;
import net.takasha.rpg.mythic.SkillExecutionContext;
import net.takasha.rpg.skills.AuraSkillType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MMOInteractionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-mmo-interaction");

    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Set<UUID> PREVIOUSLY_CROUCHING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Map<String, Long>> NEXT_TIMER_RUN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SNEAK_TAP = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SNEAK_HOLD_TICKS = new ConcurrentHashMap<>();
    private static final Set<UUID> SNEAK_HOLD_TRIGGERED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> LAST_ATTACK_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_AIR_SWING = new ConcurrentHashMap<>();

    public static void initialize() {
        LOGGER.info("Initializing MMOInteractionHandler for Takasha-RPG...");

        // 1. Right Click & Shift + Right Click
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!MMOItemData.hasMMOData(stack)) {
                return InteractionResult.PASS;
            }

            if (level.isClientSide()) {
                // Client must return SUCCESS for vanilla swords so Minecraft sends ServerboundUseItemPacket to the server
                return InteractionResult.SUCCESS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            // Consumable & Consumable Stats Handler (Restore HP, Mana, Food, Potion Effects, Commands, Cooldown)
            Optional<MMOItemData> optData = MMOItemData.readFromStack(stack);
            if (optData.isPresent()) {
                MMOItemData data = optData.get();
                if (isConsumable(data)) {
                    boolean consumed = handleConsumableUse(serverPlayer, stack, hand, data);
                    if (consumed) {
                        return InteractionResult.SUCCESS;
                    }
                }
            }

            boolean triggered = false;
            if (player.isCrouching()) {
                triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.SHIFT_RIGHT_CLICK, null);
                if (!triggered) {
                    triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.RIGHT_CLICK, null);
                }
            } else {
                triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.RIGHT_CLICK, null);
            }

            if (triggered) {
                serverPlayer.swing(hand, true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        // 2. Attack Entity (Left Click, Shift + Left Click, On Hit)
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            LAST_ATTACK_TICK.put(serverPlayer.getUUID(), serverPlayer.tickCount);

            ItemStack stack = player.getItemInHand(hand);
            if (!MMOItemData.hasMMOData(stack)) {
                return InteractionResult.PASS;
            }

            boolean triggered = false;
            if (player.isCrouching()) {
                triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.SHIFT_LEFT_CLICK, target);
                if (!triggered) {
                    triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.LEFT_CLICK, target);
                }
            } else {
                triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.LEFT_CLICK, target);
            }

            boolean onHitTriggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.ON_HIT, target);
            if (triggered || onHitTriggered) {
                // REA Parity: Return PASS so melee weapon attack executes alongside the triggered ability
                return InteractionResult.PASS;
            }

            return InteractionResult.PASS;
        });

        // 3. Attack Block (Left Click into blocks)
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            LAST_ATTACK_TICK.put(serverPlayer.getUUID(), serverPlayer.tickCount);

            ItemStack stack = player.getItemInHand(hand);
            if (!MMOItemData.hasMMOData(stack)) {
                return InteractionResult.PASS;
            }

            boolean triggered = false;
            if (player.isCrouching()) {
                triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.SHIFT_LEFT_CLICK, null);
            } else {
                triggered = tryTriggerAbility(serverPlayer, stack, MMOAbilityTrigger.LEFT_CLICK, null);
            }

            if (triggered) {
                serverPlayer.swing(hand, true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        // 4. Server Tick: Sneak transition & Passive/Timer abilities
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player == null || player.isRemoved() || player.hasDisconnected()) continue;

                ItemStack held = player.getMainHandItem();
                boolean hasMMO = MMOItemData.hasMMOData(held);
                UUID uuid = player.getUUID();

                // Sneak gestures: Double-Tap Sneak (Shift + Shift) & Hold Sneak (Hold Shift >= 10 ticks)
                if (player.isCrouching()) {
                    if (PREVIOUSLY_CROUCHING.add(uuid)) {
                        // Sneak tap event
                        long now = System.currentTimeMillis();
                        Long lastTap = LAST_SNEAK_TAP.get(uuid);
                        if (lastTap != null && (now - lastTap) <= 450) {
                            // Double Shift tap! (Shift + Shift) -> SNEAK trigger
                            LAST_SNEAK_TAP.remove(uuid);
                            SNEAK_HOLD_TICKS.put(uuid, -20); // Isolate gesture: prevent double-tap from bleeding into hold sneak
                            SNEAK_HOLD_TRIGGERED.add(uuid);
                            if (hasMMO) {
                                boolean cast = tryTriggerAbility(player, held, MMOAbilityTrigger.SNEAK, null);
                                if (cast) {
                                    player.swing(InteractionHand.MAIN_HAND, true);
                                }
                            }
                        } else {
                            LAST_SNEAK_TAP.put(uuid, now);
                        }
                    }

                    // Hold sneak tracking
                    int holdTicks = SNEAK_HOLD_TICKS.merge(uuid, 1, Integer::sum);
                    if (holdTicks >= 10 && SNEAK_HOLD_TRIGGERED.add(uuid)) {
                        // Held sneak for >= 10 ticks (0.5s)
                        if (hasMMO) {
                            boolean cast = tryTriggerAbility(player, held, MMOAbilityTrigger.SNEAK, null);
                            if (cast) {
                                player.swing(InteractionHand.MAIN_HAND, true);
                            }
                        }
                    }
                } else {
                    PREVIOUSLY_CROUCHING.remove(uuid);
                    SNEAK_HOLD_TICKS.remove(uuid);
                    SNEAK_HOLD_TRIGGERED.remove(uuid);
                }

                // Timer passives
                if (hasMMO) {
                    tickTimerAbilities(player, held);
                }
            }
        });
    }

    public static void onPlayerSwing(ServerPlayer player) {
        if (player == null || player.isRemoved()) return;

        UUID uuid = player.getUUID();
        if (player.tickCount == LAST_ATTACK_TICK.getOrDefault(uuid, -1)) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastSwing = LAST_AIR_SWING.getOrDefault(uuid, 0L);
        if (now - lastSwing < 200) {
            return;
        }
        LAST_AIR_SWING.put(uuid, now);

        ItemStack stack = player.getMainHandItem();
        if (!MMOItemData.hasMMOData(stack)) {
            return;
        }

        boolean triggered = false;
        if (player.isCrouching()) {
            triggered = tryTriggerAbility(player, stack, MMOAbilityTrigger.SHIFT_LEFT_CLICK, null);
            if (!triggered) {
                triggered = tryTriggerAbility(player, stack, MMOAbilityTrigger.LEFT_CLICK, null);
            }
        } else {
            triggered = tryTriggerAbility(player, stack, MMOAbilityTrigger.LEFT_CLICK, null);
        }

        if (triggered) {
            player.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    public static boolean tryTriggerNamedAbility(ServerPlayer player, ItemStack stack, String abilityName, @Nullable Entity target) {
        Optional<MMOItemData> dataOpt = MMOItemData.readFromStack(stack);
        if (dataOpt.isEmpty()) return false;

        MMOItemData data = dataOpt.get();
        MMOItemTemplate template = MMOItemManager.getTemplate(data.getType(), data.getId());
        if (template == null) {
            template = MMOItemManager.getTemplateById(data.getId());
        }
        if (template == null) return false;

        for (MMOItemTemplate.AbilityConfig ab : template.getAbilities()) {
            if (ab.getType().equalsIgnoreCase(abilityName) || ab.getAbilityKey().equalsIgnoreCase(abilityName)) {
                return executeAbility(player, template, ab, target);
            }
        }

        // Prefix match or subskill match: e.g. Hellfire_Overblast_CAST matches HELLFIRE_OVERBLAST
        for (MMOItemTemplate.AbilityConfig ab : template.getAbilities()) {
            String abType = ab.getType().toUpperCase(Locale.ROOT);
            String targetUpper = abilityName.toUpperCase(Locale.ROOT);
            if (targetUpper.startsWith(abType) || abType.startsWith(targetUpper)) {
                return executeAbilityWithCustomSkill(player, template, ab, abilityName, target);
            }
        }

        return false;
    }

    private static boolean executeAbilityWithCustomSkill(ServerPlayer player, MMOItemTemplate template,
                                                         MMOItemTemplate.AbilityConfig ab, String customSkillId,
                                                         @Nullable Entity target) {
        // Requirement Checks
        TakashaRpgData rpgData = TakashaRpgManager.getOrCreateData(player);
        if (template.getStats().containsKey(MMOStatType.REQUIRED_LEVEL)) {
            double reqLevel = template.getStats().get(MMOStatType.REQUIRED_LEVEL);
            if (rpgData.getTotalSkillLevel() < reqLevel) {
                player.sendSystemMessage(Component.literal("§c⛔ Butuh Total Skill Level §e" + (int) reqLevel + " §cuntuk menggunakan senjata ini!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return true;
            }
        }
        if (template.getStats().containsKey(MMOStatType.REQUIRED_FIGHTING)) {
            double reqFighting = template.getStats().get(MMOStatType.REQUIRED_FIGHTING);
            if (rpgData.getSkillLevel(AuraSkillType.FIGHTING) < reqFighting) {
                player.sendSystemMessage(Component.literal("§c⛔ Butuh Fighting Level §e" + (int) reqFighting + " §cuntuk menggunakan senjata ini!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return true;
            }
        }

        String cooldownKey = ab.getType();

        // Cooldown check
        if (isOnCooldown(player, cooldownKey)) {
            double remaining = getRemainingCooldownSeconds(player, cooldownKey);
            showCooldownMessage(player, cooldownKey, remaining);
            return false;
        }

        // Mana check
        double manaCost = ab.getManaCost();
        if (manaCost > 0) {
            if (!rpgData.consumeMana(manaCost)) {
                player.sendSystemMessage(Component.literal("§c💧 Mana tidak cukup! (§b" + (int) rpgData.getCurrentMana() + "§7/§b" + (int) rpgData.getMaxMana() + " Mana§c)"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return false;
            }
            TakashaRpgManager.syncToClient(player);
        }

        // Prepare context
        LivingEntity initialTarget = (target instanceof LivingEntity lt) ? lt : null;
        SkillExecutionContext ctx = new SkillExecutionContext((ServerLevel) player.level(), player, initialTarget);
        ctx.getParameters().putAll(ab.getExtraParams());
        if (ab.getDamage() > 0) ctx.setParameter("damage", ab.getDamage());
        if (ab.getBurnDuration() > 0) ctx.setParameter("burnduration", ab.getBurnDuration());
        if (ab.getBurnDamage() > 0) ctx.setParameter("burndamage", ab.getBurnDamage());

        // Execute custom skill
        boolean ok = MythicSkillExecutor.getInstance().executeSkill(customSkillId, ctx);
        if (ok) {
            double cd = ab.getCooldown();
            if (cd > 0) {
                setCooldown(player, cooldownKey, cd);
            }
            return true;
        }
        return false;
    }

    public static boolean tryTriggerAbility(ServerPlayer player, ItemStack stack, MMOAbilityTrigger trigger, @Nullable Entity target) {
        Optional<MMOItemData> dataOpt = MMOItemData.readFromStack(stack);
        if (dataOpt.isEmpty()) return false;

        MMOItemData data = dataOpt.get();
        MMOItemTemplate template = MMOItemManager.getTemplate(data.getType(), data.getId());
        if (template == null) {
            template = MMOItemManager.getTemplateById(data.getId());
        }
        if (template == null) return false;

        boolean anyTriggered = false;
        for (MMOItemTemplate.AbilityConfig ab : template.getAbilities()) {
            if (ab.getMode() != trigger) continue;

            if (executeAbility(player, template, ab, target)) {
                anyTriggered = true;
            }
        }

        return anyTriggered;
    }

    private static boolean executeAbility(ServerPlayer player, MMOItemTemplate template, MMOItemTemplate.AbilityConfig ab, @Nullable Entity target) {
        // Requirement Checks
        TakashaRpgData rpgData = TakashaRpgManager.getOrCreateData(player);
        if (template.getStats().containsKey(MMOStatType.REQUIRED_LEVEL)) {
            double reqLevel = template.getStats().get(MMOStatType.REQUIRED_LEVEL);
            if (rpgData.getTotalSkillLevel() < reqLevel) {
                player.sendSystemMessage(Component.literal("§c⛔ Butuh Total Skill Level §e" + (int) reqLevel + " §cuntuk menggunakan senjata ini!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return true;
            }
        }
        if (template.getStats().containsKey(MMOStatType.REQUIRED_FIGHTING)) {
            double reqFighting = template.getStats().get(MMOStatType.REQUIRED_FIGHTING);
            if (rpgData.getSkillLevel(AuraSkillType.FIGHTING) < reqFighting) {
                player.sendSystemMessage(Component.literal("§c⛔ Butuh Fighting Level §e" + (int) reqFighting + " §cuntuk menggunakan senjata ini!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return true;
            }
        }

        String skillId = ab.getType();

        // Cooldown check
        if (isOnCooldown(player, skillId)) {
            double remaining = getRemainingCooldownSeconds(player, skillId);
            showCooldownMessage(player, skillId, remaining);
            return false;
        }

        // Mana check
        double manaCost = ab.getManaCost();
        if (manaCost > 0) {
            if (!rpgData.consumeMana(manaCost)) {
                player.sendSystemMessage(Component.literal("§c💧 Mana tidak cukup! (§b" + (int) rpgData.getCurrentMana() + "§7/§b" + (int) rpgData.getMaxMana() + " Mana§c)"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return false;
            }
            TakashaRpgManager.syncToClient(player);
        }

        // Prepare context
        LivingEntity initialTarget = (target instanceof LivingEntity lt) ? lt : null;
        SkillExecutionContext ctx = new SkillExecutionContext((ServerLevel) player.level(), player, initialTarget);
        ctx.getParameters().putAll(ab.getExtraParams());
        if (ab.getDamage() > 0) ctx.setParameter("damage", ab.getDamage());
        if (ab.getBurnDuration() > 0) ctx.setParameter("burnduration", ab.getBurnDuration());
        if (ab.getBurnDamage() > 0) ctx.setParameter("burndamage", ab.getBurnDamage());

        // Execute skill
        boolean ok = MythicSkillExecutor.getInstance().executeSkill(skillId, ctx);
        if (ok) {
            double cd = ab.getCooldown();
            if (cd > 0) {
                setCooldown(player, skillId, cd);
            }
            return true;
        }
        return false;
    }

    private static void tickTimerAbilities(ServerPlayer player, ItemStack stack) {
        Optional<MMOItemData> dataOpt = MMOItemData.readFromStack(stack);
        if (dataOpt.isEmpty()) return;

        MMOItemData data = dataOpt.get();
        MMOItemTemplate template = MMOItemManager.getTemplate(data.getType(), data.getId());
        if (template == null) {
            template = MMOItemManager.getTemplateById(data.getId());
        }
        if (template == null) return;

        long now = System.currentTimeMillis();
        Map<String, Long> timers = NEXT_TIMER_RUN.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());

        for (MMOItemTemplate.AbilityConfig ab : template.getAbilities()) {
            if (ab.getMode() != MMOAbilityTrigger.TIMER) continue;
            // Skip gesture skills like FIRESTORM_VOLLEY (handled via Hold Shift)
            if ("FIRESTORM_VOLLEY".equalsIgnoreCase(ab.getType())) continue;

            String skillId = ab.getType();
            // REA Parity: In MMOItems, timer is configured in ticks (e.g. timer: 10 = 10 ticks = 500ms)
            long intervalMs;
            if (ab.getTimer() >= 1.0) {
                intervalMs = (long) (ab.getTimer() * 50L);
            } else if (ab.getTimer() > 0) {
                intervalMs = (long) (ab.getTimer() * 1000L);
            } else {
                intervalMs = 500L;
            }
            long nextRun = timers.getOrDefault(skillId, 0L);

            if (now >= nextRun) {
                timers.put(skillId, now + intervalMs);

                SkillExecutionContext ctx = new SkillExecutionContext((ServerLevel) player.level(), player, null);
                ctx.getParameters().putAll(ab.getExtraParams());
                if (ab.getDamage() > 0) ctx.setParameter("damage", ab.getDamage());
                if (ab.getBurnDuration() > 0) ctx.setParameter("burnduration", ab.getBurnDuration());
                if (ab.getBurnDamage() > 0) ctx.setParameter("burndamage", ab.getBurnDamage());

                MythicSkillExecutor.getInstance().executeSkill(skillId, ctx);
            }
        }
    }

    private static final Map<UUID, Map<String, Long>> LAST_COOLDOWN_NOTIF = new ConcurrentHashMap<>();

    private static void showCooldownMessage(ServerPlayer player, String skillKey, double remaining) {
        long now = System.currentTimeMillis();
        Map<String, Long> pNotifs = LAST_COOLDOWN_NOTIF.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        long last = pNotifs.getOrDefault(skillKey, 0L);
        if (now - last < 400) {
            return; // Throttle to avoid packet spam
        }
        pNotifs.put(skillKey, now);

        String name = skillKey.replace('_', ' ');
        Component msg = Component.literal("§c⏳ §e" + name + " §7cooldown: §c" + String.format(Locale.ROOT, "%.1f", remaining) + "s");
        player.sendSystemMessage(msg, true); // true = Action Bar overlay message
    }

    public static boolean isOnCooldown(Player player, String skillId) {
        Map<String, Long> pCds = COOLDOWNS.get(player.getUUID());
        if (pCds == null) return false;
        Long until = pCds.get(skillId.toUpperCase());
        return until != null && System.currentTimeMillis() < until;
    }

    public static double getRemainingCooldownSeconds(Player player, String skillId) {
        Map<String, Long> pCds = COOLDOWNS.get(player.getUUID());
        if (pCds == null) return 0.0;
        Long until = pCds.get(skillId.toUpperCase());
        if (until == null) return 0.0;
        long diff = until - System.currentTimeMillis();
        return diff > 0 ? diff / 1000.0 : 0.0;
    }

    public static void setCooldown(Player player, String skillId, double seconds) {
        if (seconds <= 0) return;
        COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                .put(skillId.toUpperCase(), System.currentTimeMillis() + (long) (seconds * 1000.0));
    }

    public static void clearPlayerData(UUID uuid) {
        COOLDOWNS.remove(uuid);
        PREVIOUSLY_CROUCHING.remove(uuid);
        NEXT_TIMER_RUN.remove(uuid);
        LAST_SNEAK_TAP.remove(uuid);
        SNEAK_HOLD_TICKS.remove(uuid);
        SNEAK_HOLD_TRIGGERED.remove(uuid);
    }

    public static boolean isConsumable(MMOItemData data) {
        if ("CONSUMABLE".equalsIgnoreCase(data.getType())) return true;
        Map<MMOStatType, Double> stats = data.getStats();
        return stats.containsKey(MMOStatType.RESTORE_HEALTH) ||
               stats.containsKey(MMOStatType.RESTORE_FOOD) ||
               stats.containsKey(MMOStatType.RESTORE_SATURATION) ||
               stats.containsKey(MMOStatType.RESTORE_MANA) ||
               stats.containsKey(MMOStatType.ITEM_COOLDOWN) ||
               !data.getPotionEffects().isEmpty() ||
               !data.getCommands().isEmpty();
    }

    public static boolean handleConsumableUse(ServerPlayer player, ItemStack stack, InteractionHand hand, MMOItemData data) {
        // Cooldown check
        double cd = data.getStats().getOrDefault(MMOStatType.ITEM_COOLDOWN, 0.0);
        long now = System.currentTimeMillis();
        if (cd > 0) {
            String cdKey = "ITEM_COOLDOWN:" + data.getType() + ":" + data.getId();
            Map<String, Long> pCds = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
            long last = pCds.getOrDefault(cdKey, 0L);
            if (now < last + (long) (cd * 1000L)) {
                long remainMs = (last + (long) (cd * 1000L)) - now;
                player.sendSystemMessage(Component.literal("§cItem ini sedang cooldown! (" + String.format(Locale.ROOT, "%.1f", remainMs / 1000.0) + "s)"));
                return true;
            }
            pCds.put(cdKey, now);
        }

        // 1. Health
        double restoreHp = data.getStats().getOrDefault(MMOStatType.RESTORE_HEALTH, 0.0);
        if (restoreHp > 0) {
            player.heal((float) restoreHp);
        }

        // 2. Food & Saturation
        double restoreFood = data.getStats().getOrDefault(MMOStatType.RESTORE_FOOD, 0.0);
        double restoreSat = data.getStats().getOrDefault(MMOStatType.RESTORE_SATURATION, 0.0);
        if (restoreFood > 0 || restoreSat > 0) {
            player.getFoodData().eat((int) restoreFood, (float) restoreSat);
        }

        // 3. Mana
        double restoreMana = data.getStats().getOrDefault(MMOStatType.RESTORE_MANA, 0.0);
        if (restoreMana > 0) {
            TakashaRpgData rpgData = TakashaRpgManager.getOrCreateData(player);
            rpgData.restoreMana(restoreMana);
            TakashaRpgManager.syncToClient(player);
        }

        // 4. Potion Effects
        List<String> pEffects = new ArrayList<>(data.getPotionEffects());
        if (pEffects.isEmpty()) {
            MMOItemTemplate tmpl = MMOItemManager.getTemplate(data.getType(), data.getId());
            if (tmpl != null) {
                pEffects.addAll(tmpl.getPotionEffects());
            }
        }
        for (String pe : pEffects) {
            applyPotionEffectString(player, pe);
        }

        // 5. Commands
        List<String> cmds = new ArrayList<>(data.getCommands());
        if (cmds.isEmpty()) {
            MMOItemTemplate tmpl = MMOItemManager.getTemplate(data.getType(), data.getId());
            if (tmpl != null) {
                cmds.addAll(tmpl.getCommands());
            }
        }
        for (String cmd : cmds) {
            String processed = cmd.replace("%player%", player.getScoreboardName());
            if (processed.startsWith("/")) processed = processed.substring(1);
            net.minecraft.server.MinecraftServer server = player.level().getServer();
            if (server != null) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), processed);
            }
        }

        // Play Eating/Drinking Sound & Particles
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8f, 1.0f);
        if (player.level() instanceof ServerLevel slevel) {
            slevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
        }

        // Deduct item if not creative
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.swing(hand, true);
        return true;
    }

    public static void applyPotionEffectString(LivingEntity entity, String pe) {
        if (pe == null || pe.isBlank()) return;
        String clean = pe.trim().replace(',', ':').replace(' ', ':');
        String[] parts = clean.split(":+");
        if (parts.length == 0) return;

        String name = parts[0].toLowerCase(Locale.ROOT);
        Identifier effectId = Identifier.tryParse(name.contains(":") ? name : "minecraft:" + name);
        if (effectId == null) return;

        Optional<Holder.Reference<MobEffect>> optEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (optEffect.isEmpty()) return;

        int amplifier = 0;
        int durationTicks = 200; // default 10s

        if (parts.length >= 2) {
            try {
                int p1 = Integer.parseInt(parts[1]);
                if (parts.length >= 3) {
                    int p2 = Integer.parseInt(parts[2]);
                    if (p2 > p1) {
                        amplifier = Math.max(0, p1 - 1);
                        durationTicks = p2 * 20;
                    } else {
                        amplifier = Math.max(0, p2 - 1);
                        durationTicks = p1 * 20;
                    }
                } else {
                    if (p1 <= 5) {
                        amplifier = Math.max(0, p1 - 1);
                        durationTicks = 200;
                    } else {
                        durationTicks = p1 * 20;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        entity.addEffect(new MobEffectInstance(optEffect.get(), durationTicks, amplifier, false, true));
    }
}
