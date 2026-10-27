package net.takasha.rpg.trade;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;
import net.takasha.rpg.item.TakashaCustomBookItem;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.Optional;

/**
 * TakashaVillagerTrades
 * Handles custom RPG & AdvancedEnchantments trades for Villagers and Wandering Traders.
 * Matches lootConfiguration.yml specifications.
 */
public class TakashaVillagerTrades {

    public static void initialize() {
        // Initialization hook called on mod init
    }

    public static void injectVillagerTrades(Villager villager, ServerLevel level) {
        VillagerData data = villager.getVillagerData();
        int villagerLevel = data.level();
        MerchantOffers offers = villager.getOffers();

        if (data.profession().is(VillagerProfession.LIBRARIAN)) {
            injectLibrarianTrades(offers, villagerLevel);
        } else if (data.profession().is(VillagerProfession.CLERIC)) {
            injectClericTrades(offers, villagerLevel);
        } else if (data.profession().is(VillagerProfession.ARMORER)) {
            injectArmorerTrades(offers, villagerLevel);
        } else if (data.profession().is(VillagerProfession.WEAPONSMITH)) {
            injectWeaponsmithTrades(offers, villagerLevel);
        } else if (data.profession().is(VillagerProfession.TOOLSMITH)) {
            injectToolsmithTrades(offers, villagerLevel);
        }
    }

    public static void injectWanderingTraderTrades(WanderingTrader trader, ServerLevel level) {
        MerchantOffers offers = trader.getOffers();
        if (hasOffer(offers, ModRpgItems.HOLY_SCROLL)) {
            return;
        }

        addTrade(offers, new ItemCost(Items.EMERALD, 25), new ItemStack(ModRpgItems.TRANSMUTE_SCROLL), 2, 5);
        addTrade(offers, new ItemCost(Items.EMERALD, 30), new ItemStack(ModRpgItems.RANDOMIZATION_SCROLL), 2, 5);
        addTrade(offers, new ItemCost(Items.EMERALD, 35), new ItemStack(ModRpgItems.WHITE_SCROLL), 2, 5);
        addTrade(offers, new ItemCost(Items.EMERALD, 50), new ItemStack(ModRpgItems.HOLY_SCROLL), 1, 5);
        addTrade(offers, new ItemCost(Items.EMERALD, 40), new ItemStack(ModRpgItems.SOUL_GEM), 2, 5);
    }

