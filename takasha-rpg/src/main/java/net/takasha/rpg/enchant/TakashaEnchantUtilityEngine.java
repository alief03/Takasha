package net.takasha.rpg.enchant;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.takasha.rpg.TakashaRpgMod;

import java.util.*;

/**
 * TakashaEnchantUtilityEngine
 * Engine eksekusi terpusat untuk 45+ Custom Enchantment non-tempur (Utilitas, Farming, Mining, Mobilitas, Pasif).
 */
public class TakashaEnchantUtilityEngine {

    private static boolean isExecutingSubBlock = false;

    public static void initialize() {
        // 1. Block Break Event (BEFORE): Telepathy, Smelting, Veinminer, Trench
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (isExecutingSubBlock || level.isClientSide()) return true;
            if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return true;

            ItemStack tool = sp.getMainHandItem();
            if (tool.isEmpty()) return true;

            Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(tool);
            if (enchants.isEmpty()) return true;

            boolean hasTelepathy = hasEnchant(enchants, "telepathy");
            boolean hasSmelting = hasEnchant(enchants, "smelt") || hasEnchant(enchants, "autosmelt");
            boolean hasVeinminer = hasEnchant(enchants, "veinminer");
            boolean hasTrench = hasEnchant(enchants, "trench") || hasEnchant(enchants, "blast_mining");

            // Efek Hasten (Peluang Haste buff saat menambang)
            if (hasEnchant(enchants, "hasten")) {
                int lvl = getEnchantLevel(enchants, "hasten");
                sp.addEffect(new MobEffectInstance(MobEffects.HASTE, 80 + lvl * 20, Math.min(2, lvl - 1), true, false, true));
            }

            // Efek Oxygenate (Isi oksigen jika di dalam air)
            if (hasEnchant(enchants, "oxygenate") && sp.isInWater()) {
                sp.setAirSupply(sp.getMaxAirSupply());
            }

            // Efek Experience (Peluang melipatgandakan Exp Orb)
            if (hasEnchant(enchants, "experience") && (isOre(state) || state.getBlock() instanceof CropBlock)) {
                int lvl = getEnchantLevel(enchants, "experience");
                ExperienceOrb.award(sl, sp.position(), 2 + lvl * 3);
            }

            // A. Veinminer (Menambang seluruh urat bijih yang tersambung)
            if (hasVeinminer && isOre(state)) {
                isExecutingSubBlock = true;
                try {
                    mineVein(sl, sp, pos, state, tool, hasTelepathy, hasSmelting);
                } finally {
                    isExecutingSubBlock = false;
                }
                return false; // Blok utama dan uratnya sudah diproses
            }

            // B. Trench / Blast Mining (Area tambang 3x3x3)
            if (hasTrench) {
                int lvl = Math.max(getEnchantLevel(enchants, "trench"), getEnchantLevel(enchants, "blast_mining"));
                isExecutingSubBlock = true;
                try {
                    mineTrench(sl, sp, pos, tool, lvl, hasTelepathy, hasSmelting);
                } finally {
                    isExecutingSubBlock = false;
                }
                return false;
            }

            // C. Telepathy & Smelting untuk blok tunggal
            if (hasTelepathy || hasSmelting) {
                List<ItemStack> drops = Block.getDrops(state, sl, pos, blockEntity, sp, tool);
                sl.destroyBlock(pos, false, sp);

                for (ItemStack drop : drops) {
                    ItemStack finalDrop = hasSmelting ? smeltItemStack(drop) : drop;
                    if (hasTelepathy) {
                        if (!sp.getInventory().add(finalDrop)) {
                            sp.drop(finalDrop, false);
                        }
                    } else {
                        sl.addFreshEntity(new ItemEntity(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, finalDrop));
                    }
                }

                if (hasTelepathy) {
                    sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
                }
                return false; // Block drops dikontrol sendiri
            }

            return true;
        });

        // 2. Block Break Event (AFTER): Farming Replanter & Harvest 3x3
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (isExecutingSubBlock || level.isClientSide()) return;
            if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return;

            ItemStack tool = sp.getMainHandItem();
            if (tool.isEmpty()) return;

            Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(tool);
            if (enchants.isEmpty()) return;

            boolean hasReplanter = hasEnchant(enchants, "replanter");
            boolean hasHarvest = hasEnchant(enchants, "harvest");

            // A. Replanter: Menanam kembali tanaman pada blok yang baru dipanen
            if (hasReplanter) {
                tryReplantCrop(sl, pos, state);
            }

            // B. Harvest: Memanen dan menanam kembali area 3x3 di sekitar
            if (hasHarvest) {
                int lvl = getEnchantLevel(enchants, "harvest");
                isExecutingSubBlock = true;
                try {
                    harvestArea3x3(sl, sp, pos, tool, hasReplanter);
                } finally {
                    isExecutingSubBlock = false;
                }
            }
        });

        // 3. Server Tick Event: Gears (Speed), Springs (Jump), Implants (Hunger), Blacksmith (Durability)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                if (sp.tickCount % 20 != 0) continue;

                // A. Cek Boots: Gears (Speed) & Springs (Jump Boost)
                ItemStack boots = sp.getItemBySlot(EquipmentSlot.FEET);
                if (!boots.isEmpty()) {
                    Map<String, Integer> bEnchants = TakashaEnchantManager.getCustomEnchants(boots);
                    if (hasEnchant(bEnchants, "gears")) {
                        int lvl = getEnchantLevel(bEnchants, "gears");
                        sp.addEffect(new MobEffectInstance(MobEffects.SPEED, 45, Math.min(2, lvl - 1), true, false, true));
                    }
                    if (hasEnchant(bEnchants, "springs")) {
                        int lvl = getEnchantLevel(bEnchants, "springs");
                        sp.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 45, Math.min(2, lvl - 1), true, false, true));
                    }
                    if (hasEnchant(bEnchants, "waterwalker") && sp.isInWater()) {
                        sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 45, 0, true, false, true));
                    }
                }

                // B. Cek Helmet / Armor: Implants & Alien Implants (Hunger & Saturation Regen)
                ItemStack helmet = sp.getItemBySlot(EquipmentSlot.HEAD);
                if (!helmet.isEmpty()) {
                    Map<String, Integer> hEnchants = TakashaEnchantManager.getCustomEnchants(helmet);
                    if (hasEnchant(hEnchants, "implants") || hasEnchant(hEnchants, "alienimplants")) {
                        if (sp.getFoodData().getFoodLevel() < 20) {
                            sp.getFoodData().eat(1, 0.5f);
                        }
                    }
                }

                // C. Master Blacksmith: Pulihkan 1 durability perlengkapan yang rusak secara berkala
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack item = sp.getItemBySlot(slot);
                    if (!item.isEmpty() && item.isDamaged()) {
                        Map<String, Integer> eMap = TakashaEnchantManager.getCustomEnchants(item);
                        if (hasEnchant(eMap, "masterblacksmith")) {
                            item.setDamageValue(Math.max(0, item.getDamageValue() - 1));
                            break;
                        }
                    }
                }
            }
        });

        // 4. Fall Damage Prevention: Jellylegs & Feather Step
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer sp && source.is(DamageTypes.FALL)) {
                ItemStack boots = sp.getItemBySlot(EquipmentSlot.FEET);
                if (!boots.isEmpty()) {
                    Map<String, Integer> bEnchants = TakashaEnchantManager.getCustomEnchants(boots);
                    if (hasEnchant(bEnchants, "jellylegs") || hasEnchant(bEnchants, "feather_step")) {
                        // Batalkan 100% kerusakan jatuh!
                        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 0.8f, 1.2f);
                        return false;
                    }
                }
            }
            return true;
        });
    }

    private static boolean hasEnchant(Map<String, Integer> map, String key) {
        String lower = key.toLowerCase();
        for (String k : map.keySet()) {
            if (k.toLowerCase().contains(lower)) return true;
        }
        return false;
    }

    private static int getEnchantLevel(Map<String, Integer> map, String key) {
        String lower = key.toLowerCase();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getKey().toLowerCase().contains(lower)) return e.getValue();
        }
        return 1;
    }

    private static boolean isOre(BlockState state) {
        String name = state.getBlock().getDescriptionId().toLowerCase();
        return name.contains("ore") || name.contains("ancient_debris");
    }

    /**
     * Menanam kembali benih tanaman secara instan pada umur 0.
     */
    private static void tryReplantCrop(ServerLevel level, BlockPos pos, BlockState oldState) {
        Block block = oldState.getBlock();
        if (block instanceof CropBlock crop && crop.isMaxAge(oldState)) {
            level.setBlock(pos, crop.getStateForAge(0), 3);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.7f, 1.1f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
        } else if (oldState.is(Blocks.NETHER_WART) && oldState.getValue(NetherWartBlock.AGE) == 3) {
            level.setBlock(pos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 0), 3);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.7f, 1.1f);
        } else if (oldState.is(Blocks.COCOA) && oldState.getValue(CocoaBlock.AGE) == 2) {
            level.setBlock(pos, oldState.setValue(CocoaBlock.AGE, 0), 3);
        }
    }

    /**
     * Memanen dan menanam kembali area pertanian 3x3 di sekitar blok asal.
     */
    private static void harvestArea3x3(ServerLevel level, ServerPlayer player, BlockPos origin, ItemStack tool, boolean replant) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos targetPos = origin.offset(dx, 0, dz);
                BlockState targetState = level.getBlockState(targetPos);
                Block b = targetState.getBlock();

                if (b instanceof CropBlock crop && crop.isMaxAge(targetState)) {
                    level.destroyBlock(targetPos, true, player);
                    if (replant) {
                        tryReplantCrop(level, targetPos, targetState);
                    }
                } else if (targetState.is(Blocks.NETHER_WART) && targetState.getValue(NetherWartBlock.AGE) == 3) {
                    level.destroyBlock(targetPos, true, player);
                    if (replant) {
                        tryReplantCrop(level, targetPos, targetState);
                    }
                }
            }
        }
    }

    /**
     * Menambang seluruh urat bijih yang bersambungan (Vein Miner).
     */
    private static void mineVein(ServerLevel level, ServerPlayer player, BlockPos startPos, BlockState targetState, ItemStack tool, boolean telepathy, boolean smelting) {
        Block targetBlock = targetState.getBlock();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        int maxBlocks = 24;
        int mined = 0;

        while (!queue.isEmpty() && mined < maxBlocks) {
            BlockPos current = queue.poll();
            BlockState curState = level.getBlockState(current);

            if (curState.getBlock() == targetBlock) {
                mined++;
                List<ItemStack> drops = Block.getDrops(curState, level, current, level.getBlockEntity(current), player, tool);
                level.destroyBlock(current, false, player);

                for (ItemStack drop : drops) {
                    ItemStack finalDrop = smelting ? smeltItemStack(drop) : drop;
                    if (telepathy) {
                        if (!player.getInventory().add(finalDrop)) {
                            player.drop(finalDrop, false);
                        }
                    } else {
                        level.addFreshEntity(new ItemEntity(level, current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5, finalDrop));
                    }
                }

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            BlockPos neighbor = current.offset(x, y, z);
                            if (!visited.contains(neighbor) && level.getBlockState(neighbor).getBlock() == targetBlock) {
                                visited.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Menambang area kubus 3x3x3 (Trench / Blast Mining).
     */
    private static void mineTrench(ServerLevel level, ServerPlayer player, BlockPos center, ItemStack tool, int radius, boolean telepathy, boolean smelting) {
        int r = Math.min(1, radius); // radius 1 = 3x3x3
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.getDestroySpeed(level, p) < 0) continue; // Skip bedrock

                    List<ItemStack> drops = Block.getDrops(s, level, p, level.getBlockEntity(p), player, tool);
                    level.destroyBlock(p, false, player);

                    for (ItemStack drop : drops) {
                        ItemStack finalDrop = smelting ? smeltItemStack(drop) : drop;
                        if (telepathy) {
                            if (!player.getInventory().add(finalDrop)) {
                                player.drop(finalDrop, false);
                            }
                        } else {
                            level.addFreshEntity(new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, finalDrop));
                        }
                    }
                }
            }
        }
    }

    /**
     * Melebur material tambang mentah secara instan (Smelting / Auto Smelt).
     */
    private static ItemStack smeltItemStack(ItemStack input) {
        if (input.is(Items.RAW_IRON) || input.is(Items.IRON_ORE) || input.is(Items.DEEPSLATE_IRON_ORE)) {
            return new ItemStack(Items.IRON_INGOT, input.getCount());
        }
        if (input.is(Items.RAW_GOLD) || input.is(Items.GOLD_ORE) || input.is(Items.DEEPSLATE_GOLD_ORE)) {
            return new ItemStack(Items.GOLD_INGOT, input.getCount());
        }
        if (input.is(Items.RAW_COPPER) || input.is(Items.COPPER_ORE) || input.is(Items.DEEPSLATE_COPPER_ORE)) {
            return new ItemStack(Items.COPPER_INGOT, input.getCount());
        }
        if (input.is(Items.COBBLESTONE)) {
            return new ItemStack(Items.STONE, input.getCount());
        }
        if (input.is(Items.COBBLED_DEEPSLATE)) {
            return new ItemStack(Items.DEEPSLATE, input.getCount());
        }
        if (input.is(Items.SAND) || input.is(Items.RED_SAND)) {
            return new ItemStack(Items.GLASS, input.getCount());
        }
        if (input.is(Items.ANCIENT_DEBRIS)) {
            return new ItemStack(Items.NETHERITE_SCRAP, input.getCount());
        }
        if (input.is(Items.CLAY_BALL)) {
            return new ItemStack(Items.BRICK, input.getCount());
        }
        return input;
    }
}
