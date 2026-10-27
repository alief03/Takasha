package net.takasha.rpg.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.enchant.TakashaAnvilHandler;
import net.takasha.rpg.enchant.TakashaEnchantManager;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.Random;
import java.util.function.Consumer;

public class TakashaDustItem extends Item {

    public enum DustType {
        MAGIC_DUST("Magic Dust", "Meningkatkan persentase keberhasilan (Success Rate) buku enchant khusus"),
        MYSTERY_DUST("Mystery Dust", "Serbuk misterius yang dapat diidentifikasi menjadi Magic Dust berkualitas (Klik-Kanan)"),
        SECRET_DUST("Secret Dust", "Serbuk alkimia rahasia hasil penguraian Tinkerer. Klik-Kanan untuk mengidentifikasi!"),
        SLOT_INCREASER_1("Slot Increaser Reguler", "Menambahkan kapasitas slot enchantment khusus (Slot 16 s/d 20)"),
        SLOT_INCREASER_2("Slot Increaser Master", "Menambahkan kapasitas slot enchantment hingga Grand Cap (Slot 21 s/d 25)"),
        SLOT_INCREASER_3("Soul Slot Increaser", "Menambahkan kapasitas dedicated Soul Enchantment (Slot 3 s/d 5)"),
        ITEM_RENAME_TAG("Item Rename Tag", "Tag khusus untuk menamai ulang perlengkapan RPG tanpa penalti anvil"),
        EMPTY_SCROLL("Empty Scroll", "Kertas sihir kosong untuk menyalin atau menempa resep enchantment");

        private final String title;
        private final String description;

