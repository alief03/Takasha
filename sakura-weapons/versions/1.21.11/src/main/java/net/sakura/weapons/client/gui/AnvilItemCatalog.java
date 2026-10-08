package net.sakura.weapons.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sakura.weapons.registry.DragonMechaOverlordItems;
import net.sakura.weapons.registry.ModItems;
import net.sakura.weapons.registry.ValentineItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class AnvilItemCatalog {

    public static class Entry {
        private final String setId;
        private final String baseName;
        private final String displayNameEn;
        private final String displayNameId;
        private final String baseItemHint;
        private final Supplier<ItemStack> stackSupplier;
        private ItemStack cachedStack = null;

        public Entry(String setId, String baseName, String displayNameEn, String displayNameId, String baseItemHint, Supplier<ItemStack> stackSupplier) {
            this.setId = setId;
            this.baseName = baseName;
            this.displayNameEn = displayNameEn;
            this.displayNameId = displayNameId;
            this.baseItemHint = baseItemHint;
            this.stackSupplier = stackSupplier;
        }

        public String setId() { return setId; }
        public String baseName() { return baseName; }
        public String displayNameEn() { return displayNameEn; }
        public String displayNameId() { return displayNameId; }
        public String baseItemHint() { return baseItemHint; }

        public ItemStack createStack() {
            if (this.cachedStack != null && !this.cachedStack.isEmpty()) {
                return this.cachedStack;
            }
            try {
                ItemStack stack = stackSupplier.get();
                if (stack != null && !stack.isEmpty()) {
                    this.cachedStack = stack;
                    return this.cachedStack;
                }
            } catch (Throwable ignored) {}
            this.cachedStack = new ItemStack(Items.STICK);
            return this.cachedStack;
        }

        public boolean matchesInput(ItemStack input) {
            if (input == null || input.isEmpty()) {
                return true;
            }
            String base = this.baseName.toLowerCase();

            // 1. Wings: Elytra & Paper (strictly NO chestplate)
            if (base.contains("wing")) {
                return input.is(Items.ELYTRA) || input.is(Items.PAPER);
            }

            // 2. Paper universal check
            if (input.is(Items.PAPER)) {
                return base.contains("wing") || base.contains("key") || base.contains("hat")
                        || base.contains("gauntlet") || base.contains("katana") || base.contains("club")
                        || base.contains("grenade")
                        || (this.setId.equals("valentine") && base.contains("staff"))
                        || (this.setId.equals("dragon_mecha_overlord") && (base.contains("staff") || base.contains("scythe") || base.contains("trident")));
            }

            // 3. Swords
            boolean isInputSword = input.is(ItemTags.SWORDS) || input.getItem().toString().toLowerCase().contains("sword");
            if (isInputSword) {
                return base.contains("sword") || base.contains("katana") || base.contains("dagger")
                        || base.contains("spear") || base.contains("gauntlet") || base.contains("club")
                        || (this.setId.equals("valentine") && base.contains("staff"))
                        || (this.setId.equals("dragon_mecha_overlord") && (base.contains("staff") || base.contains("scythe") || base.contains("trident")));
            }

            // 4. Pickaxes (Evaluated before Axe to avoid "pickaxe".contains("axe") false positive)
            boolean isInputPickaxe = input.is(ItemTags.PICKAXES) || input.getItem().toString().toLowerCase().contains("pickaxe");
            if (isInputPickaxe) {
                return base.contains("pickaxe");
            }

            // 5. Axes
            boolean isInputAxe = input.is(ItemTags.AXES) || (input.getItem().toString().toLowerCase().contains("axe")
                    && !input.getItem().toString().toLowerCase().contains("pickaxe"));
            if (isInputAxe) {
                return (base.contains("axe") && !base.contains("pickaxe"))
                        || base.contains("halberd") || base.contains("hammer")
                        || base.contains("mace")
                        || (this.setId.equals("valentine") && base.contains("staff"))
                        || (this.setId.equals("dragon_mecha_overlord") && (base.contains("staff") || base.contains("scythe")));
            }

            // 6. Shovels
            if (input.is(ItemTags.SHOVELS) || input.getItem().toString().toLowerCase().contains("shovel")) {
                return base.contains("shovel");
            }

            // 7. Hoes
            if (input.is(ItemTags.HOES) || input.getItem().toString().toLowerCase().contains("hoe")) {
                return base.contains("hoe");
            }

            // 8. Mace
            if (input.is(Items.MACE)) {
                return base.contains("mace") || base.contains("hammer");
            }

            // 9. Trident & Spears
            if (input.is(Items.TRIDENT) || input.getItem().toString().toLowerCase().contains("spear")) {
                return base.contains("spear")
                        || (this.setId.equals("valentine") && base.contains("staff"))
                        || (this.setId.equals("dragon_mecha_overlord") && (base.contains("trident") || base.contains("staff") || base.contains("scythe")));
            }

            // 10. Crossbow
            if (input.is(Items.CROSSBOW)) {
                return base.contains("crossbow");
            }

            // 11. Bow
            if (input.is(Items.BOW)) {
                return base.contains("bow") && !base.contains("crossbow");
            }

            // 12. Shield
            if (input.is(Items.SHIELD)) {
                return base.contains("shield");
            }

            // 13. Fishing Rod
            if (input.is(Items.FISHING_ROD) || input.getItem().toString().toLowerCase().contains("fishing_rod")) {
                return base.contains("fishing_rod");
            }

            // 14. Helmets & Carved Pumpkin
            if (input.is(ItemTags.HEAD_ARMOR) || input.is(Items.CARVED_PUMPKIN)) {
                return base.contains("hat") || base.contains("helmet");
            }

            // 15. Chestplate (Armor only, NOT wings)
            if (input.is(ItemTags.CHEST_ARMOR)) {
                return base.contains("chestplate");
            }

            // 16. Leggings
            if (input.is(ItemTags.LEG_ARMOR)) {
                return base.contains("leggings");
            }

            // 17. Boots
            if (input.is(ItemTags.FOOT_ARMOR)) {
                return base.contains("boots");
            }

            // 18. Keys & Staff utility items
            if (input.is(Items.TRIPWIRE_HOOK)) {
                return base.contains("key");
            }
            if (input.is(Items.STICK)) {
                return base.contains("staff") || base.contains("key") || base.contains("grenade") || base.contains("scythe") || base.contains("dagger");
            }
            if (input.is(Items.BLAZE_ROD)) {
                return (!this.setId.equals("valentine") && (base.contains("staff") || base.contains("key")))
                        || (this.setId.equals("dragon_mecha_overlord") && base.contains("staff"));
            }

            // 19. Elytra fallback
            if (input.is(Items.ELYTRA)) {
                return base.contains("wing");
            }

            return false;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean initialized = false;

    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        // --- SAKURA SET (20 Items) ---
        register("sakura", "sakura_katana", "Sakura Katana", "Katana Sakura", "Diamond / Netherite / Iron Sword, Paper", () -> new ItemStack(ModItems.SAKURA_KATANA));
        register("sakura", "sakura_sword", "Sakura Sword", "Pedang Sakura", "Diamond / Netherite / Iron Sword", () -> new ItemStack(ModItems.SAKURA_SWORD));
        register("sakura", "sakura_bigsword", "Sakura Bigsword", "Pedang Besar Sakura", "Diamond / Netherite / Iron Sword", () -> new ItemStack(ModItems.SAKURA_BIGSWORD));
        register("sakura", "sakura_dagger", "Sakura Dagger", "Belati Sakura", "Diamond / Netherite / Iron Sword", () -> new ItemStack(ModItems.SAKURA_DAGGER));
        register("sakura", "sakura_spear", "Sakura Spear", "Tombak Sakura", "Trident, Spear, Diamond / Iron Sword", () -> new ItemStack(ModItems.SAKURA_SPEAR));
        register("sakura", "sakura_halberd", "Sakura Halberd", "Halberd Sakura", "Diamond / Netherite / Iron Axe", () -> new ItemStack(ModItems.SAKURA_HALBERD));
        register("sakura", "sakura_hammer", "Sakura Hammer", "Palu Sakura", "Mace, Diamond / Iron Axe", () -> new ItemStack(ModItems.SAKURA_HAMMER));
        register("sakura", "sakura_club", "Sakura Club", "Gada Sakura", "Diamond / Netherite / Iron Sword, Paper", () -> new ItemStack(ModItems.SAKURA_CLUB));
        register("sakura", "sakura_mace", "Sakura Mace", "Mace Sakura", "Mace, Diamond / Netherite / Iron Axe", () -> new ItemStack(ModItems.SAKURA_MACE));
        register("sakura", "sakura_gauntlet", "Sakura Gauntlet", "Sarung Tangan Sakura", "Diamond / Netherite / Iron Sword, Paper", () -> new ItemStack(ModItems.SAKURA_GAUNTLET));
        register("sakura", "sakura_bow", "Sakura Bow", "Busur Sakura", "Bow", () -> new ItemStack(ModItems.SAKURA_BOW));
        register("sakura", "sakura_shield", "Sakura Shield", "Perisai Sakura", "Shield", () -> new ItemStack(ModItems.SAKURA_SHIELD));
        register("sakura", "sakura_fishing_rod", "Sakura Fishing Rod", "Alat Pancing Sakura", "Fishing Rod", () -> new ItemStack(ModItems.SAKURA_FISHING_ROD));
        register("sakura", "sakura_pickaxe", "Sakura Pickaxe", "Beliung Sakura", "Diamond / Netherite / Iron Pickaxe", () -> new ItemStack(ModItems.SAKURA_PICKAXE));
        register("sakura", "sakura_axe", "Sakura Axe", "Kapak Sakura", "Diamond / Netherite / Iron Axe", () -> new ItemStack(ModItems.SAKURA_AXE));
        register("sakura", "sakura_shovel", "Sakura Shovel", "Sekop Sakura", "Diamond / Netherite / Iron Shovel", () -> new ItemStack(ModItems.SAKURA_SHOVEL));
        register("sakura", "sakura_hoe", "Sakura Hoe", "Cangkul Sakura", "Diamond / Netherite / Iron Hoe", () -> new ItemStack(ModItems.SAKURA_HOE));
        register("sakura", "sakura_hat", "Sakura Hat", "Topi Sakura", "Carved Pumpkin, Helmet, Paper", () -> new ItemStack(ModItems.SAKURA_HAT));
        register("sakura", "sakura_key", "Sakura Key", "Kunci Sakura", "Tripwire Hook, Paper", () -> new ItemStack(ModItems.SAKURA_KEY));
        register("sakura", "sakura_wing", "Sakura Wing", "Sayap Sakura", "Elytra, Paper", () -> new ItemStack(ModItems.SAKURA_WING));

        // --- PINK LEGACY SET (19 Items) ---
        register("pink_legacy", "pink_legacy_sword", "Pink Legacy Sword", "Pedang Pink Legacy", "Diamond / Netherite / Iron Sword", () -> new ItemStack(ModItems.PINK_LEGACY_SWORD));
        register("pink_legacy", "pink_legacy_battle_axe", "Pink Legacy Battle Axe", "Kapak Perang Pink Legacy", "Diamond / Netherite / Iron Axe", () -> new ItemStack(ModItems.PINK_LEGACY_BATTLE_AXE));
        register("pink_legacy", "pink_legacy_spear", "Pink Legacy Spear", "Tombak Pink Legacy", "Trident, Spear, Diamond / Iron Sword", () -> new ItemStack(ModItems.PINK_LEGACY_SPEAR));
        register("pink_legacy", "pink_legacy_halberd", "Pink Legacy Halberd", "Halberd Pink Legacy", "Diamond / Netherite / Iron Axe", () -> new ItemStack(ModItems.PINK_LEGACY_HALBERD));
        register("pink_legacy", "pink_legacy_hammer", "Pink Legacy Hammer", "Palu Pink Legacy", "Mace, Diamond / Iron Axe", () -> new ItemStack(ModItems.PINK_LEGACY_HAMMER));
        register("pink_legacy", "pink_legacy_staff", "Pink Legacy Staff", "Tongkat Pink Legacy", "Blaze Rod, Stick", () -> new ItemStack(ModItems.PINK_LEGACY_STAFF));
        register("pink_legacy", "pink_legacy_bow", "Pink Legacy Bow", "Busur Pink Legacy", "Bow", () -> new ItemStack(ModItems.PINK_LEGACY_BOW));
        register("pink_legacy", "pink_legacy_shield", "Pink Legacy Shield", "Perisai Pink Legacy", "Shield", () -> new ItemStack(ModItems.PINK_LEGACY_SHIELD));
        register("pink_legacy", "pink_legacy_fishing_rod", "Pink Legacy Fishing Rod", "Alat Pancing Pink Legacy", "Fishing Rod", () -> new ItemStack(ModItems.PINK_LEGACY_FISHING_ROD));
        register("pink_legacy", "pink_legacy_pickaxe", "Pink Legacy Pickaxe", "Beliung Pink Legacy", "Diamond / Netherite / Iron Pickaxe", () -> new ItemStack(ModItems.PINK_LEGACY_PICKAXE));
        register("pink_legacy", "pink_legacy_axe", "Pink Legacy Axe", "Kapak Pink Legacy", "Diamond / Netherite / Iron Axe", () -> new ItemStack(ModItems.PINK_LEGACY_AXE));
        register("pink_legacy", "pink_legacy_shovel", "Pink Legacy Shovel", "Sekop Pink Legacy", "Diamond / Netherite / Iron Shovel", () -> new ItemStack(ModItems.PINK_LEGACY_SHOVEL));
        register("pink_legacy", "pink_legacy_hoe", "Pink Legacy Hoe", "Cangkul Pink Legacy", "Diamond / Netherite / Iron Hoe", () -> new ItemStack(ModItems.PINK_LEGACY_HOE));
        register("pink_legacy", "pink_legacy_helmet", "Pink Legacy Helmet", "Helm Pink Legacy", "Diamond / Netherite Helmet", () -> new ItemStack(ModItems.PINK_LEGACY_HELMET));
        register("pink_legacy", "pink_legacy_chestplate", "Pink Legacy Chestplate", "Zirah Pink Legacy", "Diamond / Netherite Chestplate", () -> new ItemStack(ModItems.PINK_LEGACY_CHESTPLATE));
        register("pink_legacy", "pink_legacy_leggings", "Pink Legacy Leggings", "Celana Pink Legacy", "Diamond / Netherite Leggings", () -> new ItemStack(ModItems.PINK_LEGACY_LEGGINGS));
        register("pink_legacy", "pink_legacy_boots", "Pink Legacy Boots", "Sepatu Pink Legacy", "Diamond / Netherite Boots", () -> new ItemStack(ModItems.PINK_LEGACY_BOOTS));
        register("pink_legacy", "pink_legacy_wings", "Pink Legacy Wings", "Sayap Pink Legacy", "Elytra, Paper", () -> new ItemStack(ModItems.PINK_LEGACY_WINGS));
        register("pink_legacy", "pink_legacy_key", "Pink Legacy Key", "Kunci Pink Legacy", "Tripwire Hook, Paper", () -> new ItemStack(ModItems.PINK_LEGACY_KEY));

        // --- VALENTINE SET (20 Items) ---
        register("valentine", "valentine_sword", "Valentine Sword", "Pedang Valentine", "Diamond / Netherite / Iron Sword", () -> new ItemStack(ValentineItems.VALENTINE_SWORD));
        register("valentine", "valentine_axe", "Valentine Axe", "Kapak Valentine", "Diamond / Netherite / Iron Axe", () -> new ItemStack(ValentineItems.VALENTINE_AXE));
        register("valentine", "valentine_hammer", "Valentine Hammer", "Palu Valentine", "Mace, Diamond / Iron Axe", () -> new ItemStack(ValentineItems.VALENTINE_HAMMER));
        register("valentine", "valentine_spear", "Valentine Spear", "Tombak Valentine", "Trident, Spear, Diamond / Iron Sword", () -> new ItemStack(ValentineItems.VALENTINE_SPEAR));
        register("valentine", "valentine_staff", "Valentine Staff", "Tongkat Valentine", "Spear, Axe, Sword, Stick, Paper", () -> new ItemStack(ValentineItems.VALENTINE_STAFF));
        register("valentine", "valentine_pickaxe", "Valentine Pickaxe", "Beliung Valentine", "Diamond / Netherite / Iron Pickaxe", () -> new ItemStack(ValentineItems.VALENTINE_PICKAXE));
        register("valentine", "valentine_shovel", "Valentine Shovel", "Sekop Valentine", "Diamond / Netherite / Iron Shovel", () -> new ItemStack(ValentineItems.VALENTINE_SHOVEL));
        register("valentine", "valentine_hoe", "Valentine Hoe", "Cangkul Valentine", "Diamond / Netherite / Iron Hoe", () -> new ItemStack(ValentineItems.VALENTINE_HOE));
        register("valentine", "valentine_bow", "Valentine Bow", "Busur Valentine", "Bow", () -> new ItemStack(ValentineItems.VALENTINE_BOW));
        register("valentine", "valentine_crossbow", "Valentine Crossbow", "Busur Silang Valentine", "Crossbow", () -> new ItemStack(ValentineItems.VALENTINE_CROSSBOW));
        register("valentine", "valentine_shield", "Valentine Shield", "Perisai Valentine", "Shield", () -> new ItemStack(ValentineItems.VALENTINE_SHIELD));
        register("valentine", "valentine_fishing_rod", "Valentine Fishing Rod", "Alat Pancing Valentine", "Fishing Rod", () -> new ItemStack(ValentineItems.VALENTINE_FISHING_ROD));
        register("valentine", "valentine_hat", "Valentine Hat", "Topi Valentine", "Carved Pumpkin, Helmet, Paper", () -> new ItemStack(ValentineItems.VALENTINE_HAT));
        register("valentine", "valentine_wing", "Valentine Wing", "Sayap Valentine", "Elytra, Paper", () -> new ItemStack(ValentineItems.VALENTINE_WING));
        register("valentine", "valentine_key", "Valentine Key", "Kunci Valentine", "Tripwire Hook, Paper, Stick", () -> new ItemStack(ValentineItems.VALENTINE_KEY));
        register("valentine", "valentine_grenade", "Valentine Grenade", "Granat Valentine", "Paper, Stick", () -> new ItemStack(ValentineItems.VALENTINE_GRENADE));
        register("valentine", "valentine_helmet", "Valentine Helmet", "Helm Valentine", "Diamond / Netherite Helmet", () -> new ItemStack(ValentineItems.VALENTINE_HELMET));
        register("valentine", "valentine_chestplate", "Valentine Chestplate", "Zirah Valentine", "Diamond / Netherite Chestplate", () -> new ItemStack(ValentineItems.VALENTINE_CHESTPLATE));
        register("valentine", "valentine_leggings", "Valentine Leggings", "Celana Valentine", "Diamond / Netherite Leggings", () -> new ItemStack(ValentineItems.VALENTINE_LEGGINGS));
        register("valentine", "valentine_boots", "Valentine Boots", "Sepatu Valentine", "Diamond / Netherite Boots", () -> new ItemStack(ValentineItems.VALENTINE_BOOTS));

        // --- DRAGON MECHA OVERLORD SET (25 Items) ---
        register("dragon_mecha_overlord", "dragon_mecha_overlord_sword", "Dragon Mecha Overlord Sword", "Pedang Dragon Mecha Overlord", "Diamond / Netherite / Iron Sword", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SWORD));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_great_sword", "Dragon Mecha Overlord Great Sword", "Pedang Besar Dragon Mecha Overlord", "Diamond / Netherite / Iron Sword", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_GREAT_SWORD));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_rapier_sword", "Dragon Mecha Overlord Rapier Sword", "Pedang Rapier Dragon Mecha Overlord", "Diamond / Netherite / Iron Sword", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_RAPIER_SWORD));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_dagger", "Dragon Mecha Overlord Dagger", "Belati Dragon Mecha Overlord", "Diamond / Netherite / Iron Sword, Stick", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_DAGGER));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_spear", "Dragon Mecha Overlord Spear", "Tombak Dragon Mecha Overlord", "Trident, Spear, Diamond / Iron Sword", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SPEAR));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_staff", "Dragon Mecha Overlord Staff", "Tongkat Dragon Mecha Overlord", "Spear, Sword, Stick, Blaze Rod, Paper", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_STAFF));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_scythe", "Dragon Mecha Overlord Scythe", "Sabit Dragon Mecha Overlord", "Diamond / Netherite / Iron Sword, Axe, Stick", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SCYTHE));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_hammer", "Dragon Mecha Overlord Hammer", "Palu Dragon Mecha Overlord", "Mace, Diamond / Iron Axe", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HAMMER));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_trident", "Dragon Mecha Overlord Trident", "Trisula Dragon Mecha Overlord", "Trident, Diamond / Iron Sword", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_TRIDENT));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_axe", "Dragon Mecha Overlord Axe", "Kapak Dragon Mecha Overlord", "Diamond / Netherite / Iron Axe", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_AXE));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_pickaxe", "Dragon Mecha Overlord Pickaxe", "Beliung Dragon Mecha Overlord", "Diamond / Netherite / Iron Pickaxe", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_PICKAXE));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_shovel", "Dragon Mecha Overlord Shovel", "Sekop Dragon Mecha Overlord", "Diamond / Netherite / Iron Shovel", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SHOVEL));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_hoe", "Dragon Mecha Overlord Hoe", "Cangkul Dragon Mecha Overlord", "Diamond / Netherite / Iron Hoe", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HOE));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_bow", "Dragon Mecha Overlord Bow", "Busur Dragon Mecha Overlord", "Bow", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_BOW));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_crossbow", "Dragon Mecha Overlord Crossbow", "Busur Silang Dragon Mecha Overlord", "Crossbow", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_CROSSBOW));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_shield", "Dragon Mecha Overlord Shield", "Perisai Dragon Mecha Overlord", "Shield", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SHIELD));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_fishing_rod", "Dragon Mecha Overlord Fishing Rod", "Alat Pancing Dragon Mecha Overlord", "Fishing Rod", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_FISHING_ROD));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_hat", "Dragon Mecha Overlord Hat", "Topi Dragon Mecha Overlord", "Carved Pumpkin, Helmet, Paper", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HAT));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_wing", "Dragon Mecha Overlord Wing", "Sayap Dragon Mecha Overlord", "Elytra, Paper", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_wing_1", "Dragon Mecha Overlord Wing 1", "Sayap Tajam Dragon Mecha Overlord", "Elytra, Paper", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING_1));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_key", "Dragon Mecha Overlord Key", "Kunci Dragon Mecha Overlord", "Tripwire Hook, Paper, Stick", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_KEY));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_helmet", "Dragon Mecha Overlord Helmet", "Helm Dragon Mecha Overlord", "Diamond / Netherite Helmet", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HELMET));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_chestplate", "Dragon Mecha Overlord Chestplate", "Zirah Dragon Mecha Overlord", "Diamond / Netherite Chestplate", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_CHESTPLATE));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_leggings", "Dragon Mecha Overlord Leggings", "Celana Dragon Mecha Overlord", "Diamond / Netherite Leggings", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_LEGGINGS));
        register("dragon_mecha_overlord", "dragon_mecha_overlord_boots", "Dragon Mecha Overlord Boots", "Sepatu Dragon Mecha Overlord", "Diamond / Netherite Boots", () -> new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_BOOTS));
    }

    private static void register(String setId, String baseName, String displayNameEn, String displayNameId, String baseItemHint, Supplier<ItemStack> stackSupplier) {
        ENTRIES.add(new Entry(setId, baseName, displayNameEn, displayNameId, baseItemHint, stackSupplier));
    }

    private static ItemStack getModOrFallback(String name, Item fallback) {
        try {
            Identifier id = Identifier.fromNamespaceAndPath("sakura_weapons", name);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            }
        } catch (Throwable ignored) {}
        return new ItemStack(fallback);
    }

    public static List<Entry> getAllEntries() {
        ensureInitialized();
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<Entry> getEntriesForSet(String setId) {
        ensureInitialized();
        if ("all".equalsIgnoreCase(setId)) {
            return Collections.unmodifiableList(ENTRIES);
        }
        return ENTRIES.stream().filter(e -> e.setId().equalsIgnoreCase(setId)).toList();
    }

    public static List<Entry> getEntries(String setId, ItemStack inputStack) {
        ensureInitialized();
        List<Entry> setEntries = getEntriesForSet(setId);
        if (inputStack == null || inputStack.isEmpty()) {
            return setEntries;
        }
        return setEntries.stream().filter(e -> e.matchesInput(inputStack)).toList();
    }
}
