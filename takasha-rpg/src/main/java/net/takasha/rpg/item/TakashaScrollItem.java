package net.takasha.rpg.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.enchant.TakashaAnvilHandler;
import net.takasha.rpg.enchant.TakashaEnchantManager;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.CustomEnchantDef;

import java.util.*;
import java.util.function.Consumer;

public class TakashaScrollItem extends Item {

    public enum ScrollType {
        WHITE("White Scroll", "Melindungi perlengkapan agar tidak hancur saat gagal ditempa enchant"),
        BLACK("Black Scroll", "Mencabut enchantment khusus dari senjata/armor dan mengembalikannya ke buku"),
        TRANSMUTATION("Transmutation Scroll", "Mengacak kembali salah satu enchantment khusus pada item"),
        HOLY("Holy Scroll", "Melindungi seluruh isi inventaris dan perlengkapan agar tidak hilang saat mati (1x pakai)"),
        RANDOMIZATION("Randomization Scroll", "Mengacak ulang persentase tingkat keberhasilan (Success Rate) pada buku enchantment");

        private final String title;
        private final String functionDesc;

        ScrollType(String title, String functionDesc) {
            this.title = title;
            this.functionDesc = functionDesc;
        }

        public String getTitle() {
            return title;
        }

        public String getFunctionDesc() {
            return functionDesc;
        }
    }

    private static final Random RANDOM = new Random();
    private final RarityTier tier;
    private final ScrollType scrollType;

    public TakashaScrollItem(Properties properties, RarityTier tier, ScrollType scrollType) {
        super(properties);
        this.tier = tier;
        this.scrollType = scrollType;
    }

    public RarityTier getTier() {
        return tier;
    }

