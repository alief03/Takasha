package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.*;

import java.util.*;

/**
 * 1:1 Parity MMOItemEditionMenu for Takasha-RPG (Fabric 26.2).
 *
 * Reverse-engineered from:
 * net.Indyuce.mmoitems.gui.edition.ItemEdition.class
 * net.Indyuce.mmoitems.stat.type.DoubleStat.class
 * net.Indyuce.mmoitems.stat.type.StringStat.class
 * net.Indyuce.mmoitems.stat.type.BooleanStat.class
 * net.Indyuce.mmoitems.api.edition.StatEdition.class
 */
public class MMOItemEditionMenu {

    private static final int PAGE_SIZE = 21;

    // Authentic 21-slot stat grid in 54-slot chest:
    // Row 2: 19..25, Row 3: 28..34, Row 4: 37..43
    private static final int[] STAT_GRID_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static void open(ServerPlayer player, String type, String id) {
        open(player, type, id, 1);
    }

    public static void open(ServerPlayer player, String type, String id, int page) {
        MMOItemTemplate template = MMOItemManager.getTemplate(type, id);
        if (template == null) {
            template = MMOItemManager.getTemplateById(id);
        }
        if (template == null) {
            player.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §cTemplate not found: " + type + ":" + id));
            return;
        }

        final MMOItemTemplate finalTemplate = template;
        List<MMOStatRegistry.StatDefinition> availableStats = MMOStatRegistry.getApplicableStats(template.getItemType());

        int totalPages = Math.max(1, (int) Math.ceil((double) availableStats.size() / PAGE_SIZE));
        final int currentPage = Math.max(1, Math.min(page, totalPages));

        // Screen title: "Item Edition: <ID>" (1:1 with Spigot ItemEdition.getName())
        Component title = Component.literal("Item Edition: " + template.getId());
        SimpleContainer container = new SimpleContainer(54);

        // Populate slots 1:1 with Spigot ItemEdition
        // Slot 2: Chest (Left Click: Get item / Right Click: Reroll item)
        ItemStack getItem = new ItemStack(Items.CHEST);
        getItem.set(DataComponents.CUSTOM_NAME, Component.literal("§aGet Item"));
        List<Component> getItemLore = new ArrayList<>();
        getItemLore.add(Component.literal("§7Click to get this item."));
        getItemLore.add(Component.empty());
        getItemLore.add(Component.literal("§e► Left click to get this item."));
        getItemLore.add(Component.literal("§e► Right click to reroll this item."));
        getItem.set(DataComponents.LORE, new ItemLore(getItemLore));
        container.setItem(2, getItem);

        // Slot 4: Live Item Preview
        ItemStack previewStack = MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);
        if (previewStack.isEmpty()) {
            previewStack = new ItemStack(Items.BARRIER);
            previewStack.set(DataComponents.CUSTOM_NAME, Component.literal("§cItem Generation Error"));
        }
        container.setItem(4, previewStack);

