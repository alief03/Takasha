package net.takasha.rpg.enchant;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.CustomEnchantDef;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;
import net.takasha.rpg.item.TakashaCustomBookItem;

import java.util.*;

/**
 * TakashaAnvilHandler
 * Handler penempaan dan aplikasi Enchantment tingkat lanjut autentik AdvancedEnchantments.
 * Mengelola kompatibilitas gear, pengecekan batas slot (10-20), kuota Soul (2-5),
 * roll Success Rate & Destroy Rate, serta konsumsi White Scroll protection.
 */
public class TakashaAnvilHandler {

    private static final Random RANDOM = new Random();

    public static class ApplyResult {
        public final boolean success;
        public final boolean itemDestroyed;
        public final boolean whiteScrollSaved;
        public final boolean consumed;
        public final String message;
        public final ItemStack resultStack;

        public ApplyResult(boolean success, boolean itemDestroyed, boolean whiteScrollSaved, boolean consumed, String message, ItemStack resultStack) {
            this.success = success;
            this.itemDestroyed = itemDestroyed;
            this.whiteScrollSaved = whiteScrollSaved;
            this.consumed = consumed;
            this.message = message;
            this.resultStack = resultStack;
        }

        public ApplyResult(boolean success, boolean itemDestroyed, boolean whiteScrollSaved, String message, ItemStack resultStack) {
            this(success, itemDestroyed, whiteScrollSaved, true, message, resultStack);
        }
    }

