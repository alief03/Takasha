package net.takasha.rpg.mmo;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.economy.catalog.EnchantmentDetailHelper;
import net.takasha.rpg.enchant.TakashaEnchantManager;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.item.TakashaCustomBookItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MMOItemManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-mmo");
    private static final Map<String, MMOItemTemplate> TEMPLATES = new ConcurrentHashMap<>();

    public static void registerTemplate(MMOItemTemplate template) {
        String key = template.getItemType().toUpperCase(Locale.ROOT) + ":" + template.getId().toUpperCase(Locale.ROOT);
        TEMPLATES.put(key, template);
        LOGGER.info("Registered MMOItem template: {}", key);
    }

    public static void saveAndRegisterTemplate(MMOItemTemplate template) {
        registerTemplate(template);
        MMOItemDiskManager.saveTemplate(template);
    }

    public static MMOItemTemplate getTemplate(String type, String id) {
        if (type == null || id == null) return null;
        String key = type.toUpperCase(Locale.ROOT) + ":" + id.toUpperCase(Locale.ROOT);
        return TEMPLATES.get(key);
    }

    public static MMOItemTemplate getTemplateById(String idOrKey) {
        if (idOrKey == null || idOrKey.isEmpty()) return null;
        String clean = idOrKey.trim();
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }

        // 1. Direct type:id format (e.g. TOOL:CRACK_PICKAXE)
        if (clean.contains(":")) {
            String[] parts = clean.split(":", 2);
            MMOItemTemplate direct = getTemplate(parts[0], parts[1]);
            if (direct != null) return direct;
            clean = parts[1];
        }

        // 2. Direct ID match
        for (MMOItemTemplate t : TEMPLATES.values()) {
            if (t.getId().equalsIgnoreCase(clean)) {
                return t;
            }
        }

        // 3. Normalized / Suffix / Partial match
        String lower = clean.toLowerCase(Locale.ROOT);
        for (MMOItemTemplate t : TEMPLATES.values()) {
            String tId = t.getId().toLowerCase(Locale.ROOT);
            if (tId.equals(lower) || tId.endsWith("_" + lower)) {
                return t;
            }
        }
        for (MMOItemTemplate t : TEMPLATES.values()) {
            if (t.getId().toLowerCase(Locale.ROOT).contains(lower)) {
                return t;
            }
        }
        return null;
    }

    public static List<MMOItemTemplate> getTemplatesByType(String type) {
        List<MMOItemTemplate> list = new ArrayList<>();
        if (type == null || type.isEmpty() || "ALL".equalsIgnoreCase(type)) {
            list.addAll(TEMPLATES.values());
        } else {
            String target = type.toUpperCase(Locale.ROOT);
            for (MMOItemTemplate t : TEMPLATES.values()) {
                if (t.getItemType().equalsIgnoreCase(target)) {
                    list.add(t);
                }
            }
        }
        list.sort(Comparator.comparing((MMOItemTemplate t) -> t.getTier() != null ? t.getTier().ordinal() : -1)
                .thenComparing(MMOItemTemplate::getId));
        return list;
    }

    public static Set<String> getAllItemTypes() {
        Set<String> types = new TreeSet<>();
        for (MMOItemTemplate t : TEMPLATES.values()) {
            types.add(t.getItemType().toUpperCase(Locale.ROOT));
        }
        if (types.isEmpty()) {
            types.addAll(List.of("TOOL", "SWORD", "AXE", "ARMOR", "ACCESSORY", "CONSUMABLE"));
        }
        return types;
    }

    public static boolean deleteTemplate(String type, String id) {
        if (id == null) return false;
        String resolvedType = type;
        String key = (resolvedType == null || resolvedType.isEmpty()) ? id.toUpperCase(Locale.ROOT) : (resolvedType.toUpperCase(Locale.ROOT) + ":" + id.toUpperCase(Locale.ROOT));
        MMOItemTemplate removed = TEMPLATES.remove(key);
        if (removed == null) {
            removed = TEMPLATES.remove(id.toUpperCase(Locale.ROOT));
        }
        if (removed != null && (resolvedType == null || resolvedType.isEmpty())) {
            resolvedType = removed.getItemType();
        }
        if (resolvedType != null) {
            MMOItemDiskManager.deleteTemplateDisk(resolvedType, id);
        }
        return removed != null;
    }

    public static MMOItemTemplate copyTemplate(String type, String oldId, String newId) {
        MMOItemTemplate source = getTemplate(type, oldId);
        if (source == null) return null;
        MMOItemTemplate copy = new MMOItemTemplate(newId.toUpperCase(Locale.ROOT), type.toUpperCase(Locale.ROOT));
        copy.setMaterial(source.getMaterial());
        copy.setName(source.getName());
        copy.setTier(source.getTier());
        copy.setCustomModelData(source.getCustomModelData());
        copy.getLore().addAll(source.getLore());
        copy.getStats().putAll(source.getStats());
        copy.getAbilities().addAll(source.getAbilities());
        copy.getGemSockets().addAll(source.getGemSockets());
        copy.setRevisionId(source.getRevisionId() + 1);
        saveAndRegisterTemplate(copy);
        return copy;
    }

    public static Collection<MMOItemTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(TEMPLATES.values());
    }

    public static void clear() {
        TEMPLATES.clear();
    }

    public static void loadItemTemplates(InputStream is) {
        loadItemTemplates(is, "TOOL");
    }

    public static void loadItemTemplates(InputStream is, String defaultType) {
        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Object loaded = yaml.load(is);
            if (loaded instanceof Map<?, ?> rootMap) {
                loadItemTemplatesMap(rootMap, defaultType);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load MMOItem templates from stream", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadItemTemplatesMap(Map<?, ?> rootMap, String defaultType) {
        if (rootMap == null) return;
        for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object val = entry.getValue();
            if (val instanceof Map<?, ?> itemMap) {
                loadFromYaml(defaultType, key, (Map<String, Object>) itemMap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadFromYaml(String type, String id, Map<String, Object> map) {
        MMOItemTemplate template = new MMOItemTemplate(id, type);

        Map<String, Object> base = (Map<String, Object>) map.get("base");
        if (base == null) {
            base = map;
        }

        if (base.containsKey("material")) {
            template.setMaterial(String.valueOf(base.get("material")).toUpperCase(Locale.ROOT));
        }

        if (base.containsKey("custom-model-data")) {
            Object cmdObj = base.get("custom-model-data");
            if (cmdObj instanceof Number n) {
                template.setCustomModelData(n.intValue());
            }
        }

        if (base.containsKey("name")) {
            template.setName(String.valueOf(base.get("name")));
        }

        if (base.containsKey("lore")) {
            Object loreObj = base.get("lore");
            if (loreObj instanceof List<?> list) {
                for (Object o : list) {
                    template.getLore().add(String.valueOf(o));
                }
            }
        }

        if (base.containsKey("displayed-type")) {
            template.setDisplayedType(String.valueOf(base.get("displayed-type")));
        }

        if (base.containsKey("attack-damage")) {
            Object ad = base.get("attack-damage");
            if (ad instanceof Number n) {
                template.setAttackDamage(n.doubleValue());
            } else if (ad instanceof Map<?, ?> sm && sm.get("base") instanceof Number bn) {
                template.setAttackDamage(bn.doubleValue());
            }
        }

        if (base.containsKey("attack-speed")) {
            Object as = base.get("attack-speed");
            if (as instanceof Number n) {
                template.setAttackSpeed(n.doubleValue());
            } else if (as instanceof Map<?, ?> sm && sm.get("base") instanceof Number bn) {
                template.setAttackSpeed(bn.doubleValue());
            }
        }

        if (base.containsKey("unbreakable")) {
            template.setUnbreakable(Boolean.parseBoolean(String.valueOf(base.get("unbreakable"))));
        }

        if (base.containsKey("disable-attack-passive")) {
            template.setDisableAttackPassive(Boolean.parseBoolean(String.valueOf(base.get("disable-attack-passive"))));
        }

        if (base.containsKey("tier")) {
            template.setTier(MMOItemTier.fromString(String.valueOf(base.get("tier"))));
        }

        // Tool Attributes 1:1
        if (base.containsKey("autosmelt")) {
            template.setAutosmelt(Boolean.parseBoolean(String.valueOf(base.get("autosmelt"))));
        }
        if (base.containsKey("bouncing-crack")) {
            template.setBouncingCrack(Boolean.parseBoolean(String.valueOf(base.get("bouncing-crack"))));
        }
        if (base.containsKey("pickaxe-power")) {
            Object pp = base.get("pickaxe-power");
            if (pp instanceof Number n) template.setPickaxePower(n.intValue());
        }
        if (base.containsKey("mining-efficiency")) {
            Object me = base.get("mining-efficiency");
            if (me instanceof Number n) template.setMiningEfficiency(n.doubleValue());
        }
        if (base.containsKey("max-durability")) {
            Object md = base.get("max-durability");
            if (md instanceof Number n) template.setMaxDurability(n.intValue());
        }
        if (base.containsKey("will-break")) {
            template.setWillBreak(Boolean.parseBoolean(String.valueOf(base.get("will-break"))));
        }

        // Requirements & Stats (from base)
        for (MMOStatType statType : MMOStatType.values()) {
            if (base.containsKey(statType.getKey())) {
                Object val = base.get(statType.getKey());
                if (val instanceof Number n) {
                    template.getStats().put(statType, n.doubleValue());
                } else if (val instanceof Map<?, ?> subMap && subMap.containsKey("base") && subMap.get("base") instanceof Number bn) {
                    template.getStats().put(statType, bn.doubleValue());
                }
            }
        }

        // Vanilla Enchants
        if (base.containsKey("enchants")) {
            Object enchObj = base.get("enchants");
            if (enchObj instanceof Map<?, ?> enchMap) {
                for (Map.Entry<?, ?> e : enchMap.entrySet()) {
                    String enchKey = String.valueOf(e.getKey());
                    int lvl = e.getValue() instanceof Number n ? n.intValue() : 1;
                    template.getVanillaEnchants().put(enchKey, lvl);
                }
            }
        }

        // AdvancedEnchantments
        if (base.containsKey("advancedenchantments")) {
            Object aeObj = base.get("advancedenchantments");
            if (aeObj instanceof Map<?, ?> aeMap) {
                for (Map.Entry<?, ?> e : aeMap.entrySet()) {
                    String aeKey = String.valueOf(e.getKey());
                    int lvl = e.getValue() instanceof Number n ? n.intValue() : 1;
                    TakashaEnchantRegistry.CustomEnchantDef def = TakashaEnchantRegistry.get(aeKey);
                    int maxLvl = def != null ? def.maxLevel : 10;
                    template.getAdvancedEnchants().put(aeKey, Math.max(1, Math.min(maxLvl, lvl)));
                }
            }
        }

        // Gem Sockets
        if (base.containsKey("gem-sockets")) {
            Object gsObj = base.get("gem-sockets");
            if (gsObj instanceof List<?> gsList) {
                for (Object o : gsList) {
                    template.getGemSockets().add(String.valueOf(o));
                }
            }
        }

        // Gem Stone Attributes (MMOItems 1:1)
        if (base.containsKey("gem-color")) {
            template.setGemColor(String.valueOf(base.get("gem-color")));
        }
        if (base.containsKey("gem-upgrade-scaling")) {
            template.setGemUpgradeScaling(String.valueOf(base.get("gem-upgrade-scaling")).toUpperCase(Locale.ROOT));
        }
        if (base.containsKey("item-type-restriction")) {
            Object itrObj = base.get("item-type-restriction");
            if (itrObj instanceof List<?> itrList) {
                template.getItemTypeRestrictions().clear();
                for (Object o : itrList) {
                    template.getItemTypeRestrictions().add(String.valueOf(o).toUpperCase(Locale.ROOT));
                }
            } else if (itrObj instanceof String s) {
                template.getItemTypeRestrictions().clear();
                template.getItemTypeRestrictions().add(s.toUpperCase(Locale.ROOT));
            }
        }
        if (base.containsKey("success-rate")) {
            Object sr = base.get("success-rate");
            if (sr instanceof Number n) {
                template.setSuccessRate(n.doubleValue());
            } else if (sr instanceof Map<?, ?> sm && sm.get("base") instanceof Number bn) {
                template.setSuccessRate(bn.doubleValue());
            }
        }

        // Advanced & Cosmetic Attributes (MMOItems 1:1)
        if (base.containsKey("item-set") || base.containsKey("set")) {
            template.setItemSet(String.valueOf(base.containsKey("item-set") ? base.get("item-set") : base.get("set")));
        }
        if (base.containsKey("displayed-type")) {
            template.setDisplayedType(String.valueOf(base.get("displayed-type")));
        }
        if (base.containsKey("lore-format")) {
            template.setLoreFormat(String.valueOf(base.get("lore-format")));
        }
        if (base.containsKey("shield-pattern")) {
            template.setShieldPattern(String.valueOf(base.get("shield-pattern")));
        }
        if (base.containsKey("dye-color")) {
            template.setDyeColor(String.valueOf(base.get("dye-color")));
        }
        if (base.containsKey("trim-material")) {
            template.setTrimMaterial(String.valueOf(base.get("trim-material")));
        }
        if (base.containsKey("trim-pattern")) {
            template.setTrimPattern(String.valueOf(base.get("trim-pattern")));
        }
        if (base.containsKey("required-class")) {
            template.setRequiredClass(String.valueOf(base.get("required-class")));
        }
        if (base.containsKey("permission")) {
            template.setPermission(String.valueOf(base.get("permission")));
        }
        if (base.containsKey("two-handed")) {
            template.setTwoHanded(Boolean.parseBoolean(String.valueOf(base.get("two-handed"))));
        }
        if (base.containsKey("handworn")) {
            template.setHandworn(Boolean.parseBoolean(String.valueOf(base.get("handworn"))));
        }
        if (base.containsKey("hide-enchants")) {
            template.setHideEnchants(Boolean.parseBoolean(String.valueOf(base.get("hide-enchants"))));
        }
        if (base.containsKey("can-identify")) {
            template.setCanIdentify(Boolean.parseBoolean(String.valueOf(base.get("can-identify"))));
        }
        if (base.containsKey("can-deconstruct")) {
            template.setCanDeconstruct(Boolean.parseBoolean(String.valueOf(base.get("can-deconstruct"))));
        }
        if (base.containsKey("can-deskin")) {
            template.setCanDeskin(Boolean.parseBoolean(String.valueOf(base.get("can-deskin"))));
        }
        if (base.containsKey("knockback")) {
            try { template.setKnockback(Double.parseDouble(String.valueOf(base.get("knockback")))); } catch (Exception ignored) {}
        }
        if (base.containsKey("recoil")) {
            try { template.setRecoil(Double.parseDouble(String.valueOf(base.get("recoil")))); } catch (Exception ignored) {}
        }
        if (base.containsKey("commands") && base.get("commands") instanceof List<?> cmdList) {
            template.getCommands().clear();
            for (Object o : cmdList) template.getCommands().add(String.valueOf(o));
        }
        if (base.containsKey("perm-effects") && base.get("perm-effects") instanceof List<?> peList) {
            template.getPermEffects().clear();
            for (Object o : peList) template.getPermEffects().add(String.valueOf(o));
        }
        if (base.containsKey("potion-effects") && base.get("potion-effects") instanceof List<?> potList) {
            template.getPotionEffects().clear();
            for (Object o : potList) template.getPotionEffects().add(String.valueOf(o));
        }
        if (base.containsKey("custom-sounds") && base.get("custom-sounds") instanceof List<?> sndList) {
            template.getCustomSounds().clear();
            for (Object o : sndList) template.getCustomSounds().add(String.valueOf(o));
        }
        if (base.containsKey("item-particles")) {
            template.setItemParticles(String.valueOf(base.get("item-particles")));
        }
        if (base.containsKey("arrow-particles")) {
            template.setArrowParticles(String.valueOf(base.get("arrow-particles")));
        }

        // Abilities
        if (base.containsKey("ability")) {
            Object abObj = base.get("ability");
            if (abObj instanceof Map<?, ?> abMap) {
                for (Map.Entry<?, ?> entry : abMap.entrySet()) {
                    String abKey = String.valueOf(entry.getKey());
                    if (entry.getValue() instanceof Map<?, ?> details) {
                        String abType = details.containsKey("type") ? String.valueOf(details.get("type")) : abKey;
                        String modeStr = details.containsKey("mode") ? String.valueOf(details.get("mode")) : "RIGHT_CLICK";
                        MMOAbilityTrigger trigger = MMOAbilityTrigger.fromString(modeStr);

                        double cd = details.containsKey("cooldown") ? ((Number) details.get("cooldown")).doubleValue() : 0.0;
                        double skillCd = details.containsKey("skillcooldown") ? ((Number) details.get("skillcooldown")).doubleValue() : 0.0;
                        double timer = details.containsKey("timer") ? ((Number) details.get("timer")).doubleValue() : 0.0;
                        double dmg = details.containsKey("damage") ? ((Number) details.get("damage")).doubleValue() : 0.0;
                        double burnDur = details.containsKey("burnduration") ? ((Number) details.get("burnduration")).doubleValue() : 0.0;
                        double burnDmg = details.containsKey("burndamage") ? ((Number) details.get("burndamage")).doubleValue() : 0.0;
                        double mana = details.containsKey("mana") ? ((Number) details.get("mana")).doubleValue() : 0.0;

                        MMOItemTemplate.AbilityConfig abConfig = new MMOItemTemplate.AbilityConfig(
                                abKey, abType, trigger, cd, skillCd, timer, dmg, burnDur, burnDmg, mana
                        );

                        for (Map.Entry<?, ?> detailEntry : details.entrySet()) {
                            abConfig.getExtraParams().put(String.valueOf(detailEntry.getKey()), detailEntry.getValue());
                        }

                        template.getAbilities().add(abConfig);
                    }
                }
            }
        }

        registerTemplate(template);
    }

    public static ItemStack generateItemStack(String idOrKey) {
        MMOItemTemplate template = getTemplateById(idOrKey);
        if (template == null) {
            LOGGER.warn("Attempted to generate unknown MMOItem by id: {}", idOrKey);
            return ItemStack.EMPTY;
        }
        return generateItemStack(template.getItemType(), template.getId(), 1);
    }

    public static ItemStack generateItemStack(MMOItemTemplate template) {
        return generateItemStack(template, 1);
    }

    public static ItemStack generateItemStack(MMOItemTemplate template, int amount) {
        if (template == null) return ItemStack.EMPTY;
        registerTemplate(template);
        return generateItemStack(template.getItemType(), template.getId(), amount);
    }

    public static EquipmentSlotGroup getPrimarySlotGroup(String itemType) {
        if (itemType == null) return EquipmentSlotGroup.MAINHAND;
        return switch (itemType.toUpperCase(Locale.ROOT)) {
            case "HELMET" -> EquipmentSlotGroup.HEAD;
            case "CHESTPLATE" -> EquipmentSlotGroup.CHEST;
            case "LEGGINGS" -> EquipmentSlotGroup.LEGS;
            case "BOOTS" -> EquipmentSlotGroup.FEET;
            case "ARMOR" -> EquipmentSlotGroup.ARMOR;
            case "SHIELD", "OFF_CATALYST" -> EquipmentSlotGroup.OFFHAND;
            case "ACCESSORY", "ORNAMENT", "TALISMAN", "CATALYST" -> EquipmentSlotGroup.ANY;
            default -> EquipmentSlotGroup.MAINHAND;
        };
    }

    public static ItemStack generateItemStack(String type, String id, int amount) {
        MMOItemTemplate template = getTemplate(type, id);
        if (template == null) {
            template = getTemplateById(id);
        }
        if (template == null) {
            LOGGER.warn("Attempted to generate unknown MMOItem: {}:{}", type, id);
            return ItemStack.EMPTY;
        }

        String matName = template.getMaterial().toLowerCase(Locale.ROOT);
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(matName));
        if (item == Items.AIR) {
            item = switch (template.getItemType().toUpperCase(Locale.ROOT)) {
                case "TOOL", "PICKAXE" -> Items.NETHERITE_PICKAXE;
                case "AXE" -> Items.NETHERITE_AXE;
                case "SHOVEL" -> Items.NETHERITE_SHOVEL;
                default -> Items.NETHERITE_SWORD;
            };
        }

        ItemStack stack = new ItemStack(item, Math.max(1, amount));

        // Name
        if (!template.getName().isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, TakashaColorParser.parseItemName(template.getName()));
        }

        // Custom Model Data
        if (template.getCustomModelData() > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    List.of((float) template.getCustomModelData()),
                    List.of(),
                    List.of(),
                    List.of()
            ));
        }

        // Item Model
        String sanitizedId = template.getId().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (MMOItemManager.class.getResource("/assets/takasha_rpg/items/" + sanitizedId + ".json") != null) {
            stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("takasha_rpg", sanitizedId));
        }

        // Unbreakable
        if (template.isUnbreakable()) {
            stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        }

        // MMO Data Component
        MMOItemData mmoData = new MMOItemData(template.getItemType(), template.getId(), template.getTier(), template.getCustomModelData(), template.getName());
        mmoData.setRevisionId(template.getRevisionId());
        mmoData.getStats().putAll(template.getStats());
        if (template.getAttackDamage() > 0) {
            mmoData.setStat(MMOStatType.ATTACK_DAMAGE, template.getAttackDamage());
        }
        if (template.getAttackSpeed() > 0) {
            mmoData.setStat(MMOStatType.ATTACK_SPEED, template.getAttackSpeed());
        }
        mmoData.setAutosmelt(template.isAutosmelt());
        mmoData.setBouncingCrack(template.isBouncingCrack());
        mmoData.setPickaxePower(template.getPickaxePower());
        mmoData.setMiningEfficiency(template.getMiningEfficiency());
        mmoData.setMaxDurability(template.getMaxDurability());
        mmoData.setCurrentDurability(template.getMaxDurability());
        mmoData.setWillBreak(template.isWillBreak());

        for (MMOItemTemplate.AbilityConfig ab : template.getAbilities()) {
            mmoData.addAbility(new MMOAbilityReference(ab.getType(), ab.getMode(), ab.getCooldown(), ab.getManaCost()));
        }

        mmoData.getPermEffects().addAll(template.getPermEffects());
        mmoData.getPotionEffects().addAll(template.getPotionEffects());
        mmoData.getCommands().addAll(template.getCommands());

        mmoData.writeToStack(stack);

        // Attribute Modifiers (Physical attributes integrated with Minecraft & AuraSkills)
        ItemAttributeModifiers.Builder attrBuilder = ItemAttributeModifiers.builder();
        boolean hasCustomAttr = false;
        EquipmentSlotGroup primarySlot = getPrimarySlotGroup(template.getItemType());

        // 1. Attack Damage
        double attackDamage = template.getAttackDamage() > 0 ? template.getAttackDamage() : template.getStats().getOrDefault(MMOStatType.ATTACK_DAMAGE, 0.0);
        if (attackDamage > 0) {
            attrBuilder.add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "base_attack_damage"), attackDamage - 1.0, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 2. Attack Speed
        double attackSpeed = template.getAttackSpeed() > 0 ? template.getAttackSpeed() : template.getStats().getOrDefault(MMOStatType.ATTACK_SPEED, 4.0);
        if (attackSpeed > 0 && attackSpeed != 4.0) {
            attrBuilder.add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "base_attack_speed"), attackSpeed - 4.0, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 3. Max Health (Physical integration with Minecraft & AuraSkills)
        double maxHp = template.getStats().getOrDefault(MMOStatType.MAX_HEALTH, 0.0);
        if (maxHp > 0) {
            attrBuilder.add(
                    Attributes.MAX_HEALTH,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_max_health"), maxHp, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 4. Armor
        double armorVal = template.getStats().getOrDefault(MMOStatType.ARMOR, 0.0);
        if (armorVal > 0) {
            attrBuilder.add(
                    Attributes.ARMOR,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_armor"), armorVal, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 5. Armor Toughness
        double toughVal = template.getStats().getOrDefault(MMOStatType.ARMOR_TOUGHNESS, 0.0);
        if (toughVal > 0) {
            attrBuilder.add(
                    Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_armor_toughness"), toughVal, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 6. Knockback Resistance
        double kbRes = template.getStats().getOrDefault(MMOStatType.KNOCKBACK_RESISTANCE, 0.0);
        if (kbRes > 0) {
            double normalizedKb = kbRes > 1.0 ? kbRes / 100.0 : kbRes;
            attrBuilder.add(
                    Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_knockback_resistance"), normalizedKb, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 7. Movement Speed
        double speedVal = template.getStats().getOrDefault(MMOStatType.MOVEMENT_SPEED, 0.0);
        if (speedVal != 0.0) {
            double normalizedSpd = speedVal > 1.0 ? speedVal * 0.001 : (speedVal <= 0.05 && speedVal >= -0.05 ? speedVal : speedVal * 0.001);
            attrBuilder.add(
                    Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_movement_speed"), normalizedSpd, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 8. Attack Knockback
        if (template.getKnockback() > 0) {
            attrBuilder.add(
                    Attributes.ATTACK_KNOCKBACK,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_attack_knockback"), template.getKnockback(), AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 9. Luck
        double luckVal = template.getStats().getOrDefault(MMOStatType.LUCK, 0.0);
        if (luckVal > 0) {
            attrBuilder.add(
                    Attributes.LUCK,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_luck"), luckVal, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 10. Mining Efficiency
        double miningEff = template.getMiningEfficiency() > 0 ? template.getMiningEfficiency() : template.getStats().getOrDefault(MMOStatType.MINING_EFFICIENCY, 0.0);
        if (miningEff > 0) {
            attrBuilder.add(
                    Attributes.MINING_EFFICIENCY,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_mining_efficiency"), miningEff / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        // 11. Interaction Range (Reach)
        double rangeVal = template.getStats().getOrDefault(MMOStatType.RANGE, 0.0);
        if (rangeVal > 0) {
            attrBuilder.add(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_entity_range"), rangeVal, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            attrBuilder.add(
                    Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("takasha_rpg", "mmo_block_range"), rangeVal, AttributeModifier.Operation.ADD_VALUE),
                    primarySlot,
                    ItemAttributeModifiers.Display.hidden()
            );
            hasCustomAttr = true;
        }

        if (hasCustomAttr) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attrBuilder.build());
        }

        // Apply Vanilla Enchants
        if (!template.getVanillaEnchants().isEmpty()) {
            for (Map.Entry<String, Integer> e : template.getVanillaEnchants().entrySet()) {
                EnchantmentDetailHelper.applyEnchantmentData(stack, e.getKey().toUpperCase(Locale.ROOT) + ":" + e.getValue(), null);
            }
        }

        // Apply AdvancedEnchantments
        if (!template.getAdvancedEnchants().isEmpty()) {
            for (Map.Entry<String, Integer> e : template.getAdvancedEnchants().entrySet()) {
                TakashaEnchantRegistry.CustomEnchantDef def = TakashaEnchantRegistry.get(e.getKey());
                if (def != null) {
                    int cappedLvl = Math.max(1, Math.min(def.maxLevel, e.getValue()));
                    TakashaEnchantManager.applyCustomEnchant(stack, def, cappedLvl, true);
                }
            }
        }

        // Authentic 1:1 MMOItems Lore Formatting (stats.yml & lore-format.yml)
        List<Component> loreLines = new ArrayList<>();

        // #item-type#
        if (!template.getDisplayedType().isEmpty()) {
            loreLines.add(TakashaColorParser.parseLoreLine(template.getDisplayedType()));
        } else {
            loreLines.add(TakashaColorParser.parseLoreLine("&7" + template.getItemType().toUpperCase(Locale.ROOT)));
        }

        // Requirements (#required-level#, #required-mining#, etc.)
        boolean hasReq = false;
        if (template.getStats().containsKey(MMOStatType.REQUIRED_LEVEL)) {
            loreLines.add(Component.literal("§cRequires Total Level: §e" + (int) (double) template.getStats().get(MMOStatType.REQUIRED_LEVEL)));
            hasReq = true;
        }
        if (template.getStats().containsKey(MMOStatType.REQUIRED_MINING)) {
            loreLines.add(Component.literal("§cRequires Mining Level: §e" + (int) (double) template.getStats().get(MMOStatType.REQUIRED_MINING)));
            hasReq = true;
        }
        if (template.getStats().containsKey(MMOStatType.REQUIRED_FARMING)) {
            loreLines.add(Component.literal("§cRequires Farming Level: §e" + (int) (double) template.getStats().get(MMOStatType.REQUIRED_FARMING)));
            hasReq = true;
        }
        if (template.getStats().containsKey(MMOStatType.REQUIRED_WOODCUTTING)) {
            loreLines.add(Component.literal("§cRequires Woodcutting Level: §e" + (int) (double) template.getStats().get(MMOStatType.REQUIRED_WOODCUTTING)));
            hasReq = true;
        }
        if (template.getStats().containsKey(MMOStatType.REQUIRED_FIGHTING)) {
            loreLines.add(Component.literal("§cRequires Fighting Level: §e" + (int) (double) template.getStats().get(MMOStatType.REQUIRED_FIGHTING)));
            hasReq = true;
        }

        loreLines.add(Component.literal("§8§m---------------------"));

        // Combat Stats
        if (template.getAttackDamage() > 0) {
            loreLines.add(Component.literal("§7Attack Damage: §c+" + template.getAttackDamage()));
        }
        if (template.getAttackSpeed() > 0 && template.getAttackSpeed() != 4.0) {
            loreLines.add(Component.literal("§7Attack Speed: §e" + template.getAttackSpeed()));
        }

        // Tool Stats
        if (template.getPickaxePower() > 0) {
            loreLines.add(Component.literal("§6⛏ Pickaxe Power: §e+" + template.getPickaxePower()));
        }
        if (template.getMiningEfficiency() > 0) {
            loreLines.add(Component.literal("§e⚡ Mining Speed: §f+" + (int) template.getMiningEfficiency() + "%"));
        }
        if (template.isAutosmelt()) {
            loreLines.add(Component.literal("§6🔥 Auto-Smelt: §aEnabled"));
        }
        if (template.isBouncingCrack()) {
            loreLines.add(Component.literal("§e💥 Bouncing Crack: §aActive (3x3 Area)"));
        }

        // Other Stats
        for (Map.Entry<MMOStatType, Double> entry : template.getStats().entrySet()) {
            MMOStatType stat = entry.getKey();
            if (stat.name().startsWith("REQUIRED_")) continue;
            if (stat == MMOStatType.ATTACK_DAMAGE || stat == MMOStatType.ATTACK_SPEED) continue;
            double val = entry.getValue();
            loreLines.add(Component.literal(stat.getColor() + stat.getSymbol() + " " + stat.getDisplayName() + ": §f+" + (val == (int) val ? String.valueOf((int) val) : String.valueOf(val))));
        }

        // Abilities
        if (!template.getAbilities().isEmpty()) {
            loreLines.add(Component.empty());
            loreLines.add(Component.literal("§6§lAbilities:"));
            for (MMOItemTemplate.AbilityConfig ab : template.getAbilities()) {
                String trigText = switch (ab.getMode()) {
                    case RIGHT_CLICK -> "§e[Right Click]";
                    case SHIFT_RIGHT_CLICK -> "§c[Sneak + Right Click]";
                    case LEFT_CLICK -> "§b[Left Click]";
                    case SHIFT_LEFT_CLICK -> "§4[Sneak + Left Click]";
                    case SNEAK -> "§a[Sneak]";
                    case TIMER -> "§d[Passive]";
                    default -> "§7[" + ab.getMode().getDisplay() + "]";
                };
                String abilityName = ab.getType().replace('_', ' ');
                loreLines.add(Component.literal(" " + trigText + " §f" + abilityName));
                if (ab.getCooldown() > 0) {
                    loreLines.add(Component.literal("   §8⏳ Cooldown: §7" + ab.getCooldown() + "s"));
                }
                if (ab.getManaCost() > 0) {
                    loreLines.add(Component.literal("   §3💧 Mana Cost: §b" + (int) ab.getManaCost()));
                }
            }
        }

        // Extra weapon and armor mechanics
        if (template.isTwoHanded()) {
            loreLines.add(TakashaColorParser.parseLoreLine("&cTwo-Handed Weapon"));
        }
        if (template.hasRequiredClass()) {
            loreLines.add(TakashaColorParser.parseLoreLine("&cRequires Class: &e" + template.getRequiredClass()));
        }
        if (template.hasKnockback() && template.getKnockback() > 0) {
            loreLines.add(TakashaColorParser.parseLoreLine("&7Knockback: &f+" + template.getKnockback()));
        }
        if (template.hasRecoil() && template.getRecoil() > 0) {
            loreLines.add(TakashaColorParser.parseLoreLine("&7Recoil: &f" + template.getRecoil()));
        }

        // Enchantments Tooltip Display (Vanilla & Custom AdvancedEnchantments across ALL 40 types)
        Map<String, Integer> allCustomEnchants = new LinkedHashMap<>(template.getAdvancedEnchants());
        allCustomEnchants.putAll(TakashaEnchantManager.getCustomEnchants(stack));

        if (!allCustomEnchants.isEmpty() || !template.getVanillaEnchants().isEmpty()) {
            loreLines.add(Component.empty());
            // Vanilla Enchants
            if (!template.isHideEnchants() && !template.getVanillaEnchants().isEmpty()) {
                for (Map.Entry<String, Integer> e : template.getVanillaEnchants().entrySet()) {
                    EnchantmentDetailHelper.EnchantDetail det = EnchantmentDetailHelper.getDetail(e.getKey());
                    String name = det != null ? det.englishName() : e.getKey();
                    loreLines.add(Component.literal("§7" + name + " " + TakashaCustomBookItem.toRoman(e.getValue())));
                }
            }
            // Custom Enchantments
            for (Map.Entry<String, Integer> e : allCustomEnchants.entrySet()) {
                TakashaEnchantRegistry.CustomEnchantDef def = TakashaEnchantRegistry.get(e.getKey());
                if (def != null) {
                    int capped = Math.max(1, Math.min(def.maxLevel, e.getValue()));
                    loreLines.add(Component.literal(def.tier.getColorCode() + def.name + " " + TakashaCustomBookItem.toRoman(capped)));
                } else {
                    loreLines.add(Component.literal("§7" + e.getKey() + " " + TakashaCustomBookItem.toRoman(e.getValue())));
                }
            }
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        // Custom Lore description
        if (!template.getLore().isEmpty()) {
            loreLines.add(Component.empty());
            for (String l : template.getLore()) {
                loreLines.add(TakashaColorParser.parseLoreLine(l));
            }
        }

        loreLines.add(TakashaColorParser.parseLoreLine("&8&m---------------------"));

        // Custom Durability
        if (template.getMaxDurability() > 0) {
            loreLines.add(TakashaColorParser.parseLoreLine("&7Durability: &f" + template.getMaxDurability() + " / " + template.getMaxDurability()));
        }

        // Gem Sockets (MMOItems 1:1)
        if (!template.getGemSockets().isEmpty()) {
            loreLines.add(Component.empty());
            for (String socket : template.getGemSockets()) {
                loreLines.add(TakashaColorParser.parseLoreLine("&7Empty " + socket + " Socket"));
            }
        }

        // Equipment Set bonus footer
        if (template.hasItemSet()) {
            loreLines.add(Component.empty());
            loreLines.add(TakashaColorParser.parseLoreLine("&6Set: &e" + template.getItemSet()));
        }

        // Tier footer
        if (template.getTier() != null) {
            loreLines.add(TakashaColorParser.parseLoreLine(template.getTier().getPrefixColor() + "&l✦ " + template.getTier().getDisplayName().toUpperCase(Locale.ROOT) + " " + template.getItemType().toUpperCase(Locale.ROOT) + " ✦"));
        }

        stack.set(DataComponents.LORE, new ItemLore(loreLines));

        return stack;
    }

    public static boolean updateItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Optional<MMOItemData> opt = MMOItemData.readFromStack(stack);
        if (opt.isEmpty()) return false;
        MMOItemData currentData = opt.get();
        MMOItemTemplate template = getTemplate(currentData.getType(), currentData.getId());
        if (template == null) {
            template = getTemplateById(currentData.getId());
        }
        if (template == null) return false;

        int curDurability = currentData.getCurrentDurability();
        ItemStack fresh = generateItemStack(template.getItemType(), template.getId(), stack.getCount());
        if (fresh.isEmpty()) return false;

        Optional<MMOItemData> freshDataOpt = MMOItemData.readFromStack(fresh);
        if (freshDataOpt.isPresent() && curDurability > 0) {
            MMOItemData freshData = freshDataOpt.get();
            freshData.setCurrentDurability(Math.min(curDurability, freshData.getMaxDurability()));
            freshData.writeToStack(fresh);
        }

        stack.applyComponents(fresh.getComponents());
        return true;
    }
}