        // Slot 6: Barrier (Back to Item Explorer)
        ItemStack backButton = new ItemStack(Items.BARRIER);
        backButton.set(DataComponents.CUSTOM_NAME, Component.literal("§cBack"));
        backButton.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to return to the item browser.")
        )));
        container.setItem(6, backButton);

        // Navigation Arrows (Slot 27 & 35)
        if (currentPage > 1) {
            ItemStack prevArrow = new ItemStack(Items.ARROW);
            prevArrow.set(DataComponents.CUSTOM_NAME, Component.literal("§aPrevious Page"));
            container.setItem(27, prevArrow);
        }
        if (currentPage < totalPages) {
            ItemStack nextArrow = new ItemStack(Items.ARROW);
            nextArrow.set(DataComponents.CUSTOM_NAME, Component.literal("§aNext Page"));
            container.setItem(35, nextArrow);
        }

        // 21-Slot Stat Grid
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        for (int i = 0; i < STAT_GRID_SLOTS.length; i++) {
            int slot = STAT_GRID_SLOTS[i];
            int statIndex = startIndex + i;
            if (statIndex < availableStats.size()) {
                MMOStatRegistry.StatDefinition def = availableStats.get(statIndex);
                container.setItem(slot, buildStatIcon(def, finalTemplate));
            } else {
                // Empty placeholder glass pane ONLY within stat grid
                ItemStack emptyPane = new ItemStack(Items.STAINED_GLASS_PANE.gray());
                emptyPane.set(DataComponents.CUSTOM_NAME, Component.literal("§c- No Item Stat -"));
                container.setItem(slot, emptyPane);
            }
        }

        // Open ReadOnlyChestMenu (In-place update to preserve mouse cursor position)
        ReadOnlyChestMenu.openOrUpdate(player, title, container, 6, (slotId, button, clickType, pl) ->
                handleClick(player, finalTemplate, availableStats, currentPage, totalPages, slotId, button, clickType));
    }

    private static ItemStack buildStatIcon(MMOStatRegistry.StatDefinition def, MMOItemTemplate t) {
        ItemStack item = new ItemStack(def.icon());
        item.set(DataComponents.CUSTOM_NAME, Component.literal("§a" + def.displayName()));

        List<Component> lore = new ArrayList<>();
        // Stat Lore description
        if (def.description() != null && !def.description().isEmpty()) {
            lore.add(Component.literal("§7" + def.description()));
            lore.add(Component.empty());
        }

        // Current Value 1:1 formatting
        String valDisplay = getFormattedStatValue(def, t);
        lore.add(Component.literal("§7Current Value: " + valDisplay));
        lore.add(Component.empty());

        // Action Hints 1:1 with Spigot ItemEdition
        switch (def.interactionType()) {
            case BOOLEAN -> {
                lore.add(Component.literal("§e► Left click to switch this value."));
                lore.add(Component.literal("§e► Right click to choose a probability to have this option."));
            }
            case SUB_MENU_ABILITIES, SUB_MENU_ENCHANTS, SUB_MENU_GEMS, SUB_MENU_ELEMENTS,
                 SUB_MENU_UPGRADE, SUB_MENU_SOUNDS, SUB_MENU_PARTICLES, SUB_MENU_RECIPE -> {
                lore.add(Component.literal("§e► Click to open sub-menu."));
            }
            default -> {
                lore.add(Component.literal("§e► Left click to change this value."));
                lore.add(Component.literal("§e► Right click to remove this value."));
            }
        }

        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }

    private static String getFormattedStatValue(MMOStatRegistry.StatDefinition def, MMOItemTemplate t) {
        String id = def.id();
        return switch (id) {
            case "NAME" -> t.hasName() ? ("§f" + t.getName()) : "§cNone";
            case "MATERIAL" -> "§f" + t.getMaterial();
            case "LORE" -> t.getLore().isEmpty() ? "§cNone" : ("§e" + t.getLore().size() + " lines");
            case "CUSTOM_MODEL_DATA" -> t.hasCustomModelData() ? ("§a" + t.getCustomModelData()) : "§a---";
            case "TIER" -> t.hasTier() ? (t.getTier().getPrefixColor() + t.getTier().getDisplayName()) : "§cNone";
            case "ATTACK_DAMAGE" -> t.hasAttackDamage() ? ("§a" + t.getAttackDamage()) : "§a---";
            case "ATTACK_SPEED" -> t.hasAttackSpeed() ? ("§a" + t.getAttackSpeed()) : "§a---";
            case "UNBREAKABLE" -> t.hasUnbreakable() ? (t.isUnbreakable() ? "§aTrue" : "§cFalse") : "§a---";
            case "WILL_BREAK" -> t.hasWillBreak() ? (t.isWillBreak() ? "§aTrue" : "§cFalse") : "§a---";
            case "AUTOSMELT" -> t.hasAutosmelt() ? (t.isAutosmelt() ? "§aTrue" : "§cFalse") : "§a---";
            case "BOUNCING_CRACK" -> t.hasBouncingCrack() ? (t.isBouncingCrack() ? "§aTrue" : "§cFalse") : "§a---";
            case "PICKAXE_POWER" -> t.hasPickaxePower() ? ("§a" + t.getPickaxePower()) : "§a---";
            case "MINING_EFFICIENCY" -> t.hasMiningEfficiency() ? ("§a" + (int) t.getMiningEfficiency() + "%") : "§a---";
            case "MAX_DURABILITY" -> t.hasMaxDurability() ? ("§a" + t.getMaxDurability()) : "§a---";
            case "ABILITIES" -> t.getAbilities().isEmpty() ? "§cNone" : ("§e" + t.getAbilities().size());
            case "ENCHANTS" -> (t.getVanillaEnchants().isEmpty() && t.getAdvancedEnchants().isEmpty()) ? "§cNone" : ("§e" + (t.getVanillaEnchants().size() + t.getAdvancedEnchants().size()));
            case "GEM_SOCKETS" -> t.getGemSockets().isEmpty() ? "§cNone" : ("§e" + t.getGemSockets().size());
            case "GEM_COLOR" -> t.hasGemColor() ? ("§b" + t.getGemColor()) : "§cNone";
            case "GEM_UPGRADE_SCALING" -> "§a" + t.getGemUpgradeScaling();
            case "ITEM_TYPE_RESTRICTION" -> t.getItemTypeRestrictions().isEmpty() ? "§cNone (All)" : ("§e" + String.join(", ", t.getItemTypeRestrictions()));
            case "SUCCESS_RATE" -> t.hasSuccessRate() ? ("§a" + t.getSuccessRate() + "%") : "§a100%";
            case "DISABLE_INTERACTION" -> t.hasDisableAttackPassive() ? (t.isDisableAttackPassive() ? "§aTrue" : "§cFalse") : "§a---";
            case "SET" -> t.hasItemSet() ? ("§6" + t.getItemSet()) : "§cNone";
            case "DISPLAYED_TYPE" -> !t.getDisplayedType().isEmpty() ? ("§f" + t.getDisplayedType()) : "§cDefault";
            case "LORE_FORMAT" -> t.hasLoreFormat() ? ("§f" + t.getLoreFormat()) : "§cDefault";
            case "SHIELD_PATTERN" -> t.hasShieldPattern() ? ("§f" + t.getShieldPattern()) : "§cNone";
            case "DYE_COLOR" -> t.hasDyeColor() ? ("§f" + t.getDyeColor()) : "§cNone";
            case "TRIM_MATERIAL" -> t.hasTrimMaterial() ? ("§b" + t.getTrimMaterial()) : "§cNone";
            case "TRIM_PATTERN" -> t.hasTrimPattern() ? ("§b" + t.getTrimPattern()) : "§cNone";
            case "REQUIRED_CLASS" -> t.hasRequiredClass() ? ("§e" + t.getRequiredClass()) : "§cNone";
            case "PERMISSION" -> t.hasPermission() ? ("§f" + t.getPermission()) : "§cNone";
            case "TWO_HANDED" -> t.hasTwoHanded() ? (t.isTwoHanded() ? "§aTrue" : "§cFalse") : "§a---";
            case "HANDWORN" -> t.hasHandworn() ? (t.isHandworn() ? "§aTrue" : "§cFalse") : "§a---";
            case "HIDE_ENCHANTS" -> t.hasHideEnchants() ? (t.isHideEnchants() ? "§aTrue" : "§cFalse") : "§a---";
            case "CAN_IDENTIFY" -> t.hasCanIdentify() ? (t.isCanIdentify() ? "§aTrue" : "§cFalse") : "§a---";
            case "CAN_DECONSTRUCT" -> t.hasCanDeconstruct() ? (t.isCanDeconstruct() ? "§aTrue" : "§cFalse") : "§a---";
            case "CAN_DESKIN" -> t.hasCanDeskin() ? (t.isCanDeskin() ? "§aTrue" : "§cFalse") : "§a---";
            case "KNOCKBACK" -> t.hasKnockback() ? ("§a" + t.getKnockback()) : "§a---";
            case "RECOIL" -> t.hasRecoil() ? ("§a" + t.getRecoil()) : "§a---";
            case "COMMANDS" -> t.getCommands().isEmpty() ? "§cNone" : ("§e" + t.getCommands().size() + " commands");
            case "PERM_EFFECTS" -> t.getPermEffects().isEmpty() ? "§cNone" : ("§e" + t.getPermEffects().size() + " effects");
            case "POTION_EFFECTS" -> t.getPotionEffects().isEmpty() ? "§cNone" : ("§e" + t.getPotionEffects().size() + " effects");
            case "CUSTOM_SOUNDS" -> t.getCustomSounds().isEmpty() ? "§cNone" : ("§e" + t.getCustomSounds().size() + " sounds");
            case "ITEM_PARTICLES" -> t.hasItemParticles() ? ("§f" + t.getItemParticles()) : "§cNone";
            case "ARROW_PARTICLES" -> t.hasArrowParticles() ? ("§f" + t.getArrowParticles()) : "§cNone";
            default -> {
                MMOStatType st = findStatType(id);
                if (st != null && t.getStats().containsKey(st)) {
                    double v = t.getStats().get(st);
                    if (def.interactionType() == MMOStatRegistry.InteractionType.PERCENTAGE) {
                        yield "§a" + (int) v + "%";
                    } else {
                        yield "§a" + v;
                    }
                }
                yield "§a---";
            }
        };
    }

    private static MMOStatType findStatType(String id) {
        if (id == null) return null;
        if (id.equalsIgnoreCase("SKILL_CRITICAL_STRIKE_CHANCE")) return MMOStatType.SPELL_CRITICAL_STRIKE_CHANCE;
        if (id.equalsIgnoreCase("SKILL_CRITICAL_STRIKE_POWER")) return MMOStatType.SPELL_CRITICAL_STRIKE_POWER;
        try {
            return MMOStatType.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            for (MMOStatType st : MMOStatType.values()) {
                if (st.getKey().equalsIgnoreCase(id) || st.name().equalsIgnoreCase(id)) {
                    return st;
                }
            }
            return null;
        }
    }

    private static void handleClick(ServerPlayer player, MMOItemTemplate template,
                                     List<MMOStatRegistry.StatDefinition> availableStats, int page, int maxPage,
                                     int slotId, int button, ContainerInput clickType) {
        // Slot 2: Get Item / Reroll
        if (slotId == 2) {
            if (button == 0) { // Left click: give item
                ItemStack stack = MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7f, 1.2f);
                player.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Menerima §6" + (template.hasName() ? template.getName() : template.getId())));
            } else { // Right click: reroll revision
                template.setRevisionId(template.getRevisionId() + 1);
                MMOItemDiskManager.saveTemplate(template);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
                open(player, template.getItemType(), template.getId(), page);
            }
            return;
        }

        // Slot 6: Back to Item Explorer
        if (slotId == 6) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            MMOItemBrowserMenu.open(player, template.getItemType(), 0);
            return;
        }

        // Slot 27: Previous Page
        if (slotId == 27 && page > 1) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
            open(player, template.getItemType(), template.getId(), page - 1);
            return;
        }

        // Slot 35: Next Page
        if (slotId == 35 && page < maxPage) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
            open(player, template.getItemType(), template.getId(), page + 1);
            return;
        }

        // Stat Grid Slots
        for (int i = 0; i < STAT_GRID_SLOTS.length; i++) {
            if (STAT_GRID_SLOTS[i] == slotId) {
                int statIdx = (page - 1) * PAGE_SIZE + i;
                if (statIdx >= availableStats.size()) return;

                MMOStatRegistry.StatDefinition def = availableStats.get(statIdx);
                handleStatClick(player, template, def, page, button, clickType);
                return;
            }
        }
    }

    private static void handleStatClick(ServerPlayer pl, MMOItemTemplate t, MMOStatRegistry.StatDefinition def,
                                        int page, int button, ContainerInput clickType) {
        String id = def.id();

        switch (id) {
            case "NAME" -> {
                if (button == 1) { // Right click: remove/reset name
                    t.setName(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Successfully removed Display Name&7."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else { // Left click: prompt chat
                    List<String> promptLines = List.of(
                            "Editing: &eDisplay Name &7(Current: &f" + (t.hasName() ? t.getName() : "None") + "&7)",
                            "Write in the chat the text you want.",
                            "Example: &e&6&lLegendary Katana &7(supports color codes & and &#HEX)"
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Display Name", promptLines,
                            MMOChatEditionEngine.SuggestionType.NAME, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                t.setName(input);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Display Name successfully changed to '").copy()
                                        .append(net.takasha.rpg.mmo.TakashaColorParser.parseItemName(input)).append(net.takasha.rpg.mmo.TakashaColorParser.parse("&7'.")));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "MATERIAL" -> {
                if (button == 1) { // Right click: reset material to default
                    String defMat = MMOItemBrowserMenu.getDefaultMaterialForType(t.getItemType());
                    t.setMaterial(defMat);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Successfully reset Material to &e" + defMat + "&7."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else { // Left click: prompt material
                    List<String> promptLines = List.of(
                            "Editing: &eMaterial &7(Current: &f" + t.getMaterial() + "&7)",
                            "Write in the chat the material you want.",
                            "Examples: &eNETHERITE_SWORD&7, &eDIAMOND_AXE&7, &eBOW&7, &eCROSSBOW"
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Material", promptLines,
                            MMOChatEditionEngine.SuggestionType.MATERIALS, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(clean.toLowerCase(Locale.ROOT)));
                                if (item == Items.AIR) {
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cInvalid material: " + clean));
                                    return false; // Stay in session!
                                }
                                t.setMaterial(clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Material successfully changed to '&e" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "TIER" -> {
                if (button == 1) { // Right click: remove tier
                    t.setTier(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Successfully removed Item Tier§7."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else { // Left click: open visual sub-menu!
                    pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    MMOTierEditionMenu.open(pl, t, page);
                }
            }
            case "LORE" -> {
                if (clickType == ContainerInput.QUICK_MOVE) { // Shift-click: clear lore
                    t.getLore().clear();
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Successfully removed Lore§7."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    MMOLoreEditionMenu.open(pl, t, page);
                }
            }
            case "ELEMENTS" -> {
                pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                MMOElementsEditionMenu.open(pl, t, page);
            }
            case "UPGRADE" -> {
                pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                MMOUpgradeEditionMenu.open(pl, t, page);
            }
            case "ATTACK_DAMAGE" -> handleDoubleStat(pl, t, page, button, "Attack Damage",
                    t.hasAttackDamage() ? String.valueOf(t.getAttackDamage()) : "---",
                    () -> t.setAttackDamage(null),
                    val -> t.setAttackDamage(val));
            case "ATTACK_SPEED" -> handleDoubleStat(pl, t, page, button, "Attack Speed",
                    t.hasAttackSpeed() ? String.valueOf(t.getAttackSpeed()) : "---",
                    () -> t.setAttackSpeed(null),
                    val -> t.setAttackSpeed(val));
            case "CUSTOM_MODEL_DATA" -> handleDoubleStat(pl, t, page, button, "Custom Model Data",
                    t.hasCustomModelData() ? String.valueOf(t.getCustomModelData()) : "---",
                    () -> t.setCustomModelData(null),
                    val -> t.setCustomModelData((int) Math.round(val)));
            case "MAX_DURABILITY" -> handleDoubleStat(pl, t, page, button, "Max Durability",
                    t.hasMaxDurability() ? String.valueOf(t.getMaxDurability()) : "---",
                    () -> t.setMaxDurability(null),
                    val -> t.setMaxDurability((int) Math.round(val)));
            case "PICKAXE_POWER" -> handleDoubleStat(pl, t, page, button, "Pickaxe Power",
                    t.hasPickaxePower() ? String.valueOf(t.getPickaxePower()) : "---",
                    () -> t.setPickaxePower(null),
                    val -> t.setPickaxePower((int) Math.round(val)));
            case "MINING_EFFICIENCY" -> handleDoubleStat(pl, t, page, button, "Mining Efficiency",
                    t.hasMiningEfficiency() ? (t.getMiningEfficiency() + "%") : "---",
                    () -> t.setMiningEfficiency(null),
                    val -> t.setMiningEfficiency(val));
            case "UNBREAKABLE" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasUnbreakable() && t.isUnbreakable(),
                    () -> t.setUnbreakable(!t.hasUnbreakable() || !t.isUnbreakable()));
            case "WILL_BREAK" -> handleBooleanStat(pl, t, page, button, def,
                    !t.hasWillBreak() || t.isWillBreak(),
                    () -> t.setWillBreak(t.hasWillBreak() && !t.isWillBreak()));
            case "AUTOSMELT" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasAutosmelt() && t.isAutosmelt(),
                    () -> t.setAutosmelt(!t.hasAutosmelt() || !t.isAutosmelt()));
            case "BOUNCING_CRACK" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasBouncingCrack() && t.isBouncingCrack(),
                    () -> t.setBouncingCrack(!t.hasBouncingCrack() || !t.isBouncingCrack()));
            case "DISABLE_INTERACTION" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasDisableAttackPassive() && t.isDisableAttackPassive(),
                    () -> t.setDisableAttackPassive(!t.hasDisableAttackPassive() || !t.isDisableAttackPassive()));
            case "ABILITIES" -> {
                pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                MMOAbilityEditionMenu.open(pl, t);
            }
            case "ENCHANTS" -> {
                pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                MMOEnchantEditionMenu.open(pl, t);
            }
            case "GEM_SOCKETS" -> {
                pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                MMOGemSocketMenu.open(pl, t, page);
            }
            case "GEM_COLOR" -> {
                if (button == 1) { // Right click: reset
                    t.setGemColor(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Successfully removed Gem Color."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eGem Color &7(Current: &b" + (t.hasGemColor() ? t.getGemColor() : "None") + "&7)",
                            "Write in the chat the gem socket color (e.g. 'Red', 'Blue', 'Yellow', 'Green', 'Prismatic', 'Nether')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Gem Color", promptLines,
                            MMOChatEditionEngine.SuggestionType.GEM_COLORS, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim();
                                if (clean.isEmpty()) return false;
                                t.setGemColor(clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Gem Color successfully set to '&b" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "GEM_UPGRADE_SCALING" -> {
                // Cycle 1:1 with Spigot ChooseStat: SUBSEQUENT -> NEVER -> HISTORIC -> SUBSEQUENT
                String cur = t.getGemUpgradeScaling();
                String next = switch (cur.toUpperCase(Locale.ROOT)) {
                    case "SUBSEQUENT" -> "NEVER";
                    case "NEVER" -> "HISTORIC";
                    default -> "SUBSEQUENT";
                };
                t.setGemUpgradeScaling(next);
                MMOItemDiskManager.saveTemplate(t);
                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Gem Upgrade Scaling set to &a" + next + "&7."));
                open(pl, t.getItemType(), t.getId(), page);
            }
            case "ITEM_TYPE_RESTRICTION" -> {
                if (button == 1) { // Right click: clear restrictions
                    t.getItemTypeRestrictions().clear();
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Cleared all Item Type Restrictions."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eItem Type Restriction &7(Current: &f" + (t.getItemTypeRestrictions().isEmpty() ? "All" : String.join(", ", t.getItemTypeRestrictions())) + "&7)",
                            "Write in the chat the item type to allow (e.g. 'WEAPON', 'ARMOR', 'SWORD', 'BOW').",
                            "Type an existing type again to toggle/remove it."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Item Restriction", promptLines,
                            MMOChatEditionEngine.SuggestionType.ITEM_TYPES, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                if (clean.isEmpty()) return false;
                                if (t.getItemTypeRestrictions().contains(clean)) {
                                    t.getItemTypeRestrictions().remove(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cRemoved restriction: " + clean));
                                } else {
                                    t.getItemTypeRestrictions().add(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &aAdded restriction: " + clean));
                                }
                                MMOItemDiskManager.saveTemplate(t);
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "SUCCESS_RATE" -> {
                if (button == 1) {
                    t.setSuccessRate(100.0);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Reset Success Rate to &a100.0%&7."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eSuccess Rate &7(Current: &a" + t.getSuccessRate() + "%&7)",
                            "Write in the chat the success rate in % (e.g. '90.0' or '75.5')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Success Rate", promptLines,
                            MMOChatEditionEngine.SuggestionType.NUMERIC, List.of("100.0", "95.0", "90.0", "85.0", "75.0", "50.0"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                try {
                                    double val = Double.parseDouble(input.replace("%", "").trim());
                                    t.setSuccessRate(Math.clamp(val, 0.0, 100.0));
                                    MMOItemDiskManager.saveTemplate(t);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Success Rate successfully changed to &a" + t.getSuccessRate() + "%&7."));
                                    open(pl, t.getItemType(), t.getId(), page);
                                    return true;
                                } catch (Exception e) {
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cInvalid number format: " + input));
                                    return false;
                                }
                            });
                }
            }
            case "SET" -> {
                if (button == 1) {
                    t.setItemSet(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Successfully removed Equipment Set."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eEquipment Set &7(Current: &6" + (t.hasItemSet() ? t.getItemSet() : "None") + "&7)",
                            "Write in the chat the equipment set name (e.g. 'STEEL', 'DRAGON', 'NETHERITE')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Item Set", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("STEEL", "DRAGON", "NETHERITE", "PALADIN", "ASSASSIN"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                t.setItemSet(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Equipment Set set to '&6" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "DISPLAYED_TYPE" -> {
                if (button == 1) {
                    t.setDisplayedType("");
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Reset Displayed Type to default."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eDisplayed Type &7(Current: &f" + (!t.getDisplayedType().isEmpty() ? t.getDisplayedType() : "Default") + "&7)",
                            "Write in the chat the displayed type name (supports color codes &).",
                            "Examples: &e&6Legendary Greatsword&7, &e&bFrost Staff"
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Displayed Type", promptLines,
                            MMOChatEditionEngine.SuggestionType.NAME, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                t.setDisplayedType(input.trim());
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Displayed Type changed to '").copy()
                                        .append(net.takasha.rpg.mmo.TakashaColorParser.parse(input)).append(net.takasha.rpg.mmo.TakashaColorParser.parse("&7'.")));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "LORE_FORMAT" -> {
                if (button == 1) {
                    t.setLoreFormat(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Reset Lore Format to default."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eLore Format &7(Current: &f" + (t.hasLoreFormat() ? t.getLoreFormat() : "Default") + "&7)",
                            "Write in the chat the lore format identifier (e.g. 'DEFAULT', 'TIERED', 'CUSTOM')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Lore Format", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("DEFAULT", "TIERED", "COMPACT", "DETAILED"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                t.setLoreFormat(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Lore Format set to '&e" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "SHIELD_PATTERN" -> {
                if (button == 1) {
                    t.setShieldPattern(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Shield Pattern."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eShield Pattern &7(Current: &f" + (t.hasShieldPattern() ? t.getShieldPattern() : "None") + "&7)",
                            "Write in the chat the shield banner pattern code."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Shield Pattern", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                t.setShieldPattern(input.trim());
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Shield Pattern set to '&f" + input.trim() + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "DYE_COLOR" -> {
                if (button == 1) {
                    t.setDyeColor(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Dye Color."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eDye Color &7(Current: &f" + (t.hasDyeColor() ? t.getDyeColor() : "None") + "&7)",
                            "Write in the chat RGB format 'R, G, B' (e.g. '255, 0, 128') or hex code '#FF0080'."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Dye Color", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("255, 0, 0", "0, 255, 0", "0, 0, 255", "255, 215, 0", "138, 43, 226"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                t.setDyeColor(input.trim());
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Dye Color set to '&f" + input.trim() + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "TRIM_MATERIAL" -> {
                if (button == 1) {
                    t.setTrimMaterial(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Trim Material."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eTrim Material &7(Current: &b" + (t.hasTrimMaterial() ? t.getTrimMaterial() : "None") + "&7)",
                            "Write in the chat the trim material (e.g. 'NETHERITE', 'DIAMOND', 'GOLD', 'AMETHYST')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Trim Material", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("NETHERITE", "DIAMOND", "GOLD", "EMERALD", "IRON", "AMETHYST", "REDSTONE", "LAPIS", "QUARTZ", "COPPER"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                t.setTrimMaterial(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Trim Material set to '&b" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "TRIM_PATTERN" -> {
                if (button == 1) {
                    t.setTrimPattern(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Trim Pattern."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eTrim Pattern &7(Current: &b" + (t.hasTrimPattern() ? t.getTrimPattern() : "None") + "&7)",
                            "Write in the chat the armor trim pattern (e.g. 'SENTRY', 'VEX', 'WILD', 'COAST', 'DUNE', 'WARD', 'SILENCE')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Trim Pattern", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("SENTRY", "VEX", "WILD", "COAST", "DUNE", "WAYFINDER", "RAISER", "SHAPER", "HOST", "WARD", "SILENCE", "TIDE", "SNOUT", "RIB", "EYE", "SPIRE", "FLOW", "BOLT"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                t.setTrimPattern(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Trim Pattern set to '&b" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "REQUIRED_CLASS" -> {
                if (button == 1) {
                    t.setRequiredClass(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Required Class."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eRequired Class &7(Current: &e" + (t.hasRequiredClass() ? t.getRequiredClass() : "None") + "&7)",
                            "Write in the chat the class requirement (e.g. 'WARRIOR', 'MAGE', 'ARCHER', 'ASSASSIN', 'PALADIN')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Required Class", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("WARRIOR", "MAGE", "ARCHER", "ASSASSIN", "PALADIN", "BERSERKER"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                t.setRequiredClass(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Required Class set to '&e" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "PERMISSION" -> {
                if (button == 1) {
                    t.setPermission(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Permission requirement."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &ePermission &7(Current: &f" + (t.hasPermission() ? t.getPermission() : "None") + "&7)",
                            "Write in the chat the permission node required to use this item (e.g. 'takasha.rpg.weapon.katana')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Permission", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim();
                                t.setPermission(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Permission set to '&f" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "TWO_HANDED" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasTwoHanded() && t.isTwoHanded(),
                    () -> t.setTwoHanded(!t.hasTwoHanded() || !t.isTwoHanded()));
            case "HANDWORN" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasHandworn() && t.isHandworn(),
                    () -> t.setHandworn(!t.hasHandworn() || !t.isHandworn()));
            case "HIDE_ENCHANTS" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasHideEnchants() && t.isHideEnchants(),
                    () -> t.setHideEnchants(!t.hasHideEnchants() || !t.isHideEnchants()));
            case "CAN_IDENTIFY" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasCanIdentify() && t.isCanIdentify(),
                    () -> t.setCanIdentify(!t.hasCanIdentify() || !t.isCanIdentify()));
            case "CAN_DECONSTRUCT" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasCanDeconstruct() && t.isCanDeconstruct(),
                    () -> t.setCanDeconstruct(!t.hasCanDeconstruct() || !t.isCanDeconstruct()));
            case "CAN_DESKIN" -> handleBooleanStat(pl, t, page, button, def,
                    t.hasCanDeskin() && t.isCanDeskin(),
                    () -> t.setCanDeskin(!t.hasCanDeskin() || !t.isCanDeskin()));
            case "KNOCKBACK" -> handleDoubleStat(pl, t, page, button, "Knockback",
                    t.hasKnockback() ? String.valueOf(t.getKnockback()) : "---",
                    () -> t.setKnockback(null),
                    val -> t.setKnockback(val));
            case "RECOIL" -> handleDoubleStat(pl, t, page, button, "Recoil",
                    t.hasRecoil() ? String.valueOf(t.getRecoil()) : "---",
                    () -> t.setRecoil(null),
                    val -> t.setRecoil(val));
            case "COMMANDS" -> {
                if (button == 1) {
                    t.getCommands().clear();
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Cleared all Commands."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eCommands &7(Current: &e" + t.getCommands().size() + " commands&7)",
                            "Write in the chat the command to add/remove (e.g. 'heal %player%' or 'give %player% diamond 1').",
                            "Type an existing command again to remove it."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Commands", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, null,
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim();
                                if (clean.isEmpty()) return false;
                                if (t.getCommands().contains(clean)) {
                                    t.getCommands().remove(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cRemoved command: " + clean));
                                } else {
                                    t.getCommands().add(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &aAdded command: " + clean));
                                }
                                MMOItemDiskManager.saveTemplate(t);
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "PERM_EFFECTS" -> {
                if (button == 1) {
                    t.getPermEffects().clear();
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Cleared all Permanent Effects."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &ePermanent Effects &7(Current: &e" + t.getPermEffects().size() + " effects&7)",
                            "Write in the chat the potion effect and amplifier to add/remove.",
                            "Format: &e<EFFECT>:<AMPLIFIER> &7(e.g. 'SPEED:1', 'NIGHT_VISION:1', 'STRENGTH:2')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Permanent Effects", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("SPEED:1", "SPEED:2", "HASTE:1", "HASTE:2", "STRENGTH:1", "RESISTANCE:1", "NIGHT_VISION:1", "REGENERATION:1", "FIRE_RESISTANCE:1", "WATER_BREATHING:1"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                if (clean.isEmpty()) return false;
                                if (t.getPermEffects().contains(clean)) {
                                    t.getPermEffects().remove(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cRemoved permanent effect: " + clean));
                                } else {
                                    t.getPermEffects().add(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &aAdded permanent effect: " + clean));
                                }
                                MMOItemDiskManager.saveTemplate(t);
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "POTION_EFFECTS" -> {
                if (button == 1) {
                    t.getPotionEffects().clear();
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Cleared all Potion Effects."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &ePotion Effects &7(Current: &e" + t.getPotionEffects().size() + " effects&7)",
                            "Write in the chat the potion effect to trigger on hit/use.",
                            "Format: &e<EFFECT>:<DURATION_SEC>:<AMPLIFIER> &7(e.g. 'BLINDNESS:5:1', 'POISON:4:2')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Potion Effects", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("BLINDNESS:5:1", "POISON:4:1", "SLOWNESS:5:2", "WITHER:4:1", "WEAKNESS:5:1"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                if (clean.isEmpty()) return false;
                                if (t.getPotionEffects().contains(clean)) {
                                    t.getPotionEffects().remove(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cRemoved potion effect: " + clean));
                                } else {
                                    t.getPotionEffects().add(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &aAdded potion effect: " + clean));
                                }
                                MMOItemDiskManager.saveTemplate(t);
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "CUSTOM_SOUNDS" -> {
                if (button == 1) {
                    t.getCustomSounds().clear();
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Cleared all Custom Sounds."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eCustom Sounds &7(Current: &e" + t.getCustomSounds().size() + " sounds&7)",
                            "Write in the chat the sound identifier (e.g. 'entity.ender_dragon.growl', 'item.trident.thunder')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Custom Sounds", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("entity.ender_dragon.growl", "item.trident.thunder", "entity.lightning_bolt.thunder", "entity.wither.spawn"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim();
                                if (clean.isEmpty()) return false;
                                if (t.getCustomSounds().contains(clean)) {
                                    t.getCustomSounds().remove(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cRemoved custom sound: " + clean));
                                } else {
                                    t.getCustomSounds().add(clean);
                                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &aAdded custom sound: " + clean));
                                }
                                MMOItemDiskManager.saveTemplate(t);
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "ITEM_PARTICLES" -> {
                if (button == 1) {
                    t.setItemParticles(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Item Particles."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eItem Particles &7(Current: &f" + (t.hasItemParticles() ? t.getItemParticles() : "None") + "&7)",
                            "Write in the chat the particle effect identifier (e.g. 'FLAME', 'SOUL_FIRE_FLAME', 'ENCHANT', 'PORTAL')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Item Particles", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("FLAME", "SOUL_FIRE_FLAME", "ENCHANT", "PORTAL", "DRAGON_BREATH", "END_ROD", "HEART"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                t.setItemParticles(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Item Particles set to '&f" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            case "ARROW_PARTICLES" -> {
                if (button == 1) {
                    t.setArrowParticles(null);
                    MMOItemDiskManager.saveTemplate(t);
                    pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed Arrow Particles."));
                    open(pl, t.getItemType(), t.getId(), page);
                } else {
                    List<String> promptLines = List.of(
                            "Editing: &eArrow Particles &7(Current: &f" + (t.hasArrowParticles() ? t.getArrowParticles() : "None") + "&7)",
                            "Write in the chat the arrow particle trail identifier (e.g. 'CRIT', 'FIREWORK', 'DRAGON_BREATH')."
                    );
                    MMOChatEditionEngine.beginSession(pl, "Item Edition", "Arrow Particles", promptLines,
                            MMOChatEditionEngine.SuggestionType.GENERIC, List.of("CRIT", "ENCHANTED_HIT", "FIREWORK", "DRAGON_BREATH", "SONIC_BOOM"),
                            () -> open(pl, t.getItemType(), t.getId(), page),
                            input -> {
                                String clean = input.trim().toUpperCase(Locale.ROOT);
                                t.setArrowParticles(clean.isEmpty() ? null : clean);
                                MMOItemDiskManager.saveTemplate(t);
                                pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Arrow Particles set to '&f" + clean + "&7'."));
                                open(pl, t.getItemType(), t.getId(), page);
                                return true;
                            });
                }
            }
            default -> {
                MMOStatType st = findStatType(id);
                if (st != null) {
                    handleDoubleStat(pl, t, page, button, def.displayName(),
                            t.getStats().containsKey(st) ? String.valueOf(t.getStats().get(st)) : "---",
                            () -> t.getStats().remove(st),
                            val -> t.getStats().put(st, val));
                } else {
                    pl.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Stat " + def.displayName() + " dikonfigurasi melalui config!"));
                }
            }
        }
    }

    private static void handleDoubleStat(ServerPlayer pl, MMOItemTemplate t, int page, int button,
                                         String statName, String currentDisplay, Runnable onRemove,
                                         java.util.function.DoubleConsumer onSet) {
        if (button == 1) { // Right click: remove stat
            onRemove.run();
            MMOItemDiskManager.saveTemplate(t);
            pl.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Successfully removed " + statName + "§7."));
            open(pl, t.getItemType(), t.getId(), page);
        } else { // Left click: prompt chat
            List<String> promptLines = List.of(
                    "Write in the chat the numeric value you want.",
                    "Second Format: {Base} {Scaling Value} {Spread} {Max Spread} {Hard Min} {Hard Max}",
                    "Third Format: {Min Value} -> {Max Value}",
                    "Type 'cancel' to abort editing."
            );
            MMOChatEditionEngine.beginSession(pl, "Item Edition", statName, promptLines,
                    MMOChatEditionEngine.SuggestionType.NUMERIC, null,
                    () -> open(pl, t.getItemType(), t.getId(), page),
                    input -> {
                        double parsed = parseNumericStatFormula(input.trim());
                        onSet.accept(parsed);
                        MMOItemDiskManager.saveTemplate(t);
                        pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7" + statName + " successfully changed to {" + parsed + " - 0.0 - 0.0 - 0.0 - (0.0 -> 0.0)}"));
                        open(pl, t.getItemType(), t.getId(), page);
                        return true;
                    });
        }
    }

    private static void handleBooleanStat(ServerPlayer pl, MMOItemTemplate t, int page, int button,
                                          MMOStatRegistry.StatDefinition def, boolean currentValue,
                                          Runnable onToggle) {
        if (button == 0) { // Left click: toggle value directly
            onToggle.run();
            MMOItemDiskManager.saveTemplate(t);
            pl.level().playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
            open(pl, t.getItemType(), t.getId(), page);
        } else { // Right click: prompt chance probability (percentage)
            List<String> promptLines = List.of(
                    "Editing: §e" + def.displayName() + " Chance",
                    "Write in the chat the probability you want (a percentage)",
                    "Examples: §e50 §7(for 50%), §e75.5 §7(for 75.5%)"
            );
            MMOChatEditionEngine.beginSession(pl, "Item Edition", def.displayName() + " Chance", promptLines,
                    MMOChatEditionEngine.SuggestionType.ELEMENTS, List.of("25.0", "50.0", "75.0", "100.0"),
                    () -> open(pl, t.getItemType(), t.getId(), page),
                    input -> {
                        double chance = Double.parseDouble(input.trim());
                        if (chance < 0 || chance > 100) {
                            pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cChance must be between 0 and 100"));
                            return false; // Stay in session!
                        }
                        onToggle.run();
                        MMOItemDiskManager.saveTemplate(t);
                        pl.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7" + def.displayName() + " successfully changed to &a" + chance + "%&7."));
                        open(pl, t.getItemType(), t.getId(), page);
                        return true;
                    });
        }
    }

    private static double parseNumericStatFormula(String input) {
        if (input.contains("->")) {
            String[] parts = input.split("->");
            double min = Double.parseDouble(parts[0].trim());
            double max = Double.parseDouble(parts[1].trim());
            return (min + max) / 2.0;
        } else if (input.contains(" ")) {
            String[] parts = input.split("\\s+");
            return Double.parseDouble(parts[0].trim());
        } else {
            return Double.parseDouble(input.trim());
        }
    }
}