    /**
     * Memeriksa apakah gear kompatibel dengan target enchant (appliesTo).
     */
    /**
     * Memeriksa apakah gear kompatibel dengan target enchant (appliesTo).
     * Mendukung bahasa Indonesia dan bahasa Inggris, serta kategori umum.
     */
    public static boolean isCompatible(ItemStack gear, CustomEnchantDef def) {
        if (gear.isEmpty() || def == null) return false;
        String applies = def.appliesTo != null ? def.appliesTo.toUpperCase() : "ANY";

        // Dukungan penuh untuk Senjata Kustom Takasha RPG
        if (gear.getItem() instanceof net.takasha.rpg.item.TakashaCustomWeaponItem customWeapon) {
            net.takasha.rpg.item.TakashaCustomWeaponItem.WeaponType type = customWeapon.getWeaponType();
            if (applies.equals("ANY") || applies.contains("ALL") || applies.contains("SEMUA") || applies.contains("GLOBAL")) return true;
            if (applies.contains("WEAPON") || applies.contains("SENJATA")) return true;
            if ((applies.contains("SWORD") || applies.contains("PEDANG")) && type != net.takasha.rpg.item.TakashaCustomWeaponItem.WeaponType.KOTH_AXE) return true;
            if ((applies.contains("AXE") || applies.contains("KAPAK")) && type == net.takasha.rpg.item.TakashaCustomWeaponItem.WeaponType.KOTH_AXE) return true;
            if (applies.contains("SCYTHE") && type == net.takasha.rpg.item.TakashaCustomWeaponItem.WeaponType.PHANTOM_SCYTHE) return true;
            if (applies.contains("DAGGER") && type == net.takasha.rpg.item.TakashaCustomWeaponItem.WeaponType.SUPREME_WEAPON) return true;
            return false;
        }

        // Dukungan penuh untuk 28 Armor Kustom Takasha RPG
        if (gear.getItem() instanceof net.takasha.rpg.item.TakashaArmorSetItem armorItem) {
            if (applies.equals("ANY") || applies.contains("ALL") || applies.contains("SEMUA") || applies.contains("GLOBAL") || applies.contains("ARMOR") || applies.contains("ZIRAH")) return true;
            String regName = BuiltInRegistries.ITEM.getKey(armorItem).getPath().toUpperCase();
            if ((applies.contains("HELMET") || applies.contains("HELM")) && regName.contains("HELMET")) return true;
            if ((applies.contains("CHESTPLATE") || applies.contains("CHEST") || applies.contains("DADA")) && regName.contains("CHESTPLATE")) return true;
            if ((applies.contains("LEGGINGS") || applies.contains("LEGS") || applies.contains("CELANA")) && regName.contains("LEGGINGS")) return true;
            if ((applies.contains("BOOTS") || applies.contains("SEPATU")) && regName.contains("BOOTS")) return true;
            return false;
        }

        if (applies.equals("ANY") || applies.contains("ALL") || applies.contains("SEMUA") || applies.contains("GLOBAL")) {
            return gear.isDamageableItem();
        }

        String itemName = gear.getItem().toString().toUpperCase();

        boolean isSword = itemName.contains("SWORD") || itemName.contains("KATANA");
        boolean isAxe = itemName.contains("AXE") && !itemName.contains("PICKAXE");
        boolean isBow = itemName.contains("BOW") && !itemName.contains("CROSSBOW");
        boolean isCrossbow = itemName.contains("CROSSBOW");
        boolean isWeapon = isSword || isAxe || isBow || isCrossbow || itemName.contains("SCYTHE") || itemName.contains("DAGGER") || itemName.contains("MACE") || itemName.contains("TRIDENT");

        boolean isHelmet = itemName.contains("HELMET") || itemName.contains("CAP");
        boolean isChest = itemName.contains("CHESTPLATE") || itemName.contains("TUNIC");
        boolean isLeggings = itemName.contains("LEGGINGS") || itemName.contains("PANTS");
        boolean isBoots = itemName.contains("BOOTS");
        boolean isArmor = isHelmet || isChest || isLeggings || isBoots || itemName.contains("ELYTRA");

        boolean isPickaxe = itemName.contains("PICKAXE");
        boolean isShovel = itemName.contains("SHOVEL");
        boolean isHoe = itemName.contains("HOE");
        boolean isTool = isPickaxe || isShovel || isAxe || isHoe || itemName.contains("FISHING_ROD") || itemName.contains("SHEARS");
        boolean isFishing = itemName.contains("FISHING_ROD");
        boolean isElytra = itemName.contains("ELYTRA");
        boolean isShield = itemName.contains("SHIELD");

        if ((applies.contains("SWORD") || applies.contains("PEDANG")) && isSword) return true;
        if ((applies.contains("AXE") || applies.contains("KAPAK")) && isAxe) return true;
        if ((applies.contains("WEAPON") || applies.contains("SENJATA")) && isWeapon) return true;
        if ((applies.contains("BOW") || applies.contains("BUSUR")) && (isBow || (applies.contains("CROSSBOW") || applies.contains("SILANG") && isCrossbow))) return true;
        if ((applies.contains("CROSSBOW") || applies.contains("SILANG")) && isCrossbow) return true;
        if ((applies.contains("HELMET") || applies.contains("HELM")) && isHelmet) return true;
        if ((applies.contains("CHESTPLATE") || applies.contains("ZIRAH DADA") || applies.contains("DADA")) && isChest) return true;
        if ((applies.contains("LEGGINGS") || applies.contains("CELANA")) && isLeggings) return true;
        if ((applies.contains("BOOTS") || applies.contains("SEPATU")) && isBoots) return true;
        if ((applies.contains("ARMOR") || applies.contains("ZIRAH")) && isArmor) return true;
        if ((applies.contains("PICKAXE") || applies.contains("BELIUNG")) && isPickaxe) return true;
        if ((applies.contains("SHOVEL") || applies.contains("SEKOP")) && isShovel) return true;
        if ((applies.contains("HOE") || applies.contains("CANGKUL")) && isHoe) return true;
        if ((applies.contains("TOOL") || applies.contains("ALAT") || applies.contains("PERKAKAS")) && isTool) return true;
        if ((applies.contains("FISHING") || applies.contains("PANCING")) && isFishing) return true;
        if ((applies.contains("ELYTRA") || applies.contains("SAYAP")) && isElytra) return true;
        if ((applies.contains("SHIELD") || applies.contains("PERISAI")) && isShield) return true;

        // Default izinkan jika item adalah damageable gear
        return gear.isDamageableItem();
    }