    private static void injectLibrarianTrades(MerchantOffers offers, int level) {
        if (level >= 1 && !hasOffer(offers, ModRpgItems.SECRET_DUST)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 4), new ItemStack(ModRpgItems.SECRET_DUST), 8, 2);
            addTrade(offers, new ItemCost(Items.EMERALD, 8), new ItemStack(ModRpgItems.MAGIC_DUST), 8, 2);
        }
        if (level >= 2 && !hasOffer(offers, ModRpgItems.CUSTOM_BOOK_SIMPLE)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 12), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.SIMPLE), 4, 5);
            addTrade(offers, new ItemCost(Items.EMERALD, 16), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.UNIQUE), 4, 5);
        }
        if (level >= 3 && !hasOffer(offers, ModRpgItems.WHITE_SCROLL)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 24), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.ELITE), 4, 10);
            addTrade(offers, new ItemCost(Items.EMERALD, 32), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.ULTIMATE), 4, 10);
            addTrade(offers, new ItemCost(Items.EMERALD, 20), new ItemStack(ModRpgItems.WHITE_SCROLL), 3, 10);
        }
        if (level >= 4 && !hasOffer(offers, ModRpgItems.BLACK_SCROLL)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 40), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.LEGENDARY), 3, 15);
            addTrade(offers, new ItemCost(Items.EMERALD, 48), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.FABLED), 3, 15);
            addTrade(offers, new ItemCost(Items.EMERALD, 28), new ItemStack(ModRpgItems.TRANSMUTE_SCROLL), 3, 15);
            addTrade(offers, new ItemCost(Items.EMERALD, 32), new ItemStack(ModRpgItems.BLACK_SCROLL), 3, 15);
        }
        if (level >= 5 && !hasOffer(offers, ModRpgItems.HOLY_SCROLL)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 56), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.HEROIC), 2, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 64), new ItemCost(Items.BOOK, 1),
                    TakashaCustomBookItem.createRandomRevealedBook(EnchantTier.MASTERY), 2, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 40), new ItemStack(ModRpgItems.RANDOMIZATION_SCROLL), 2, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 48), new ItemStack(ModRpgItems.HOLY_SCROLL), 2, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 36), new ItemStack(ModRpgItems.SLOT_INCREASER_1), 2, 20);
        }
    }

    private static void injectClericTrades(MerchantOffers offers, int level) {
        if (level >= 1 && !hasOffer(offers, ModRpgItems.MAGIC_DUST)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 6), new ItemStack(ModRpgItems.MAGIC_DUST), 8, 2);
        }
        if (level >= 2 && !hasOffer(offers, ModRpgItems.SECRET_DUST)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 12), new ItemStack(ModRpgItems.SECRET_DUST), 6, 5);
        }
        if (level >= 3 && !hasOffer(offers, ModRpgItems.SOUL_GEM)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 28), new ItemStack(ModRpgItems.SOUL_GEM), 4, 10);
        }
        if (level >= 4 && !hasOffer(offers, ModRpgItems.HOLY_SCROLL)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 45), new ItemStack(ModRpgItems.HOLY_SCROLL), 2, 15);
        }
        if (level >= 5 && !hasOffer(offers, ModRpgItems.SLOT_INCREASER_2)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 64), new ItemStack(ModRpgItems.SLOT_INCREASER_2), 2, 20);
        }
    }

    private static void injectArmorerTrades(MerchantOffers offers, int level) {
        if (level >= 5 && !hasOffer(offers, ModRpgItems.ARMOR_ENCHANTMENT_ORB)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 32), new ItemStack(ModRpgItems.SLOT_INCREASER_1), 3, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 48), new ItemStack(ModRpgItems.ARMOR_ENCHANTMENT_ORB), 2, 20);
        }
    }

    private static void injectWeaponsmithTrades(MerchantOffers offers, int level) {
        if (level >= 5 && !hasOffer(offers, ModRpgItems.WEAPON_ENCHANTMENT_ORB)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 28), new ItemStack(ModRpgItems.STAT_TRACKER_MOBS), 3, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 32), new ItemStack(ModRpgItems.STAT_TRACKER_KILLS), 3, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 48), new ItemStack(ModRpgItems.WEAPON_ENCHANTMENT_ORB), 2, 20);
        }
    }

    private static void injectToolsmithTrades(MerchantOffers offers, int level) {
        if (level >= 5 && !hasOffer(offers, ModRpgItems.TOOL_ENCHANTMENT_ORB)) {
            addTrade(offers, new ItemCost(Items.EMERALD, 24), new ItemStack(ModRpgItems.STAT_TRACKER_BLOCKS), 3, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 24), new ItemStack(ModRpgItems.STAT_TRACKER_FISH), 3, 20);
            addTrade(offers, new ItemCost(Items.EMERALD, 48), new ItemStack(ModRpgItems.TOOL_ENCHANTMENT_ORB), 2, 20);
        }
    }

    private static void addTrade(MerchantOffers offers, ItemCost costA, ItemStack result, int maxUses, int xp) {
        offers.add(new MerchantOffer(costA, Optional.empty(), result, maxUses, xp, 0.05f));
    }

    private static void addTrade(MerchantOffers offers, ItemCost costA, ItemCost costB, ItemStack result, int maxUses, int xp) {
        offers.add(new MerchantOffer(costA, Optional.of(costB), result, maxUses, xp, 0.05f));
    }

    private static boolean hasOffer(MerchantOffers offers, Item resultItem) {
        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(resultItem)) {
                return true;
            }
        }
        return false;
    }
}
