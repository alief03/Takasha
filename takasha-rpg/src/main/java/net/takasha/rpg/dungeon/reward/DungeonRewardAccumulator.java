package net.takasha.rpg.dungeon.reward;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.dungeon.DungeonTier;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Akumulator Hadiah Dungeon per-Wave.
 * Menghitung dan menyimpan akumulasi koin fisik, dust, scroll, serta item RPG Takasha & AE
 * yang diundi secara dinamis dengan probabilitas persentase (chance) eksplisit.
 */
public class DungeonRewardAccumulator {

    private final List<ItemStack> accumulatedItems = new ArrayList<>();
    private final Random rng = new Random();

    public void clear() {
        accumulatedItems.clear();
    }

    public void accumulateWave(DungeonTier tier, int waveNumber) {
        int coinMultiplier = switch (tier) {
            case COMMON -> 1;
            case NIGHTMARE -> 2;
            case SAKURA_BOSS -> 4;
        };

        switch (waveNumber) {
            case 1 -> {
                // Fixed: 50 RPG Coins, 2 Magic Dust
                addOrStackItem(new ItemStack(ModRpgItems.RPG_COIN, 50 * coinMultiplier));
                addOrStackItem(new ItemStack(ModRpgItems.MAGIC_DUST, 2));

                // Peluang 40%: Mystery Book Simple
                if (rng.nextFloat() < 0.40f) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_SIMPLE, 1));
                }
                // Peluang 15%: Relik Dasar
                if (rng.nextFloat() < 0.15f) {
                    Item relic = rng.nextBoolean() ? ModRpgItems.ASTROLABE_NAVIGATOR : ModRpgItems.TEAR_FROSTBORN;
                    addOrStackItem(new ItemStack(relic, 1));
                }
            }
            case 2 -> {
                // Fixed: 120 RPG Coins, 4 Magic Dust, 1 Mystery Dust
                addOrStackItem(new ItemStack(ModRpgItems.RPG_COIN, 120 * coinMultiplier));
                addOrStackItem(new ItemStack(ModRpgItems.MAGIC_DUST, 4));
                addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_DUST, 1));

                // Peluang 45%: Mystery Book Unique
                if (rng.nextFloat() < 0.45f) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_UNIQUE, 1));
                }
                // Peluang 30%: Stat Tracker Acak
                if (rng.nextFloat() < 0.30f) {
                    Item tracker = switch (rng.nextInt(3)) {
                        case 0 -> ModRpgItems.STAT_TRACKER_KILLS;
                        case 1 -> ModRpgItems.STAT_TRACKER_MOBS;
                        default -> ModRpgItems.STAT_TRACKER_BLOCKS;
                    };
                    addOrStackItem(new ItemStack(tracker, 1));
                }
                // Peluang 20%: Jimat Pemulih
                if (rng.nextFloat() < 0.20f) {
                    addOrStackItem(new ItemStack(ModRpgItems.DREAMCATCHER_CHARM, 1));
                }
            }
            case 3 -> {
                // Fixed: 250 RPG Coins, 1 Gold Coin
                addOrStackItem(new ItemStack(ModRpgItems.RPG_COIN, 250 * coinMultiplier));
                addOrStackItem(new ItemStack(ModRpgItems.RPG_GOLD_COIN, 1 * coinMultiplier));

                // Peluang 40%: Mystery Book Elite
                if (rng.nextFloat() < 0.40f) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_ELITE, 1));
                }
                // Peluang 25%: Mystery Book Ultimate
                if (rng.nextFloat() < 0.25f) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_ULTIMATE, 1));
                }
                // Peluang 35%: 1-2 Item Utilitas AE
                if (rng.nextFloat() < 0.35f) {
                    int count = 1 + rng.nextInt(2);
                    for (int i = 0; i < count; i++) {
                        Item util = switch (rng.nextInt(3)) {
                            case 0 -> ModRpgItems.SLOT_INCREASER_1;
                            case 1 -> ModRpgItems.SECRET_DUST;
                            default -> ModRpgItems.RANDOMIZATION_SCROLL;
                        };
                        addOrStackItem(new ItemStack(util, 1));
                    }
                }
                // Peluang 20%: Kitab Portabel Kuno
                if (rng.nextFloat() < 0.20f) {
                    Item portable = switch (rng.nextInt(3)) {
                        case 0 -> ModRpgItems.TOME_WORKBENCH;
                        case 1 -> ModRpgItems.SCROLL_ASCENSION;
                        default -> ModRpgItems.WANDERER_COMPASS;
                    };
                    addOrStackItem(new ItemStack(portable, 1));
                }
            }
            case 4 -> {
                // Fixed: 500 RPG Coins, 2 Gold Coins, 1 White Scroll
                addOrStackItem(new ItemStack(ModRpgItems.RPG_COIN, 500 * coinMultiplier));
                addOrStackItem(new ItemStack(ModRpgItems.RPG_GOLD_COIN, 2 * coinMultiplier));
                addOrStackItem(new ItemStack(ModRpgItems.WHITE_SCROLL, 1));

                // Peluang Ancient Keystone (+1 Max Home)
                float keystoneChance = switch (tier) {
                    case COMMON -> 0.45f;
                    case NIGHTMARE -> 0.65f;
                    case SAKURA_BOSS -> 0.85f;
                };
                if (rng.nextFloat() < keystoneChance) {
                    addOrStackItem(new ItemStack(ModRpgItems.ANCIENT_KEYSTONE, 1));
                }

                // Peluang 30%: Mystery Book Legendary
                if (rng.nextFloat() < 0.30f) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_LEGENDARY, 1));
                }
                // Peluang 15%: Mystery Book Fabled
                if (rng.nextFloat() < 0.15f) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_FABLED, 1));
                }
                // Peluang 35%: 1-2 Bagian Zirah Set Kit AE (Phantom / Ranger)
                if (rng.nextFloat() < 0.35f) {
                    int count = 1 + rng.nextInt(2);
                    for (int i = 0; i < count; i++) {
                        Item armor = getRandomMidTierArmor();
                        addOrStackItem(new ItemStack(armor, 1));
                    }
                }
                // Peluang 20%: Kitab Tempa Kuno
                if (rng.nextFloat() < 0.20f) {
                    Item smith = switch (rng.nextInt(3)) {
                        case 0 -> ModRpgItems.TOME_ANVIL;
                        case 1 -> ModRpgItems.TOME_SMITHING;
                        default -> ModRpgItems.TOME_GRINDSTONE;
                    };
                    addOrStackItem(new ItemStack(smith, 1));
                }
            }
            case 5 -> {
                // Fixed Boss: 1.000 RPG Coins, 5 Gold Coins, 3 Takasha Gems, 1 Holy Scroll
                addOrStackItem(new ItemStack(ModRpgItems.RPG_COIN, 1000 * coinMultiplier));
                addOrStackItem(new ItemStack(ModRpgItems.RPG_GOLD_COIN, 5 * coinMultiplier));
                addOrStackItem(new ItemStack(ModRpgItems.TAKASHA_GEM, 3 * (tier == DungeonTier.SAKURA_BOSS ? 2 : 1)));
                addOrStackItem(new ItemStack(ModRpgItems.HOLY_SCROLL, 1));

                // Peluang Ancient Keystone (+1 Max Home, batas 10)
                float keystoneChance = switch (tier) {
                    case COMMON -> 0.75f;
                    case NIGHTMARE -> 0.85f;
                    case SAKURA_BOSS -> 0.95f;
                };
                if (rng.nextFloat() < keystoneChance) {
                    addOrStackItem(new ItemStack(ModRpgItems.ANCIENT_KEYSTONE, 1));
                }

                // Peluang Mystery Book Heroic (50%)
                if (rng.nextFloat() < (tier == DungeonTier.COMMON ? 0.50f : 0.70f)) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_HEROIC, 1));
                }
                // Peluang Mystery Book Soul (25% s.d. 75%)
                float soulChance = switch (tier) {
                    case COMMON -> 0.25f;
                    case NIGHTMARE -> 0.50f;
                    case SAKURA_BOSS -> 0.75f;
                };
                if (rng.nextFloat() < soulChance) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_SOUL, 1));
                }
                // Peluang Mystery Book Mastery (10% s.d. 45%)
                float masteryChance = switch (tier) {
                    case COMMON -> 0.10f;
                    case NIGHTMARE -> 0.25f;
                    case SAKURA_BOSS -> 0.45f;
                };
                if (rng.nextFloat() < masteryChance) {
                    addOrStackItem(new ItemStack(ModRpgItems.MYSTERY_BOOK_MASTERY, 1));
                }

                // Peluang 45%: 1-2 Bagian Zirah Set Kit AE Tingkat Tinggi
                if (rng.nextFloat() < 0.45f) {
                    int count = 1 + rng.nextInt(2);
                    for (int i = 0; i < count; i++) {
                        Item highArmor = getRandomHighTierArmor();
                        addOrStackItem(new ItemStack(highArmor, 1));
                    }
                }

                // Peluang Senjata Kosmik AE Acak (25% s.d. 75%)
                float weaponChance = switch (tier) {
                    case COMMON -> 0.25f;
                    case NIGHTMARE -> 0.50f;
                    case SAKURA_BOSS -> 0.75f;
                };
                if (rng.nextFloat() < weaponChance) {
                    Item weapon = switch (rng.nextInt(5)) {
                        case 0 -> ModRpgItems.PHANTOM_SCYTHE;
                        case 1 -> ModRpgItems.KOTH_SWORD;
                        case 2 -> ModRpgItems.KOTH_AXE;
                        case 3 -> ModRpgItems.DIMENSIONAL_SWORD;
                        default -> ModRpgItems.SUPREME_WEAPON;
                    };
                    addOrStackItem(new ItemStack(weapon, 1));
                }

                // Peluang 25%: Relik Sakral Eksekutif Acak
                if (rng.nextFloat() < 0.25f) {
                    Item sacredRelic = switch (rng.nextInt(5)) {
                        case 0 -> ModRpgItems.HAMMER_MASTER_SMITH;
                        case 1 -> ModRpgItems.HAMMER_DIVINE_SMITH;
                        case 2 -> ModRpgItems.SIGIL_VITALITY;
                        case 3 -> ModRpgItems.SIGIL_GOURMET;
                        default -> ModRpgItems.TOME_ENDERCHEST;
                    };
                    addOrStackItem(new ItemStack(sacredRelic, 1));
                }

                // Peluang 35%: Soul Gem atau Orb
                if (rng.nextFloat() < 0.35f) {
                    Item orb = switch (rng.nextInt(4)) {
                        case 0 -> ModRpgItems.SOUL_GEM;
                        case 1 -> ModRpgItems.WEAPON_ENCHANTMENT_ORB;
                        case 2 -> ModRpgItems.ARMOR_ENCHANTMENT_ORB;
                        default -> ModRpgItems.TOOL_ENCHANTMENT_ORB;
                    };
                    addOrStackItem(new ItemStack(orb, 1));
                }
            }
        }
    }

    private Item getRandomMidTierArmor() {
        Item[] midTier = {
                ModRpgItems.PHANTOM_HELMET, ModRpgItems.PHANTOM_CHESTPLATE, ModRpgItems.PHANTOM_LEGGINGS, ModRpgItems.PHANTOM_BOOTS,
                ModRpgItems.RANGER_HELMET, ModRpgItems.RANGER_CHESTPLATE, ModRpgItems.RANGER_LEGGINGS, ModRpgItems.RANGER_BOOTS
        };
        return midTier[rng.nextInt(midTier.length)];
    }

    private Item getRandomHighTierArmor() {
        Item[] highTier = {
                ModRpgItems.DIMENSIONAL_TRAVELLER_HELMET, ModRpgItems.DIMENSIONAL_TRAVELLER_CHESTPLATE, ModRpgItems.DIMENSIONAL_TRAVELLER_LEGGINGS, ModRpgItems.DIMENSIONAL_TRAVELLER_BOOTS,
                ModRpgItems.KOTH_HELMET, ModRpgItems.KOTH_CHESTPLATE, ModRpgItems.KOTH_LEGGINGS, ModRpgItems.KOTH_BOOTS,
                ModRpgItems.SUPREME_HELMET, ModRpgItems.SUPREME_CHESTPLATE, ModRpgItems.SUPREME_LEGGINGS, ModRpgItems.SUPREME_BOOTS,
                ModRpgItems.YETI_HELMET, ModRpgItems.YETI_CHESTPLATE, ModRpgItems.YETI_LEGGINGS, ModRpgItems.YETI_BOOTS,
                ModRpgItems.YIJKI_HELMET, ModRpgItems.YIJKI_CHESTPLATE, ModRpgItems.YIJKI_LEGGINGS, ModRpgItems.YIJKI_BOOTS
        };
        return highTier[rng.nextInt(highTier.length)];
    }

    private void addOrStackItem(ItemStack newStack) {
        if (newStack.isEmpty()) return;

        for (ItemStack existing : accumulatedItems) {
            if (ItemStack.isSameItemSameComponents(existing, newStack) && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, newStack.getCount());
                existing.grow(toAdd);
                newStack.shrink(toAdd);
                if (newStack.isEmpty()) return;
            }
        }

        while (!newStack.isEmpty()) {
            int toTake = Math.min(newStack.getCount(), newStack.getMaxStackSize());
            ItemStack sub = newStack.split(toTake);
            accumulatedItems.add(sub);
        }
    }

    public List<ItemStack> getAccumulatedItems() {
        return new ArrayList<>(accumulatedItems);
    }

    public boolean isEmpty() {
        return accumulatedItems.isEmpty();
    }

    public com.google.gson.JsonArray toJsonArray() {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (ItemStack stack : accumulatedItems) {
            if (stack.isEmpty()) continue;
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            obj.addProperty("count", stack.getCount());
            array.add(obj);
        }
        return array;
    }

    public static DungeonRewardAccumulator fromJsonArray(com.google.gson.JsonArray array) {
        DungeonRewardAccumulator acc = new DungeonRewardAccumulator();
        if (array == null) return acc;
        for (com.google.gson.JsonElement el : array) {
            if (el.isJsonObject()) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                if (obj.has("item") && obj.has("count")) {
                    String itemId = obj.get("item").getAsString();
                    int count = obj.get("count").getAsInt();
                    try {
                        Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(itemId));
                        if (it != null && it != net.minecraft.world.item.Items.AIR) {
                            acc.addOrStackItem(new ItemStack(it, count));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return acc;
    }
}