    /**
     * Menerapkan buku custom enchant ke gear target dengan simulasi roll success/destroy.
     */
    public static ApplyResult applyBookToGear(ItemStack targetGear, ItemStack bookStack, boolean isForceAdmin) {
        if (targetGear.isEmpty() || bookStack.isEmpty()) {
            return new ApplyResult(false, false, false, false, "§cItem target atau buku tidak valid!", targetGear);
        }

        String enchantId = TakashaCustomBookItem.getEnchantId(bookStack);
        if (enchantId == null || enchantId.isEmpty()) {
            return new ApplyResult(false, false, false, false, "§cBuku ini belum dibuka! Buka terlebih dahulu dengan Klik-Kanan.", targetGear);
        }

        CustomEnchantDef def = TakashaEnchantRegistry.get(enchantId);
        if (def == null) {
            return new ApplyResult(false, false, false, false, "§cEnchantment tidak terdaftar di sistem!", targetGear);
        }

        if (!isForceAdmin && !isCompatible(targetGear, def)) {
            return new ApplyResult(false, false, false, false, "§cSihir ini tidak kompatibel dengan jenis peralatan ini! (Butuh: " + def.appliesTo + ")", targetGear);
        }

        if (!TakashaEnchantManager.canApplyEnchant(targetGear, def, isForceAdmin)) {
            if (def.tier == EnchantTier.SOUL && TakashaEnchantManager.getSoulEnchantCount(targetGear) >= TakashaEnchantManager.getMaxSoulSlots(targetGear)) {
                return new ApplyResult(false, false, false, false, "§cSlot Soul Enchantment penuh! Maksimal " + TakashaEnchantManager.getMaxSoulSlots(targetGear) + " slot.", targetGear);
            }
            return new ApplyResult(false, false, false, false, "§cSlot Custom Enchantment penuh! Maksimal " + TakashaEnchantManager.getMaxSlots(targetGear) + " slot. Gunakan Slot Increaser untuk menambah.", targetGear);
        }

        int bookLevel = TakashaCustomBookItem.getEnchantLevel(bookStack);
        int currentLevel = TakashaEnchantManager.getEnchantLevel(targetGear, enchantId);
        int targetLevel = bookLevel;

        if (currentLevel > 0) {
            if (bookLevel == currentLevel) {
                targetLevel = Math.min(def.maxLevel, currentLevel + 1);
            } else {
                targetLevel = Math.max(currentLevel, bookLevel);
            }
        }

        // Jika force admin, langsung berhasil 100%
        if (isForceAdmin) {
            ItemStack result = targetGear.copy();
            TakashaEnchantManager.applyCustomEnchant(result, def, targetLevel, true);
            refreshGearLore(result);
            return new ApplyResult(true, false, false, "§aBerhasil menerapkan sihir " + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(targetLevel) + " §a(Admin Force)!", result);
        }

        // Roll Success & Destroy Rate
        int successRate = TakashaCustomBookItem.getSuccessRate(bookStack);
        int destroyRate = TakashaCustomBookItem.getDestroyRate(bookStack);
        int rollSuccess = RANDOM.nextInt(100) + 1; // 1 s/d 100

        if (rollSuccess <= successRate) {
            // Berhasil!
            ItemStack result = targetGear.copy();
            TakashaEnchantManager.applyCustomEnchant(result, def, targetLevel, false);
            refreshGearLore(result);
            return new ApplyResult(true, false, false, "§a§lSUKSES! §aSihir " + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(targetLevel) + " §aberhasil ditempa!", result);
        } else {
            // Gagal! Cek apakah item hancur
            int rollDestroy = RANDOM.nextInt(100) + 1;
            if (rollDestroy <= destroyRate) {
                // Item berisiko hancur, periksa White Scroll
                if (TakashaEnchantManager.isWhiteScrollProtected(targetGear)) {
                    ItemStack saved = targetGear.copy();
                    TakashaEnchantManager.setWhiteScrollProtected(saved, false);
                    refreshGearLore(saved);
                    return new ApplyResult(false, false, true, "§e§lGAGAL! §fPenempaan gagal, namun §f§lWhite Scroll §emenyelamatkan peralatanmu dari kehancuran!", saved);
                } else {
                    // Item hancur!
                    return new ApplyResult(false, true, false, "§c§lHANCUR! §cPenempaan sihir gagal dan peralatanmu musnah berkeping-keping!", ItemStack.EMPTY);
                }
            } else {
                // Gagal biasa tanpa kehancuran
                return new ApplyResult(false, false, false, "§c§lGAGAL! §cPenempaan sihir gagal, buku lenyap namun peralatanmu tetap utuh.", targetGear.copy());
            }
        }
    }

