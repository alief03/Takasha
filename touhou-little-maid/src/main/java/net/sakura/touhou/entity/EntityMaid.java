package net.sakura.touhou.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.sakura.touhou.compat.enchant.MaidEnchantBridge;
import net.sakura.touhou.compat.rpg.MaidRpgBondingBridge;
import net.sakura.touhou.entity.task.MaidTaskMode;

/**
 * EntityMaid
 * Entitas pendamping maid untuk Minecraft 26.2 dengan dukungan sinkronisasi atribut Takasha-RPG,
 * Custom Enchantments, inventory 36-slot terpadu, dan rendering Bedrock 3D.
 */
public class EntityMaid extends TamableAnimal {

    public static final EntityDataAccessor<String> DATA_MODEL_ID =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> DATA_TASK_MODE =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_FAVORABILITY =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_IS_SITTING =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_YSM_MODEL =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> DATA_YSM_MODEL_ID =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> DATA_YSM_MODEL_TEXTURE =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> DATA_YSM_MODEL_NAME =
            SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);

    private final SimpleContainer inventory = new SimpleContainer(36);
    private String activeSetResonance = null;
    private int syncCooldown = 0;
    private int outOfCombatTicks = 0;
    private String currentRouletteAnim = "";
    private boolean isRouletteAnimPlaying = false;

    public EntityMaid(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createMaidAttributes() {
        return TamableAnimal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0)
                .add(Attributes.LUCK, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODEL_ID, "touhou_little_maid:hakurei_reimu");
        builder.define(DATA_TASK_MODE, MaidTaskMode.FOLLOW.ordinal());
        builder.define(DATA_FAVORABILITY, 0);
        builder.define(DATA_IS_SITTING, false);
        builder.define(DATA_IS_YSM_MODEL, false);
        builder.define(DATA_YSM_MODEL_ID, "");
        builder.define(DATA_YSM_MODEL_TEXTURE, "");
        builder.define(DATA_YSM_MODEL_NAME, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(5, new net.sakura.touhou.compat.tlmo.task.MaidTreeTask(this));
        this.goalSelector.addGoal(5, new net.sakura.touhou.compat.tlmo.task.MaidMineTask(this));
        this.goalSelector.addGoal(5, new net.sakura.touhou.compat.tlmo.task.HandCrankTask(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Target Selectors: Serang monster yang menyerang pemilik atau yang diserang pemilik
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            syncCooldown++;
            outOfCombatTicks++;

            // Sinkronisasi atribut berkala (tiap 40 tick / 2 detik)
            if (syncCooldown >= 40) {
                syncCooldown = 0;
                MaidRpgBondingBridge.syncOwnerStats(this);
            }

            // Regenerasi alami di luar combat (setiap 60 tick saat tenang)
            if (outOfCombatTicks >= 100 && this.getHealth() < this.getMaxHealth()) {
                if (this.tickCount % 40 == 0) {
                    this.heal(1.0f);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.2, this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                    }
                }
            }

            // Tick aura Mobile Beacon jika terpasang
            net.sakura.touhou.compat.tlmo.beacon.BeaconEffectGeneratorBauble.tickBeaconAura(this);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            // Friendly Fire Guard
            if (MaidEnchantBridge.isFriendly(this, target)) {
                return false;
            }

            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            DamageSource source = this.damageSources().mobAttack(this);

            boolean success = target.hurtServer(level, source, damage);
            if (success) {
                outOfCombatTicks = 0;
                // Eksekusi custom enchantments & resonance aura
                MaidEnchantBridge.onMeleeAttack(this, livingTarget, source, damage);
            }
            return success;
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Friendly Fire Guard: Cegah damage dari pemilik atau sesama pet
        if (source.getEntity() != null && MaidEnchantBridge.isFriendly(this, source.getEntity())) {
            return false;
        }

        LivingEntity attacker = (source.getEntity() instanceof LivingEntity le) ? le : null;

        // Custom Enchantment Defense & Dodge check
        boolean allow = MaidEnchantBridge.onHurt(this, attacker, source, amount);
        if (!allow) {
            return false; // Serangan dihindari
        }

        outOfCombatTicks = 0;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide() && this.getOwner() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal("§c[Maid] §f" + this.getCustomName() + " §7telah gugur dalam pertempuran!"));
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // 1. Taming Protocol
        if (!this.isTame()) {
            if (itemStack.is(Items.CAKE) || itemStack.is(Items.COOKIE) || itemStack.is(Items.APPLE) || itemStack.is(Items.GOLDEN_APPLE)) {
                if (!player.getAbilities().instabuild) {
                    itemStack.shrink(1);
                }
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.setMaidSitting(true);
                    this.level().broadcastEntityEvent(this, (byte) 7); // Heart particles
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
                    player.sendSystemMessage(Component.literal("§d❤ §fMaid berhasil dijinakkan! Atribut RPG sekarang tersinkronisasi."));
                    MaidRpgBondingBridge.syncOwnerStats(this);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6); // Smoke particles
                }
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }

        // 2. Owner Interaction
        if (this.isOwnedBy(player)) {
            // Shift + Klik Kanan dengan tangan kosong: Petting / Affection / Kiss
            if (player.isShiftKeyDown() && itemStack.isEmpty()) {
                return net.sakura.touhou.compat.tlmo.affection.KissMaidHandler.handleKiss(this, player);
            }

            // Shift + Klik Kanan dengan item lain: Tampilkan status ringkas
            if (player.isShiftKeyDown()) {
                if (!this.level().isClientSide()) {
                    player.sendSystemMessage(Component.literal("§6[Touhou Maid] §fMode: §e" + getTaskMode().getDisplayName() +
                            " §7| §c❤ HP: §f" + (int) this.getHealth() + "/" + (int) this.getMaxHealth() +
                            " §7| §dFavor: §f" + getFavorability() +
                            (isYsmModel() ? " §7| §bYSM: §f" + getYsmModelId() : "")));
                }
                return InteractionResult.SUCCESS;
            }

            // Memberi Makan Pemulihan HP
            if (itemStack.is(Items.SUGAR) || itemStack.is(Items.SWEET_BERRIES) || itemStack.is(Items.APPLE)) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(3.0f);
                    if (!player.getAbilities().instabuild) itemStack.shrink(1);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.5f, 1.2f);
                    return InteractionResult.SUCCESS;
                }
            }

            // Normal Klik Kanan: Buka GUI Maid Utama (Inventaris, Bauble, Model YSM, Task)
            if (!this.level().isClientSide()) {
                openMaidGui(player);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    public boolean openMaidGui(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<Integer>() {
                @Override
                public Integer getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
                    return getId();
                }

                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inv, Player p) {
                    return new net.sakura.touhou.inventory.MaidMainContainer(syncId, inv, getId());
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null; // Maid tidak berkembang biak
    }

    // Getters & Setters
    public String getModelId() {
        return this.entityData.get(DATA_MODEL_ID);
    }

    public void setModelId(String modelId) {
        this.entityData.set(DATA_MODEL_ID, modelId != null ? modelId : "touhou_little_maid:hakurei_reimu");
    }

    public MaidTaskMode getTaskMode() {
        int ord = this.entityData.get(DATA_TASK_MODE);
        MaidTaskMode[] values = MaidTaskMode.values();
        return (ord >= 0 && ord < values.length) ? values[ord] : MaidTaskMode.FOLLOW;
    }

    public void setTaskMode(MaidTaskMode mode) {
        this.entityData.set(DATA_TASK_MODE, mode != null ? mode.ordinal() : MaidTaskMode.FOLLOW.ordinal());
    }

    public int getFavorability() {
        return this.entityData.get(DATA_FAVORABILITY);
    }

    public void setFavorability(int favorability) {
        this.entityData.set(DATA_FAVORABILITY, Math.max(0, favorability));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(net.sakura.touhou.registry.ModItems.CHERRY_BLOSSOM_CAKE);
    }

    public boolean isSitting() {
        return this.entityData.get(DATA_IS_SITTING) || this.isInSittingPose();
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(DATA_IS_SITTING, sitting);
        this.setInSittingPose(sitting);
    }

    public boolean isMaidSitting() {
        return isSitting();
    }

    public void setMaidSitting(boolean sitting) {
        setSitting(sitting);
    }

    public boolean isBegging() {
        return this.getFavorability() >= 80;
    }

    public String getActiveSetResonance() {
        return activeSetResonance;
    }

    public void setActiveSetResonance(String activeSetResonance) {
        this.activeSetResonance = activeSetResonance;
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    // Persistence (26.2 ValueOutput & ValueInput)
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("ModelId", this.getModelId());
        output.putInt("TaskMode", this.getTaskMode().ordinal());
        output.putInt("Favorability", this.getFavorability());
        output.putBoolean("IsSitting", this.isSitting());
        output.putBoolean("IsYsmModel", this.isYsmModel());
        output.putString("YsmModelId", this.getYsmModelId());
        output.putString("YsmModelTexture", this.getYsmModelTexture());
        output.putString("YsmModelName", this.getYsmModelName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setModelId(input.getStringOr("ModelId", "touhou_little_maid:hakurei_reimu"));
        int taskOrdinal = input.getIntOr("TaskMode", 0);
        MaidTaskMode[] modes = MaidTaskMode.values();
        this.setTaskMode(modes[Math.max(0, Math.min(taskOrdinal, modes.length - 1))]);
        this.setFavorability(input.getIntOr("Favorability", 0));
        this.setSitting(input.getBooleanOr("IsSitting", false));
        this.setIsYsmModel(input.getBooleanOr("IsYsmModel", false));
        this.setYsmModel(
                input.getStringOr("YsmModelId", ""),
                input.getStringOr("YsmModelTexture", ""),
                Component.literal(input.getStringOr("YsmModelName", ""))
        );
    }

    // YSM Handlers
    public boolean isYsmModel() {
        return this.entityData.get(DATA_IS_YSM_MODEL);
    }

    public void setIsYsmModel(boolean isYsmModel) {
        this.entityData.set(DATA_IS_YSM_MODEL, isYsmModel);
    }

    public String getYsmModelId() {
        return this.entityData.get(DATA_YSM_MODEL_ID);
    }

    public String getYsmModelTexture() {
        return this.entityData.get(DATA_YSM_MODEL_TEXTURE);
    }

    public String getYsmModelName() {
        return this.entityData.get(DATA_YSM_MODEL_NAME);
    }

    public void setYsmModel(String modelId, String texture, Component name) {
        this.entityData.set(DATA_YSM_MODEL_ID, modelId != null ? modelId : "");
        this.entityData.set(DATA_YSM_MODEL_TEXTURE, texture != null ? texture : "");
        this.entityData.set(DATA_YSM_MODEL_NAME, name != null ? name.getString() : "");
    }

    public void playRouletteAnim(String anim) {
        this.currentRouletteAnim = anim != null ? anim : "";
        this.isRouletteAnimPlaying = true;
    }

    public void stopRouletteAnim() {
        this.isRouletteAnimPlaying = false;
    }

    public boolean isRouletteAnimPlaying() {
        return isRouletteAnimPlaying;
    }

    public String getCurrentRouletteAnim() {
        return currentRouletteAnim;
    }
}