        DustType(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final String NBT_DUST_BONUS = "dust_bonus";
    private static final Random RANDOM = new Random();
    private final RarityTier tier;
    private final DustType dustType;

    public TakashaDustItem(Properties properties, RarityTier tier, DustType dustType) {
        super(properties);
        this.tier = tier;
        this.dustType = dustType;
    }

    public RarityTier getTier() {
        return tier;
    }

    public DustType getDustType() {
        return dustType;
    }

    public static int getDustBonus(ItemStack stack) {
        if (stack.isEmpty()) return 10;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_DUST_BONUS)) {
                return Math.max(1, Math.min(100, tag.getIntOr(NBT_DUST_BONUS, 10)));
            }
        }
        return 10;
    }

    public static ItemStack createMagicDust(int bonus) {
        ItemStack stack = new ItemStack(ModRpgItems.MAGIC_DUST);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(NBT_DUST_BONUS, Math.max(1, Math.min(100, bonus)));
        });
        return stack;
    }

    public static ItemStack createRandomMagicDust() {
        int bonus = 5 + RANDOM.nextInt(16); // +5% s/d +20%
        return createMagicDust(bonus);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (dustType == DustType.MAGIC_DUST) {
            int bonus = getDustBonus(stack);
            return Component.literal("§a§lMagic Dust §f(§a+" + bonus + "%§f)");
        }
        return super.getName(stack);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;

        ItemStack target = slot.getItem();
        if (target.isEmpty()) return false;

        if (dustType == DustType.MAGIC_DUST) {
            if (target.getItem() instanceof TakashaCustomBookItem) {
                String enchId = TakashaCustomBookItem.getEnchantId(target);
                if (enchId != null && !enchId.isEmpty()) {
                    int currentSuccess = TakashaCustomBookItem.getSuccessRate(target);
                    if (currentSuccess >= 100) {
                        player.sendSystemMessage(Component.literal("§eBuku sihir ini sudah memiliki tingkat sukses 100%!"));
                        return true;
                    }
                    if (!player.level().isClientSide()) {
                        int bonus = getDustBonus(self);
                        int newSuccess = Math.min(100, currentSuccess + bonus);
                        CustomData.update(DataComponents.CUSTOM_DATA, target, tag -> {
                            tag.putInt(TakashaCustomBookItem.NBT_SUCCESS_RATE, newSuccess);
                        });
                        self.shrink(1);
                        player.sendSystemMessage(Component.literal("§a§l[Magic Dust] §fTingkat sukses buku meningkat §a+" + bonus + "% §fmenjadi §a" + newSuccess + "%§f!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                        slot.setChanged();
                        player.containerMenu.broadcastChanges();
                    }
                    return true;
                }
            }
        } else if (dustType == DustType.SLOT_INCREASER_1 || dustType == DustType.SLOT_INCREASER_2 || dustType == DustType.SLOT_INCREASER_3) {
            if (target.isDamageableItem() || target.getItem() instanceof TakashaCustomWeaponItem || target.getItem() instanceof TakashaArmorSetItem) {
                int incTier = (dustType == DustType.SLOT_INCREASER_1) ? 1 : (dustType == DustType.SLOT_INCREASER_2) ? 2 : 3;
                if (!player.level().isClientSide()) {
                    boolean applied = TakashaEnchantManager.applySlotIncreaser(target, incTier);
                    if (applied) {
                        TakashaAnvilHandler.refreshGearLore(target);
                        self.shrink(1);
                        int newMax = (incTier == 3) ? TakashaEnchantManager.getMaxSoulSlots(target) : TakashaEnchantManager.getMaxSlots(target);
                        player.sendSystemMessage(Component.literal("§a§l[Slot Increaser] §fKapasitas slot berhasil diperluas menjadi §e" + newMax + " Slot§f!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
                    } else {
                        player.sendSystemMessage(Component.literal("§cItem ini sudah mencapai batas maksimal untuk tingkatan increaser ini!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            }
        }
        return super.overrideStackedOnOther(self, slot, action, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;
        if (other.isEmpty()) return false;

        if (dustType == DustType.MAGIC_DUST) {
            if (other.getItem() instanceof TakashaCustomBookItem) {
                String enchId = TakashaCustomBookItem.getEnchantId(other);
                if (enchId != null && !enchId.isEmpty()) {
                    int currentSuccess = TakashaCustomBookItem.getSuccessRate(other);
                    if (currentSuccess >= 100) {
                        player.sendSystemMessage(Component.literal("§eBuku sihir ini sudah memiliki tingkat sukses 100%!"));
                        return true;
                    }
                    if (!player.level().isClientSide()) {
                        int bonus = getDustBonus(self);
                        int newSuccess = Math.min(100, currentSuccess + bonus);
                        CustomData.update(DataComponents.CUSTOM_DATA, other, tag -> {
                            tag.putInt(TakashaCustomBookItem.NBT_SUCCESS_RATE, newSuccess);
                        });
                        self.shrink(1);
                        player.sendSystemMessage(Component.literal("§a§l[Magic Dust] §fTingkat sukses buku meningkat §a+" + bonus + "% §fmenjadi §a" + newSuccess + "%§f!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                        slot.setChanged();
                        player.containerMenu.broadcastChanges();
                    }
                    return true;
                }
            }
        } else if (dustType == DustType.SLOT_INCREASER_1 || dustType == DustType.SLOT_INCREASER_2 || dustType == DustType.SLOT_INCREASER_3) {
            if (other.isDamageableItem() || other.getItem() instanceof TakashaCustomWeaponItem || other.getItem() instanceof TakashaArmorSetItem) {
                int incTier = (dustType == DustType.SLOT_INCREASER_1) ? 1 : (dustType == DustType.SLOT_INCREASER_2) ? 2 : 3;
                if (!player.level().isClientSide()) {
                    boolean applied = TakashaEnchantManager.applySlotIncreaser(other, incTier);
                    if (applied) {
                        TakashaAnvilHandler.refreshGearLore(other);
                        self.shrink(1);
                        int newMax = (incTier == 3) ? TakashaEnchantManager.getMaxSoulSlots(other) : TakashaEnchantManager.getMaxSlots(other);
                        player.sendSystemMessage(Component.literal("§a§l[Slot Increaser] §fKapasitas slot berhasil diperluas menjadi §e" + newMax + " Slot§f!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
                    } else {
                        player.sendSystemMessage(Component.literal("§cItem ini sudah mencapai batas maksimal untuk tingkatan increaser ini!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            }
        }
        return super.overrideOtherStackedOnMe(self, other, slot, action, player, access);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (dustType == DustType.SECRET_DUST) {
            if (!level.isClientSide()) {
                held.shrink(1);
                // 80% chance for Magic Dust, 20% for Mystery Dust
                boolean success = RANDOM.nextInt(100) < 80;
                ItemStack reward = success ? createRandomMagicDust()
                                           : new ItemStack(ModRpgItems.MYSTERY_DUST, 1);

                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);

                if (success) {
                    player.sendSystemMessage(Component.literal("§a§l[Secret Dust] §fBerhasil diidentifikasi menjadi §bMagic Dust§f!"));
                } else {
                    player.sendSystemMessage(Component.literal("§e§l[Secret Dust] §fPenguraian tidak stabil, berubah menjadi §7Mystery Dust§f!"));
                }
            }
            return InteractionResult.SUCCESS;
        } else if (dustType == DustType.MYSTERY_DUST) {
            if (!level.isClientSide()) {
                held.shrink(1);
                boolean success = RANDOM.nextInt(100) < 65;
                if (success) {
                    ItemStack magic = createRandomMagicDust();
                    if (!player.getInventory().add(magic)) {
                        player.drop(magic, false);
                    }
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
                    player.sendSystemMessage(Component.literal("§a§l[Mystery Dust] §fSukses dimurnikan menjadi §bMagic Dust§f!"));
                } else {
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.0f);
                    player.sendSystemMessage(Component.literal("§c§l[Mystery Dust] §cGagal dimurnikan dan menguap menjadi abu!"));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);

        if (dustType == DustType.MAGIC_DUST) {
            int bonus = getDustBonus(stack);
            net.takasha.rpg.util.RpgTooltipBuilder.create("Magic Dust (+" + bonus + "%)", tier)
                    .category("ENCHANT DUST")
                    .description("Meningkatkan persentase keberhasilan (Success Rate) buku sihir khusus sebesar +" + bonus + "% (Maksimal 100%).")
                    .affordance("Terapkan", "Drag & Drop ke Buku Sihir di inventory")
                    .build(consumer);
            return;
        }

        net.takasha.rpg.util.RpgTooltipBuilder.create(dustType.getTitle(), tier)
                .category("ENCHANT DUST")
                .description(dustType.getDescription())
                .affordance("Gunakan", dustType == DustType.SECRET_DUST || dustType == DustType.MYSTERY_DUST
                        ? "Klik Kanan di tangan untuk mengidentifikasi"
                        : "Drag & Drop ke Perlengkapan di inventory")
                .build(consumer);
    }
}