    public static void formatGearLore(ItemStack stack) {
        refreshGearLore(stack);
    }

    /**
     * Memperbarui visual Lore pada gear agar menampilkan Custom Enchantments,
     * status slot, dan status White/Holy Scroll.
     */
    public static void refreshGearLore(ItemStack stack) {
        if (stack.isEmpty()) return;

        Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(stack);
        int maxSlots = TakashaEnchantManager.getMaxSlots(stack);
        int maxSoulSlots = TakashaEnchantManager.getMaxSoulSlots(stack);
        boolean whiteScroll = TakashaEnchantManager.isWhiteScrollProtected(stack);
        boolean holyScroll = TakashaEnchantManager.isHolyScrollProtected(stack);

        List<Component> newLore = new ArrayList<>();

        // 1. Tampilkan Holy Scroll / White Scroll banner di paling atas jika aktif
        if (holyScroll) {
            newLore.add(Component.literal("§e§l*§f§lHOLY§e§l* §f§lPROTECTED"));
        }
        if (whiteScroll) {
            newLore.add(Component.literal("§f§lPROTECTED"));
        }

        // 2. Tampilkan Custom Enchantments
        if (!enchants.isEmpty()) {
            newLore.add(Component.literal("§8§m------------------------"));
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                CustomEnchantDef def = TakashaEnchantRegistry.get(entry.getKey());
                if (def != null) {
                    newLore.add(Component.literal(def.tier.getColorCode() + def.name + " " + TakashaCustomBookItem.toRoman(entry.getValue())));
                } else {
                    newLore.add(Component.literal("§7" + entry.getKey() + " " + entry.getValue()));
                }
            }
            newLore.add(Component.literal("§8§m------------------------"));
        }

        // 3. Tampilkan Slot Status Info
        int usedSlots = enchants.size();
        newLore.add(Component.literal("§7Custom Enchant Slots: §e" + usedSlots + "§7/§6" + maxSlots));
        int soulUsed = TakashaEnchantManager.getSoulEnchantCount(stack);
        if (soulUsed > 0 || maxSoulSlots > TakashaEnchantManager.DEFAULT_SOUL_SLOTS) {
            newLore.add(Component.literal("§3Soul Slots: §b" + soulUsed + "§7/§3" + maxSoulSlots));
        }

        // 4. Tampilkan Stat Trackers jika terpasang
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            net.minecraft.nbt.CompoundTag rootTag = cd.copyTag();
            if (rootTag.contains(TakashaEnchantManager.NBT_TRACKERS)) {
                net.minecraft.nbt.CompoundTag trTag = rootTag.getCompoundOrEmpty(TakashaEnchantManager.NBT_TRACKERS);
                for (String k : trTag.keySet()) {
                    try {
                        net.takasha.rpg.item.TakashaStatTrackerItem.TrackerType tt = net.takasha.rpg.item.TakashaStatTrackerItem.TrackerType.valueOf(k);
                        int val = trTag.getIntOr(k, 0);
                        String label = switch (tt) {
                            case KILLS -> "§c§lPlayer Kills: §f" + val;
                            case MOBS_KILLED -> "§6§lMob Kills: §f" + val;
                            case BLOCKS_MINED -> "§b§lBlocks Mined: §f" + val;
                            case DAMAGE_DEALT -> "§4§lDamage Dealt: §f" + val;
                            case FISH_CAUGHT -> "§9§lFish Caught: §f" + val;
                        };
                        newLore.add(Component.literal(label));
                    } catch (Exception ignored) {}
                }
            }
        }

        stack.set(DataComponents.LORE, new ItemLore(newLore));
        if (!enchants.isEmpty() || holyScroll || whiteScroll) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
    }
}
