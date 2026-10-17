package net.sakura.weapons.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.sakura.weapons.client.gui.TakashaMaidSubScreen;
import net.sakura.weapons.maid.model.MaidModelRegistry;

public class TakashaMaidEntity extends TamableAnimal {

    public static final EntityDataAccessor<String> DATA_MODEL_ID =
            SynchedEntityData.defineId(TakashaMaidEntity.class, EntityDataSerializers.STRING);

    public static final EntityDataAccessor<Integer> DATA_MAID_TASK =
            SynchedEntityData.defineId(TakashaMaidEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Boolean> DATA_IS_BEGGING =
            SynchedEntityData.defineId(TakashaMaidEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<BlockPos> DATA_HOME_POS =
            SynchedEntityData.defineId(TakashaMaidEntity.class, EntityDataSerializers.BLOCK_POS);

    // Tasks: 0 = IDLE/FOLLOW, 1 = FARMING, 2 = FISHING
    public static final int TASK_FOLLOW = 0;
    public static final int TASK_FARM = 1;
    public static final int TASK_FISH = 2;

    private final SimpleContainer inventory = new SimpleContainer(36);
    private int fishTimer = 0;
    private int farmScanCooldown = 0;

    public TakashaMaidEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODEL_ID, MaidModelRegistry.DEFAULT_MODEL_ID);
        builder.define(DATA_MAID_TASK, TASK_FOLLOW);
        builder.define(DATA_IS_BEGGING, false);
        builder.define(DATA_HOME_POS, BlockPos.ZERO);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F) {
            @Override
            public boolean canUse() {
                return super.canUse() && getMaidTask() == TASK_FOLLOW;
            }
        });
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return super.canUse() && !isOrderedToSit() && getMaidTask() != TASK_FISH;
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            LivingEntity owner = this.getOwner();

            // Begging check: if owner holds food/cake nearby
            if (owner instanceof Player player) {
                ItemStack mainHand = player.getMainHandItem();
                boolean holdsTreat = isTreatItem(mainHand);
                if (this.distanceToSqr(player) < 16.0 && holdsTreat) {
                    this.entityData.set(DATA_IS_BEGGING, true);
                    this.getLookControl().setLookAt(player, 10.0F, (float) this.getMaxHeadXRot());
                } else {
                    this.entityData.set(DATA_IS_BEGGING, false);
                }
            }

            // Task execution in singleworld
            if (!this.isOrderedToSit()) {
                if (getMaidTask() == TASK_FARM) {
                    tickFarmTask();
                } else if (getMaidTask() == TASK_FISH) {
                    tickFishTask();
                }
            }
        }
    }

    private void tickFarmTask() {
        if (--farmScanCooldown > 0) return;
        farmScanCooldown = 40; // scan every 2 seconds

        BlockPos center = getHomePos().equals(BlockPos.ZERO) ? this.blockPosition() : getHomePos();
        int radius = 8;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos target = center.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(target);

                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                        // Harvest crop
                        this.level().destroyBlock(target, true, this);
                        // Replant
                        BlockState defaultCrop = crop.getStateForAge(0);
                        this.level().setBlock(target, defaultCrop, 3);
                        this.swing(InteractionHand.MAIN_HAND);
                        this.playSound(SoundEvents.CROP_PLANTED, 1.0F, 1.0F);
                        return;
                    } else if (state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
                        this.level().destroyBlock(target, true, this);
                        this.level().setBlock(target, Blocks.NETHER_WART.defaultBlockState(), 3);
                        this.swing(InteractionHand.MAIN_HAND);
                        this.playSound(SoundEvents.NETHER_WART_PLANTED, 1.0F, 1.0F);
                        return;
                    }
                }
            }
        }
    }

    private void tickFishTask() {
        // Equip fishing rod in main hand if available
        if (!this.getMainHandItem().is(Items.FISHING_ROD)) {
            for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                ItemStack stack = this.inventory.getItem(i);
                if (stack.is(Items.FISHING_ROD)) {
                    ItemStack old = this.getMainHandItem();
                    this.setItemSlot(EquipmentSlot.MAINHAND, stack);
                    this.inventory.setItem(i, old);
                    break;
                }
            }
        }

        if (++fishTimer >= 140) { // catch every 7 seconds
            fishTimer = 0;
            this.swing(InteractionHand.MAIN_HAND);
            this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.5F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);

            // Generate catch
            ItemStack catchItem = generateFishingLoot();
            if (!catchItem.isEmpty()) {
                ItemStack remainder = this.inventory.addItem(catchItem);
                if (!remainder.isEmpty()) {
                    this.spawnAtLocation((ServerLevel) this.level(), remainder);
                }
            }
        }
    }

    private ItemStack generateFishingLoot() {
        float roll = this.random.nextFloat();
        if (roll < 0.60f) {
            // Raw Cod / Salmon
            return this.random.nextBoolean() ? new ItemStack(Items.COD) : new ItemStack(Items.SALMON);
        } else if (roll < 0.75f) {
            // Tropical Fish / Pufferfish
            return this.random.nextBoolean() ? new ItemStack(Items.TROPICAL_FISH) : new ItemStack(Items.PUFFERFISH);
        } else if (roll < 0.90f) {
            // Treasures: Lily Pad, Bamboo, Nautilus Shell
            float tRoll = this.random.nextFloat();
            if (tRoll < 0.4f) return new ItemStack(Items.LILY_PAD);
            if (tRoll < 0.7f) return new ItemStack(Items.BAMBOO);
            return new ItemStack(Items.NAUTILUS_SHELL);
        } else {
            // Junk: String, Leather, Stick
            return new ItemStack(Items.STRING);
        }
    }

    private boolean isTreatItem(ItemStack stack) {
        return stack.is(Items.CAKE) || stack.is(Items.COOKIE) || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.PUMPKIN_PIE) || stack.is(Items.APPLE) || stack.is(Items.HONEY_BOTTLE);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isTreatItem(stack);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (!this.isTame()) {
            if (isTreatItem(held)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(false);
                    this.setInSittingPose(false);
                    this.spawnTamingParticles(true);
                    this.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.2F);
                    player.sendSystemMessage(Component.literal("§d[Takasha Maid] §f♥ Aku sekarang adalah pelayan setiamu, Tuan! (Tekan Shift + Klik Kanan untuk pengaturan)"));
                } else {
                    this.spawnTamingParticles(false);
                    this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }

        if (this.isOwnedBy(player)) {
            // Shift + Right Click: open settings/model gallery subscreen
            if (player.isShiftKeyDown()) {
                if (this.level().isClientSide()) {
                    openClientScreen();
                }
                return InteractionResult.SUCCESS;
            }

            // Normal Right Click: Toggle Sit / Stand / Follow
            if (isTreatItem(held) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                this.heal(6.0F);
                this.spawnTamingParticles(true);
                this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }

            boolean newSit = !this.isOrderedToSit();
            this.setOrderedToSit(newSit);
            this.setInSittingPose(newSit);
            this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
            player.sendOverlayMessage(Component.literal(newSit ? "§d[Maid] §fMode: §eDuduk / Istirahat" : "§d[Maid] §fMode: §aMengikuti Tuan"));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private void openClientScreen() {
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(new TakashaMaidSubScreen(this));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (damageSource.getEntity() instanceof Player player && this.isOwnedBy(player)) {
            return false; // Prevent friendly fire from owner
        }
        return super.hurtServer(level, damageSource, amount);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("ModelId", getModelId());
        output.putInt("MaidTask", getMaidTask());
        BlockPos home = getHomePos();
        output.putInt("HomeX", home.getX());
        output.putInt("HomeY", home.getY());
        output.putInt("HomeZ", home.getZ());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setModelId(input.getStringOr("ModelId", MaidModelRegistry.DEFAULT_MODEL_ID));
        setMaidTask(input.getIntOr("MaidTask", TASK_FOLLOW));
        int hx = input.getIntOr("HomeX", 0);
        int hy = input.getIntOr("HomeY", 0);
        int hz = input.getIntOr("HomeZ", 0);
        setHomePos(new BlockPos(hx, hy, hz));
    }

    // Getters and setters
    public String getModelId() {
        return this.entityData.get(DATA_MODEL_ID);
    }

    public void setModelId(String modelId) {
        this.entityData.set(DATA_MODEL_ID, modelId != null ? modelId : MaidModelRegistry.DEFAULT_MODEL_ID);
    }

    public int getMaidTask() {
        return this.entityData.get(DATA_MAID_TASK);
    }

    public void setMaidTask(int task) {
        this.entityData.set(DATA_MAID_TASK, task);
    }

    public boolean isBegging() {
        return this.entityData.get(DATA_IS_BEGGING);
    }

    public BlockPos getHomePos() {
        return this.entityData.get(DATA_HOME_POS);
    }

    public void setHomePos(BlockPos pos) {
        this.entityData.set(DATA_HOME_POS, pos != null ? pos : BlockPos.ZERO);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }
}