    public ScrollType getScrollType() {
        return scrollType;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;

        ItemStack target = slot.getItem();
        if (target.isEmpty()) return false;

        if (scrollType == ScrollType.WHITE) {
            if (target.isDamageableItem()) {
                if (TakashaEnchantManager.isWhiteScrollProtected(target)) {
                    player.sendSystemMessage(Component.literal("§cItem ini sudah dilindungi oleh White Scroll!"));
                    return true;
                }
                if (!player.level().isClientSide()) {
                    TakashaEnchantManager.setWhiteScrollProtected(target, true);
                    TakashaAnvilHandler.refreshGearLore(target);
                    self.shrink(1);
                    player.sendSystemMessage(Component.literal("§f§l[White Scroll] §aPeralatan ini sekarang §f§lDILINDUNGI §adari risiko hancur!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            }
        } else if (scrollType == ScrollType.HOLY) {
            if (target.isDamageableItem()) {
                if (TakashaEnchantManager.isHolyScrollProtected(target)) {
                    player.sendSystemMessage(Component.literal("§cItem ini sudah dilindungi oleh Holy Scroll!"));
                    return true;
                }
                if (!player.level().isClientSide()) {
                    TakashaEnchantManager.setHolyScrollProtected(target, true);
                    TakashaAnvilHandler.refreshGearLore(target);
                    self.shrink(1);
                    player.sendSystemMessage(Component.literal("§e§l[Holy Scroll] §aPeralatan ini diberkati dengan proteksi §e§l*HOLY* §a(Keep on Death)!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            }
        } else if (scrollType == ScrollType.BLACK) {
            Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(target);
            if (!enchants.isEmpty()) {
                if (!player.level().isClientSide()) {
                    List<String> keys = new ArrayList<>(enchants.keySet());
                    String chosenId = keys.get(RANDOM.nextInt(keys.size()));
                    int level = enchants.get(chosenId);

                    TakashaEnchantManager.removeCustomEnchant(target, chosenId);
                    TakashaAnvilHandler.refreshGearLore(target);

                    CustomEnchantDef def = TakashaEnchantRegistry.get(chosenId);
                    if (def != null) {
                        ItemStack extractedBook = TakashaCustomBookItem.createRevealedBook(def, level, 100, 0);
                        if (!player.getInventory().add(extractedBook)) {
                            player.drop(extractedBook, false);
                        }
                        player.sendSystemMessage(Component.literal("§8§l[Black Scroll] §aBerhasil mencabut sihir "
                                + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(level)
                                + " §amenjadi buku sihir 100%!"));
                    }
                    self.shrink(1);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            } else {
                player.sendSystemMessage(Component.literal("§cItem ini tidak memiliki Custom Enchantment untuk dicabut!"));
                return true;
            }
        } else if (scrollType == ScrollType.RANDOMIZATION) {
            if (target.getItem() instanceof TakashaCustomBookItem) {
                String enchId = TakashaCustomBookItem.getEnchantId(target);
                if (enchId != null && !enchId.isEmpty()) {
                    if (!player.level().isClientSide()) {
                        int newSuccess = 40 + RANDOM.nextInt(61);
                        int newDestroy = RANDOM.nextInt(35);

                        CustomData.update(DataComponents.CUSTOM_DATA, target, tag -> {
                            tag.putInt(TakashaCustomBookItem.NBT_SUCCESS_RATE, newSuccess);
                            tag.putInt(TakashaCustomBookItem.NBT_DESTROY_RATE, newDestroy);
                        });

                        self.shrink(1);
                        player.sendSystemMessage(Component.literal("§d§l[Randomization Scroll] §aPersentase buku diacak ulang! §a"
                                + newSuccess + "% Sukses §7| §c" + newDestroy + "% Hancur"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.4f);
                        slot.setChanged();
                        player.containerMenu.broadcastChanges();
                    }
                    return true;
                }
            }
        } else if (scrollType == ScrollType.TRANSMUTATION) {
            Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(target);
            if (!enchants.isEmpty()) {
                if (!player.level().isClientSide()) {
                    List<String> keys = new ArrayList<>(enchants.keySet());
                    String chosenId = keys.get(RANDOM.nextInt(keys.size()));
                    int level = enchants.get(chosenId);
                    CustomEnchantDef currentDef = TakashaEnchantRegistry.get(chosenId);

                    if (currentDef != null) {
                        List<CustomEnchantDef> sameTier = TakashaEnchantRegistry.getByTier(currentDef.tier);
                        List<CustomEnchantDef> candidates = new ArrayList<>();
                        for (CustomEnchantDef d : sameTier) {
                            if (!d.id.equalsIgnoreCase(chosenId) && TakashaAnvilHandler.isCompatible(target, d)) {
                                candidates.add(d);
                            }
                        }

                        if (!candidates.isEmpty()) {
                            CustomEnchantDef replacement = candidates.get(RANDOM.nextInt(candidates.size()));
                            TakashaEnchantManager.removeCustomEnchant(target, chosenId);
                            TakashaEnchantManager.applyCustomEnchant(target, replacement, Math.min(replacement.maxLevel, level), false);
                            TakashaAnvilHandler.refreshGearLore(target);

                            player.sendSystemMessage(Component.literal("§b§l[Transmutation Scroll] §aMengubah sihir §7"
                                    + currentDef.name + " §amenjadi " + replacement.tier.getColorCode() + "§l" + replacement.name + "§a!"));
                            self.shrink(1);
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.2f);
                            slot.setChanged();
                            player.containerMenu.broadcastChanges();
                        } else {
                            player.sendSystemMessage(Component.literal("§cTidak ditemukan variasi sihir alternatif lain untuk tier ini!"));
                        }
                    }
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

        if (scrollType == ScrollType.WHITE) {
            if (other.isDamageableItem()) {
                if (TakashaEnchantManager.isWhiteScrollProtected(other)) {
                    player.sendSystemMessage(Component.literal("§cItem ini sudah dilindungi oleh White Scroll!"));
                    return true;
                }
                if (!player.level().isClientSide()) {
                    TakashaEnchantManager.setWhiteScrollProtected(other, true);
                    TakashaAnvilHandler.refreshGearLore(other);
                    self.shrink(1);
                    player.sendSystemMessage(Component.literal("§f§l[White Scroll] §aPeralatan ini sekarang §f§lDILINDUNGI §adari risiko hancur!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            }
        } else if (scrollType == ScrollType.HOLY) {
            if (other.isDamageableItem()) {
                if (TakashaEnchantManager.isHolyScrollProtected(other)) {
                    player.sendSystemMessage(Component.literal("§cItem ini sudah dilindungi oleh Holy Scroll!"));
                    return true;
                }
                if (!player.level().isClientSide()) {
                    TakashaEnchantManager.setHolyScrollProtected(other, true);
                    TakashaAnvilHandler.refreshGearLore(other);
                    self.shrink(1);
                    player.sendSystemMessage(Component.literal("§e§l[Holy Scroll] §aPeralatan ini diberkati dengan proteksi §e§l*HOLY* §a(Keep on Death)!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            }
        } else if (scrollType == ScrollType.BLACK) {
            Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(other);
            if (!enchants.isEmpty()) {
                if (!player.level().isClientSide()) {
                    List<String> keys = new ArrayList<>(enchants.keySet());
                    String chosenId = keys.get(RANDOM.nextInt(keys.size()));
                    int level = enchants.get(chosenId);

                    TakashaEnchantManager.removeCustomEnchant(other, chosenId);
                    TakashaAnvilHandler.refreshGearLore(other);

                    CustomEnchantDef def = TakashaEnchantRegistry.get(chosenId);
                    if (def != null) {
                        ItemStack extractedBook = TakashaCustomBookItem.createRevealedBook(def, level, 100, 0);
                        if (!player.getInventory().add(extractedBook)) {
                            player.drop(extractedBook, false);
                        }
                        player.sendSystemMessage(Component.literal("§8§l[Black Scroll] §aBerhasil mencabut sihir "
                                + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(level)
                                + " §amenjadi buku sihir 100%!"));
                    }
                    self.shrink(1);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    slot.setChanged();
                    player.containerMenu.broadcastChanges();
                }
                return true;
            } else {
                player.sendSystemMessage(Component.literal("§cItem ini tidak memiliki Custom Enchantment untuk dicabut!"));
                return true;
            }
        } else if (scrollType == ScrollType.RANDOMIZATION) {
            if (other.getItem() instanceof TakashaCustomBookItem) {
                String enchId = TakashaCustomBookItem.getEnchantId(other);
                if (enchId != null && !enchId.isEmpty()) {
                    if (!player.level().isClientSide()) {
                        int newSuccess = 40 + RANDOM.nextInt(61);
                        int newDestroy = RANDOM.nextInt(35);

                        CustomData.update(DataComponents.CUSTOM_DATA, other, tag -> {
                            tag.putInt(TakashaCustomBookItem.NBT_SUCCESS_RATE, newSuccess);
                            tag.putInt(TakashaCustomBookItem.NBT_DESTROY_RATE, newDestroy);
                        });

                        self.shrink(1);
                        player.sendSystemMessage(Component.literal("§d§l[Randomization Scroll] §aPersentase buku diacak ulang! §a"
                                + newSuccess + "% Sukses §7| §c" + newDestroy + "% Hancur"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.4f);
                        slot.setChanged();
                        player.containerMenu.broadcastChanges();
                    }
                    return true;
                }
            }
        } else if (scrollType == ScrollType.TRANSMUTATION) {
            Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(other);
            if (!enchants.isEmpty()) {
                if (!player.level().isClientSide()) {
                    List<String> keys = new ArrayList<>(enchants.keySet());
                    String chosenId = keys.get(RANDOM.nextInt(keys.size()));
                    int level = enchants.get(chosenId);
                    CustomEnchantDef currentDef = TakashaEnchantRegistry.get(chosenId);

                    if (currentDef != null) {
                        List<CustomEnchantDef> sameTier = TakashaEnchantRegistry.getByTier(currentDef.tier);
                        List<CustomEnchantDef> candidates = new ArrayList<>();
                        for (CustomEnchantDef d : sameTier) {
                            if (!d.id.equalsIgnoreCase(chosenId) && TakashaAnvilHandler.isCompatible(other, d)) {
                                candidates.add(d);
                            }
                        }

                        if (!candidates.isEmpty()) {
                            CustomEnchantDef replacement = candidates.get(RANDOM.nextInt(candidates.size()));
                            TakashaEnchantManager.removeCustomEnchant(other, chosenId);
                            TakashaEnchantManager.applyCustomEnchant(other, replacement, Math.min(replacement.maxLevel, level), false);
                            TakashaAnvilHandler.refreshGearLore(other);

                            player.sendSystemMessage(Component.literal("§b§l[Transmutation Scroll] §aMengubah sihir §7"
                                    + currentDef.name + " §amenjadi " + replacement.tier.getColorCode() + "§l" + replacement.name + "§a!"));
                            self.shrink(1);
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.2f);
                            slot.setChanged();
                            player.containerMenu.broadcastChanges();
                        } else {
                            player.sendSystemMessage(Component.literal("§cTidak ditemukan variasi sihir alternatif lain untuk tier ini!"));
                        }
                    }
                }
                return true;
            }
        }
        return super.overrideOtherStackedOnMe(self, other, slot, action, player, access);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);

        net.takasha.rpg.util.RpgTooltipBuilder.create(scrollType.getTitle(), tier)
                .category("ENCHANT SCROLL")
                .description(scrollType.getFunctionDesc())
                .affordance("Gunakan", "Drag & Drop ke Perlengkapan / Buku Sihir di inventory")
                .build(consumer);
    }
}
