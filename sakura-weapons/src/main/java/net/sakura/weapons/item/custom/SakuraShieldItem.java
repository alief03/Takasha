package net.sakura.weapons.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.List;
import java.util.Optional;

public class SakuraShieldItem extends ShieldItem {

    public SakuraShieldItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * Constructs properties for custom shields in Minecraft 1.21.11 / Fabric.
     * In 1.21.11, blocking behavior requires DataComponents.BLOCKS_ATTACKS
     * and equippableUnswappable(EquipmentSlot.OFFHAND).
     */
    public static Item.Properties createShieldProperties(int durability) {
        return new Item.Properties()
            .durability(durability)
            .repairable(ItemTags.WOODEN_TOOL_MATERIALS)
            .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
            .equippableUnswappable(EquipmentSlot.OFFHAND)
            .delayedComponent(DataComponents.BLOCKS_ATTACKS, provider -> new BlocksAttacks(
                0.25f,
                1.0f,
                List.of(new BlocksAttacks.DamageReduction(90.0f, Optional.empty(), 0.0f, 1.0f)),
                new BlocksAttacks.ItemDamageFunction(3.0f, 1.0f, 1.0f),
                Optional.of(provider.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)
            ))
            .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }
}
