package net.takasha.rpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.enchant.TakashaAnvilHandler;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.CustomEnchantDef;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * TakashaCustomBookItem
 * Buku Enchantment Khusus 9 Tier Kelangkaan autentik AdvancedEnchantments.
 * Mendukung kondisi Unrevealed (misteri, dapat dibuka dengan klik kanan)
 * dan Revealed (berisi sihir spesifik dengan tingkat sukses & hancur).
 */
public class TakashaCustomBookItem extends Item {

    public static final String NBT_ENCHANT_ID = "EnchantId";
    public static final String NBT_ENCHANT_LEVEL = "EnchantLevel";
    public static final String NBT_SUCCESS_RATE = "SuccessRate";
    public static final String NBT_DESTROY_RATE = "DestroyRate";

    private static final Random RANDOM = new Random();
    private final EnchantTier tier;

    public TakashaCustomBookItem(Properties properties, EnchantTier tier) {
        super(properties);
        this.tier = tier;
    }

    public EnchantTier getTier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        String enchId = getEnchantId(stack);
        if (enchId != null && !enchId.isEmpty()) {
            CustomEnchantDef def = TakashaEnchantRegistry.get(enchId);
            String displayName = def != null ? def.name : enchId;
            int level = getEnchantLevel(stack);
            return Component.literal(tier.getColorCode() + "§l" + displayName + " " + toRoman(level));
        }
        return Component.translatable(this.getDescriptionId());
    }

    public static String getEnchantId(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_ENCHANT_ID)) {
                return tag.getStringOr(NBT_ENCHANT_ID, "");
            }
        }
        return null;
    }

    public static int getEnchantLevel(ItemStack stack) {
        if (stack.isEmpty()) return 1;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return Math.max(1, tag.getIntOr(NBT_ENCHANT_LEVEL, 1));
        }
        return 1;
    }

    public static int getSuccessRate(ItemStack stack) {
        if (stack.isEmpty()) return 100;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_SUCCESS_RATE)) {
                return Math.max(1, Math.min(100, tag.getIntOr(NBT_SUCCESS_RATE, 100)));
            }
        }
        return 100;
    }

    public static int getDestroyRate(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_DESTROY_RATE)) {
                return Math.max(0, Math.min(100, tag.getIntOr(NBT_DESTROY_RATE, 0)));
            }
        }
        return 0;
    }

    public static ItemStack createRevealedBook(CustomEnchantDef def, int level, int successRate, int destroyRate) {
        Item bookItem = getBookItemForTier(def.tier);
        ItemStack stack = new ItemStack(bookItem);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(NBT_ENCHANT_ID, def.id);
            tag.putInt(NBT_ENCHANT_LEVEL, Math.max(1, Math.min(def.maxLevel, level)));
            tag.putInt(NBT_SUCCESS_RATE, Math.max(1, Math.min(100, successRate)));
            tag.putInt(NBT_DESTROY_RATE, Math.max(0, Math.min(100, destroyRate)));
        });
        return stack;
    }

    public static ItemStack createRandomRevealedBook(EnchantTier tier) {
        List<CustomEnchantDef> list = TakashaEnchantRegistry.getByTier(tier);
        if (list.isEmpty()) {
            CustomEnchantDef fallbackDef = new CustomEnchantDef(
                    tier.name().toLowerCase() + "_blessing",
                    tier.getDisplayName() + " Blessing",
                    "Memberikan kekuatan berkah murni tingkat " + tier.getDisplayName(),
                    tier,
                    "Senjata Tempur",
                    3
            );
            TakashaEnchantRegistry.register(fallbackDef.id, fallbackDef.name, fallbackDef.description, fallbackDef.tier, fallbackDef.appliesTo, fallbackDef.maxLevel);
            list = TakashaEnchantRegistry.getByTier(tier);
        }
        CustomEnchantDef def = list.get(RANDOM.nextInt(list.size()));
        int level = 1 + (def.maxLevel > 1 ? RANDOM.nextInt(def.maxLevel) : 0);

        int minSuccess = switch (tier) {
            case SIMPLE -> 50;
            case UNIQUE -> 45;
            case ELITE -> 40;
            case ULTIMATE -> 35;
            case LEGENDARY -> 30;
            case FABLED -> 25;
            case HEROIC -> 20;
            case SOUL -> 15;
            case MASTERY -> 10;
        };
        int maxSuccessBonus = 51;
        int maxDestroy = switch (tier) {
            case SIMPLE -> 25;
            case UNIQUE -> 30;
            case ELITE -> 35;
            case ULTIMATE -> 40;
            case LEGENDARY -> 45;
            case FABLED -> 50;
            case HEROIC -> 55;
            case SOUL -> 60;
            case MASTERY -> 65;
        };

        int success = Math.min(100, minSuccess + RANDOM.nextInt(maxSuccessBonus));
        int destroy = RANDOM.nextInt(maxDestroy + 1);
        return createRevealedBook(def, level, success, destroy);
    }

    public static Item getBookItemForTier(EnchantTier tier) {
        return switch (tier) {
            case SIMPLE -> ModRpgItems.CUSTOM_BOOK_SIMPLE;
            case UNIQUE -> ModRpgItems.CUSTOM_BOOK_UNIQUE;
            case ELITE -> ModRpgItems.CUSTOM_BOOK_ELITE;
            case ULTIMATE -> ModRpgItems.CUSTOM_BOOK_ULTIMATE;
            case LEGENDARY -> ModRpgItems.CUSTOM_BOOK_LEGENDARY;
            case FABLED -> ModRpgItems.CUSTOM_BOOK_FABLED;
            case HEROIC -> ModRpgItems.CUSTOM_BOOK_HEROIC;
            case SOUL -> ModRpgItems.CUSTOM_BOOK_SOUL;
            case MASTERY -> ModRpgItems.CUSTOM_BOOK_MASTERY;
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        String enchantId = getEnchantId(held);

        // Jika buku belum terbuka (unrevealed), klik kanan untuk membuka (reveal)
        if (enchantId == null || enchantId.isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack revealed = createRandomRevealedBook(tier);
                revealed.setCount(1);
                held.shrink(1);

                // Ekstrak metadata SEBELUM dimasukkan ke inventori, karena inventory.add(revealed)
                // mengosongkan itemstack (count=0) sehingga getter rate fallback ke 100% dan 0%!
                String name = getEnchantId(revealed);
                CustomEnchantDef def = TakashaEnchantRegistry.get(name);
                String displayName = def != null ? def.name : (name != null ? name : "Sihir Kuno");
                int lvl = getEnchantLevel(revealed);
                int success = getSuccessRate(revealed);
                int destroy = getDestroyRate(revealed);

                if (!player.getInventory().add(revealed)) {
                    player.drop(revealed, false);
                }
                player.containerMenu.broadcastChanges();

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.4, 0.4, 0.1);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.3, 0.3, 0.15);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

                player.sendSystemMessage(Component.literal("§a§l✦ BUKU TERBUKA! ✦\n"
                        + tier.getColorCode() + "§l" + displayName + " " + toRoman(lvl)
                        + " §f(Sukses: §a" + success + "%§f, Hancur: §c" + destroy + "%§f)"));
            }
            return InteractionResult.SUCCESS;
        }

        return super.use(level, player, hand);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;

        ItemStack target = slot.getItem();
        if (target.isEmpty() || (!target.isDamageableItem() && !(target.getItem() instanceof TakashaCustomWeaponItem) && !(target.getItem() instanceof TakashaArmorSetItem))) return false;

        String enchId = getEnchantId(self);
        if (enchId == null || enchId.isEmpty()) return false;

        if (!player.level().isClientSide()) {
            TakashaAnvilHandler.ApplyResult result = TakashaAnvilHandler.applyBookToGear(target, self, false);
            player.sendSystemMessage(Component.literal(result.message));
            if (result.success) {
                slot.setByPlayer(result.resultStack);
                self.shrink(1);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
            } else if (result.itemDestroyed) {
                slot.setByPlayer(ItemStack.EMPTY);
                self.shrink(1);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
            } else if (result.whiteScrollSaved) {
                slot.setByPlayer(result.resultStack);
                self.shrink(1);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
            } else {
                if (result.consumed) {
                    self.shrink(1);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            slot.setChanged();
            player.containerMenu.broadcastChanges();
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;

        String enchId = getEnchantId(self);
        if (enchId == null || enchId.isEmpty()) return false;

        // Case 1: Carried damageable gear dropped onto slotted book
        if (!other.isEmpty() && (other.isDamageableItem() || other.getItem() instanceof TakashaCustomWeaponItem)) {
            if (!player.level().isClientSide()) {
                TakashaAnvilHandler.ApplyResult result = TakashaAnvilHandler.applyBookToGear(other, self, false);
                player.sendSystemMessage(Component.literal(result.message));
                if (result.success) {
                    access.set(result.resultStack);
                    self.shrink(1);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                } else if (result.itemDestroyed) {
                    access.set(ItemStack.EMPTY);
                    self.shrink(1);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
                } else if (result.whiteScrollSaved) {
                    access.set(result.resultStack);
                    self.shrink(1);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    if (result.consumed) {
                        self.shrink(1);
                    }
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                slot.setChanged();
                player.containerMenu.broadcastChanges();
            }
            return true;
        }

        // Case 2: Carried Magic Dust dropped onto slotted book
        if (!other.isEmpty() && other.getItem() instanceof TakashaDustItem dustItem && dustItem.getDustType() == TakashaDustItem.DustType.MAGIC_DUST) {
            int currentSuccess = getSuccessRate(self);
            if (currentSuccess >= 100) {
                player.sendSystemMessage(Component.literal("§eBuku sihir ini sudah memiliki tingkat sukses 100%!"));
                return true;
            }
            if (!player.level().isClientSide()) {
                int bonus = TakashaDustItem.getDustBonus(other);
                int newSuccess = Math.min(100, currentSuccess + bonus);
                CustomData.update(DataComponents.CUSTOM_DATA, self, tag -> {
                    tag.putInt(NBT_SUCCESS_RATE, newSuccess);
                });
                other.shrink(1);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(sp, "magicDustUsed", 1);
                }
                player.sendSystemMessage(Component.literal("§a§l[Magic Dust] §fTingkat sukses buku meningkat §a+" + bonus + "% §fmenjadi §a" + newSuccess + "%§f!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                slot.setChanged();
                player.containerMenu.broadcastChanges();
            }
            return true;
        }

        return super.overrideOtherStackedOnMe(self, other, slot, action, player, access);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);

        String enchId = getEnchantId(stack);
        if (enchId == null || enchId.isEmpty()) {
            // Unrevealed Lore
            consumer.accept(Component.literal("§7Klik-Kanan untuk memeriksa isi buku sihir!"));
            consumer.accept(Component.literal("§8Tingkat: " + tier.getColorCode() + tier.getDisplayName() + " §7(" + tier.getEnchantCount() + " Variasi)"));
            consumer.accept(Component.literal("§e✦ Klik Kanan di Tangan untuk Membuka ✦"));
        } else {
            CustomEnchantDef def = TakashaEnchantRegistry.get(enchId);
            int success = getSuccessRate(stack);
            int destroy = getDestroyRate(stack);

            if (def != null && def.appliesTo != null && !def.appliesTo.isEmpty()) {
                consumer.accept(Component.literal("§7Berlaku untuk: §f" + def.appliesTo));
            }
            if (def != null && def.description != null && !def.description.isEmpty()) {
                consumer.accept(Component.literal("§f" + def.description));
            }
            consumer.accept(Component.literal(""));
            consumer.accept(Component.literal("§a" + success + "% Tingkat Keberhasilan"));
            consumer.accept(Component.literal("§c" + destroy + "% Tingkat Kerusakan Item"));
            consumer.accept(Component.literal("§8Tingkat: " + tier.getColorCode() + tier.getDisplayName()));
            consumer.accept(Component.literal("§e✦ Terapkan di Anvil atau seret ke Gear ✦"));
        }
    }

    public static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
}
