package net.sakura.weapons.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.registry.ModItems;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.component.ResolvableProfile;
import net.sakura.weapons.entity.npc.NpcPushPermission;

/**
 * Takasha Custom Humanoid NPC & Display Mannequin Entity for Minecraft 26.2.
 * Supports:
 * - Dynamic asynchronous skin loading via URL or Player Name
 * - Multi-axis limb posing & 7 special lying down / sleeping variations
 * - Replay Mod zero-desync recording (all visual properties in SynchedEntityData)
 * - Multiplayer dedicated server anti-conflict protections:
 *   * Strict ownership & tiered permissions (Private, Protected View, Co-op, Public)
 *   * Single-editor session locking to prevent duplication exploits
 *   * Immobile physics (anti-push by boats, minecarts, fishing rods, pistons, fluids)
 *   * Safe clean dismantle with zero item loss
 *   * PvE hostile mob neutrality
 */
public class TakashaNpcEntity extends Avatar {

    // Global tracker for active editing sessions to ensure graceful release on player disconnect
    private static final Map<UUID, TakashaNpcEntity> ACTIVE_EDITORS = new ConcurrentHashMap<>();

    // Permission mode constants
    public static final byte PERM_PRIVATE = 0;        // Only owner & server OP
    public static final byte PERM_PROTECTED_VIEW = 1; // View-only for public, edit for owner
    public static final byte PERM_COOP_TEAM = 2;      // Owner, team members & OP
    public static final byte PERM_PUBLIC_EDIT = 3;    // Sandbox / Creative free edit

    // Tracked Synched Data Accessors (Recorded natively by Replay Mod into .mcpr files)
    public static final EntityDataAccessor<String> DATA_SKIN_URL =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> DATA_SKIN_MODEL =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.STRING);

    public static final EntityDataAccessor<Rotations> DATA_HEAD_POSE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_BODY_POSE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_ARM_POSE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_ARM_POSE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_LEG_POSE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_LEG_POSE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.ROTATIONS);

    public static final EntityDataAccessor<Integer> DATA_POSE_PRESET =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_BED_HEIGHT_OFFSET =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> DATA_EMOTE_ID =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.STRING);
    public static final byte EMOTE_MODE_LOOP = 0;
    public static final byte EMOTE_MODE_HOLD = 1;
    public static final byte EMOTE_MODE_STAND = 2;
    public static final EntityDataAccessor<Byte> DATA_EMOTE_PLAY_MODE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Float> DATA_YAW_ROTATION =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> DATA_IS_SMALL =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_LOCKED =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_SHOW_NAME =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Byte> DATA_PERMISSION_MODE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<ItemStack> DATA_WINGS_ITEM =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.ITEM_STACK);

    // Push Physics Data Accessors
    public static final EntityDataAccessor<Boolean> DATA_PUSHABLE =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_PUSH_STRENGTH =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DATA_PUSH_PERMISSION =
        SynchedEntityData.defineId(TakashaNpcEntity.class, EntityDataSerializers.INT);

    // Origin Anchor Coordinates
    private double originX;
    private double originY;
    private double originZ;
    private float originYaw;

    // Ownership fields
    private Optional<UUID> ownerUUID = Optional.empty();
    private String ownerName = "";

    // Single-editor session lock fields
    private UUID currentEditingPlayer = null;
    private String currentEditingPlayerName = "";
    private long editingLockTimestamp = 0L;

    // 7-Slot Persistent Inventory (0..3: Armor, 4: MainHand, 5: OffHand, 6: Wings)
    private boolean isUpdatingInventory = false;
    private final SimpleContainer inventory = new SimpleContainer(7) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!isUpdatingInventory) {
                syncEquipmentFromInventory();
            }
        }
    };

    public TakashaNpcEntity(EntityType<? extends TakashaNpcEntity> entityType, Level level) {
        super(entityType, level);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.originX = this.getX();
        this.originY = this.getY();
        this.originZ = this.getZ();
        this.originYaw = this.getYRot();
    }

    @Override
    public ResolvableProfile getProfile() {
        return ResolvableProfile.createResolved(new GameProfile(this.getUUID(), this.getDisplayName().getString()));
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN_URL, "");
        builder.define(DATA_SKIN_MODEL, "default"); // "default" (Steve) or "slim" (Alex)
        builder.define(DATA_HEAD_POSE, new Rotations(-1.5f, 2.0f, 0f));
        builder.define(DATA_BODY_POSE, new Rotations(1.0f, 0f, 0f));
        builder.define(DATA_LEFT_ARM_POSE, new Rotations(-1.0f, 0f, -3.5f));
        builder.define(DATA_RIGHT_ARM_POSE, new Rotations(2.0f, 0f, 3.5f));
        builder.define(DATA_LEFT_LEG_POSE, new Rotations(-1.5f, -2.0f, -1.0f));
        builder.define(DATA_RIGHT_LEG_POSE, new Rotations(1.5f, 2.0f, 1.0f));
        builder.define(DATA_POSE_PRESET, 0);
        builder.define(DATA_BED_HEIGHT_OFFSET, 0.0f);
        builder.define(DATA_EMOTE_ID, "");
        builder.define(DATA_EMOTE_PLAY_MODE, EMOTE_MODE_LOOP);
        builder.define(DATA_YAW_ROTATION, 0.0f);
        builder.define(DATA_IS_SMALL, false);
        builder.define(DATA_IS_LOCKED, false);
        builder.define(DATA_SHOW_NAME, true);
        builder.define(DATA_PERMISSION_MODE, PERM_PRIVATE);
        builder.define(DATA_WINGS_ITEM, ItemStack.EMPTY);
        builder.define(DATA_PUSHABLE, true);         // Default: BISA DIDORONG (PUSHABLE)
        builder.define(DATA_PUSH_STRENGTH, 0.35f);   // Default: Sedang (0.35x)
        builder.define(DATA_PUSH_PERMISSION, 2);     // Default: SEMUA ORANG (EVERYONE)
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_POSE_PRESET.equals(key)) {
            int preset = this.entityData.get(DATA_POSE_PRESET);
            applyEntityPoseForPreset(preset);
            if (preset > 0 && preset <= 32) {
                applyPosePreset(preset);
            }
        } else if (DATA_YAW_ROTATION.equals(key)) {
            float yaw = this.entityData.get(DATA_YAW_ROTATION);
            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.setYBodyRot(yaw);
        } else if (DATA_IS_SMALL.equals(key)) {
            this.refreshDimensions();
        } else if (DATA_PUSHABLE.equals(key)) {
            boolean pushable = this.entityData.get(DATA_PUSHABLE);
            this.setNoGravity(!pushable);
            if (!pushable) {
                this.setDeltaMovement(Vec3.ZERO);
            }
        }

        if (this.level().isClientSide()) {
            if (DATA_EMOTE_ID.equals(key) || DATA_EMOTE_PLAY_MODE.equals(key)) {
                this.animErrorCount = 0;
                this.animCooldownTicks = 0;
                this.standEmotePlayed = false;
                String emote = getEmoteId();
                if (emote != null && !emote.isBlank()) {
                    net.sakura.weapons.compat.EmotecraftCompat.playEmoteSafely(this, emote, getEmotePlayMode());
                } else {
                    net.sakura.weapons.compat.EmotecraftCompat.stopEmoteSafely(this);
                    net.sakura.weapons.compat.EmotecraftCompat.resetAnimStateSafely(this);
                }
            } else if (DATA_SKIN_URL.equals(key)) {
                String skinUrl = getSkinUrl();
                if (skinUrl != null && !skinUrl.isBlank()) {
                    net.sakura.weapons.client.skin.NpcSkinManager.fetchSkinAsync(skinUrl);
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Multiplayer Ownership & Permissions
    // -----------------------------------------------------------------------------------------

    public Optional<UUID> getOwnerUUID() {
        return this.ownerUUID;
    }

    public void setOwner(Player player) {
        if (player != null) {
            this.ownerUUID = Optional.of(player.getUUID());
            this.ownerName = player.getName().getString();
        }
    }

    public void setOwnerData(Optional<UUID> uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name != null ? name : "";
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public byte getPermissionMode() {
        return this.entityData.get(DATA_PERMISSION_MODE);
    }

    public void setPermissionMode(byte mode) {
        this.entityData.set(DATA_PERMISSION_MODE, mode);
    }

    public boolean isOwner(Player player) {
        return player != null && this.ownerUUID.isPresent() && this.ownerUUID.get().equals(player.getUUID());
    }

    public boolean isWhitelisted(Player player) {
        if (player == null) return false;
        if (getPermissionMode() == PERM_COOP_TEAM && player.getTeam() != null) {
            return true;
        }
        return false;
    }

    public boolean isPushableMode() {
        return this.entityData.get(DATA_PUSHABLE);
    }

    public void setPushableMode(boolean pushable) {
        this.entityData.set(DATA_PUSHABLE, pushable);
        this.setNoGravity(!pushable);
        if (!pushable) {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    public float getPushStrength() {
        return this.entityData.get(DATA_PUSH_STRENGTH);
    }

    public void setPushStrength(float strength) {
        this.entityData.set(DATA_PUSH_STRENGTH, Math.max(0.05f, Math.min(1.0f, strength)));
    }

    public int getPushPermissionId() {
        return this.entityData.get(DATA_PUSH_PERMISSION);
    }

    public void setPushPermissionId(int permissionId) {
        this.entityData.set(DATA_PUSH_PERMISSION, permissionId);
    }

    public NpcPushPermission getPushPermission() {
        return NpcPushPermission.fromId(getPushPermissionId());
    }

    public void setPushPermission(NpcPushPermission permission) {
        setPushPermissionId(permission != null ? permission.getId() : 0);
    }

    public double getOriginX() { return originX; }
    public double getOriginY() { return originY; }
    public double getOriginZ() { return originZ; }
    public float getOriginYaw() { return originYaw; }

    public void resetToOriginPosition() {
        this.snapTo(this.originX, this.originY, this.originZ, this.originYaw, 0.0f);
        this.setDeltaMovement(Vec3.ZERO);
        this.hurtMarked = true;
    }

    public void lockCurrentAsNewOrigin() {
        this.originX = this.getX();
        this.originY = this.getY();
        this.originZ = this.getZ();
        this.originYaw = this.getYRot();
    }

    public boolean canPlayerManage(Player player) {
        if (player == null) return false;
        // Creative mode players and server operators always have full administrative access
        if (player.isCreative()) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.level().getServer() != null) {
            if (serverPlayer.level().getServer().getPlayerList().isOp(serverPlayer.nameAndId())) {
                return true;
            }
        }
        // Unclaimed statue can be managed by anyone
        if (this.ownerUUID.isEmpty()) {
            return true;
        }
        // Owner has full access
        if (this.ownerUUID.get().equals(player.getUUID())) {
            return true;
        }
        // Public edit mode
        if (getPermissionMode() == PERM_PUBLIC_EDIT) {
            return true;
        }
        // Co-op team mode: check if players share the same team
        if (getPermissionMode() == PERM_COOP_TEAM && player.getTeam() != null) {
            return player.getTeam().isAlliedTo(player.getTeam());
        }
        return false;
    }

    // -----------------------------------------------------------------------------------------
    // Single-Editor Session Lock (Anti-Race Condition & Anti-Duplication)
    // -----------------------------------------------------------------------------------------

    public synchronized boolean tryAcquireEditingLock(Player player) {
        long now = System.currentTimeMillis();
        // If expired (held for > 60 seconds without interaction), automatically release
        if (this.currentEditingPlayer != null && (now - this.editingLockTimestamp > 60000L)) {
            forceReleaseLock();
        }

        if (this.currentEditingPlayer == null || this.currentEditingPlayer.equals(player.getUUID())) {
            this.currentEditingPlayer = player.getUUID();
            this.currentEditingPlayerName = player.getName().getString();
            this.editingLockTimestamp = now;
            ACTIVE_EDITORS.put(player.getUUID(), this);
            return true;
        }
        return false;
    }

    public synchronized void releaseEditingLock(UUID playerUuid) {
        if (this.currentEditingPlayer != null && this.currentEditingPlayer.equals(playerUuid)) {
            this.currentEditingPlayer = null;
            this.currentEditingPlayerName = "";
            this.editingLockTimestamp = 0L;
            if (playerUuid != null) {
                ACTIVE_EDITORS.remove(playerUuid);
            }
        }
    }

    public synchronized void forceReleaseLock() {
        if (this.currentEditingPlayer != null) {
            ACTIVE_EDITORS.remove(this.currentEditingPlayer);
            this.currentEditingPlayer = null;
            this.currentEditingPlayerName = "";
            this.editingLockTimestamp = 0L;
        }
    }

    public boolean isBeingEditedByOther(Player player) {
        if (this.currentEditingPlayer == null) return false;
        long now = System.currentTimeMillis();
        if (now - this.editingLockTimestamp > 60000L) {
            forceReleaseLock();
            return false;
        }
        return !this.currentEditingPlayer.equals(player.getUUID());
    }

    public String getCurrentEditingPlayerName() {
        return this.currentEditingPlayerName;
    }

    public static void onPlayerDisconnected(UUID playerUuid) {
        if (playerUuid != null) {
            TakashaNpcEntity entity = ACTIVE_EDITORS.remove(playerUuid);
            if (entity != null) {
                entity.forceReleaseLock();
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Safe Clean Dismantle (Zero Item Loss)
    // -----------------------------------------------------------------------------------------

    public void dismantle(Player player) {
        if (!this.level().isClientSide()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                // Drop or return all 7 inventory slots (armor, hands, wings)
                for (int i = 0; i < 7; i++) {
                    ItemStack stack = this.inventory.getItem(i);
                    if (!stack.isEmpty()) {
                        giveOrDropItem(player, stack.copy());
                        this.inventory.setItem(i, ItemStack.EMPTY);
                    }
                }

                // Also clear entity equipment slots & wings
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    super.setItemSlot(slot, ItemStack.EMPTY);
                }
                setWingsItem(ItemStack.EMPTY);

                // Return wand spawner to player if not in creative mode
                if (player != null && !player.isCreative()) {
                    giveOrDropItem(player, new ItemStack(ModItems.NPC_SPAWNER_WAND));
                }

                // Poof particles and break sound
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.POOF,
                    this.getX(), this.getY() + 0.9, this.getZ(),
                    20, 0.25, 0.4, 0.25, 0.05
                );
                this.playSound(net.minecraft.sounds.SoundEvents.ARMOR_STAND_BREAK, 1.0F, 1.0F);
            }

            forceReleaseLock();
            this.discard();
        }
    }

    private void giveOrDropItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (player != null) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        } else {
            this.spawnAtLocation((ServerLevel) this.level(), stack);
        }
    }

    // -----------------------------------------------------------------------------------------
    // Getters & Setters for Synched Data
    // -----------------------------------------------------------------------------------------

    public String getSkinUrl() {
        return this.entityData.get(DATA_SKIN_URL);
    }

    public void setSkinUrl(String url) {
        this.entityData.set(DATA_SKIN_URL, url != null ? url : "");
    }

    public String getSkinModel() {
        return this.entityData.get(DATA_SKIN_MODEL);
    }

    public void setSkinModel(String model) {
        this.entityData.set(DATA_SKIN_MODEL, "slim".equalsIgnoreCase(model) ? "slim" : "default");
    }

    public Rotations getHeadPose() {
        return this.entityData.get(DATA_HEAD_POSE);
    }

    public void setHeadPose(Rotations rotations) {
        this.entityData.set(DATA_HEAD_POSE, rotations != null ? rotations : new Rotations(0f, 0f, 0f));
    }

    public Rotations getBodyPose() {
        return this.entityData.get(DATA_BODY_POSE);
    }

    public void setBodyPose(Rotations rotations) {
        this.entityData.set(DATA_BODY_POSE, rotations != null ? rotations : new Rotations(0f, 0f, 0f));
    }

    public Rotations getLeftArmPose() {
        return this.entityData.get(DATA_LEFT_ARM_POSE);
    }

    public void setLeftArmPose(Rotations rotations) {
        this.entityData.set(DATA_LEFT_ARM_POSE, rotations != null ? rotations : new Rotations(0f, 0f, 0f));
    }

    public Rotations getRightArmPose() {
        return this.entityData.get(DATA_RIGHT_ARM_POSE);
    }

    public void setRightArmPose(Rotations rotations) {
        this.entityData.set(DATA_RIGHT_ARM_POSE, rotations != null ? rotations : new Rotations(0f, 0f, 0f));
    }

    public Rotations getLeftLegPose() {
        return this.entityData.get(DATA_LEFT_LEG_POSE);
    }

    public void setLeftLegPose(Rotations rotations) {
        this.entityData.set(DATA_LEFT_LEG_POSE, rotations != null ? rotations : new Rotations(0f, 0f, 0f));
    }

    public Rotations getRightLegPose() {
        return this.entityData.get(DATA_RIGHT_LEG_POSE);
    }

    public void setRightLegPose(Rotations rotations) {
        this.entityData.set(DATA_RIGHT_LEG_POSE, rotations != null ? rotations : new Rotations(0f, 0f, 0f));
    }

    public int getPosePreset() {
        return this.entityData.get(DATA_POSE_PRESET);
    }

    public void setPosePreset(int preset) {
        this.entityData.set(DATA_POSE_PRESET, preset);
        applyPosePreset(preset);
    }

    public float getBedHeightOffset() {
        return this.entityData.get(DATA_BED_HEIGHT_OFFSET);
    }

    private int animErrorCount = 0;
    private int animCooldownTicks = 0;
    private boolean standEmotePlayed = false;

    public void setBedHeightOffset(float offset) {
        this.entityData.set(DATA_BED_HEIGHT_OFFSET, offset);
    }

    public String getEmoteId() {
        return this.entityData.get(DATA_EMOTE_ID);
    }

    public byte getEmotePlayMode() {
        return this.entityData.get(DATA_EMOTE_PLAY_MODE);
    }

    public void setEmotePlayMode(byte mode) {
        this.entityData.set(DATA_EMOTE_PLAY_MODE, mode);
        this.standEmotePlayed = false;
        if (this.level().isClientSide()) {
            String emote = getEmoteId();
            if (emote != null && !emote.isBlank()) {
                net.sakura.weapons.compat.EmotecraftCompat.playEmoteSafely(this, emote, mode);
            }
        }
    }

    public void setEmoteId(String emoteId) {
        this.entityData.set(DATA_EMOTE_ID, emoteId != null ? emoteId : "");
        this.animErrorCount = 0;
        this.animCooldownTicks = 0;
        this.standEmotePlayed = false;
        if (this.level().isClientSide()) {
            if (emoteId != null && !emoteId.isBlank()) {
                net.sakura.weapons.compat.EmotecraftCompat.playEmoteSafely(this, emoteId, getEmotePlayMode());
            } else {
                net.sakura.weapons.compat.EmotecraftCompat.stopEmoteSafely(this);
                net.sakura.weapons.compat.EmotecraftCompat.resetAnimStateSafely(this);
            }
        }
    }


    @Override
    public void tick() {
        try {
            super.tick();
        } catch (Throwable t) {
            animErrorCount++;
            animCooldownTicks = 60; // 3 seconds cooldown to allow recover
            SakuraWeaponsMod.LOGGER.warn("Caught animation/entity tick exception on Takasha NPC (id: {}, errors: {}): {}", this.getId(), animErrorCount, t.getMessage());
            if (this.level().isClientSide()) {
                net.sakura.weapons.compat.EmotecraftCompat.stopEmoteSafely(this);
                net.sakura.weapons.compat.EmotecraftCompat.resetAnimStateSafely(this);
            }
            if (animErrorCount >= 3) {
                // Prolong cooldown to avoid spamming errors, but PRESERVE emoteId in SynchedEntityData
                // so ReplayMod recording and timeline editor retain the emote pose
                animCooldownTicks = 100;
            }
        }

        if (this.level().isClientSide()) {
            if (animCooldownTicks > 0) {
                animCooldownTicks--;
            } else {
                String emote = getEmoteId();
                if (emote != null && !emote.isBlank()) {
                    byte mode = getEmotePlayMode();
                    if (mode == EMOTE_MODE_STAND) {
                        // In play-once mode: start once if not started, and when it finishes, stop cleanly
                        if (!standEmotePlayed) {
                            net.sakura.weapons.compat.EmotecraftCompat.playEmoteSafely(this, emote, mode);
                            standEmotePlayed = true;
                        } else if (!net.sakura.weapons.compat.EmotecraftCompat.isPlayingEmote(this)) {
                            net.sakura.weapons.compat.EmotecraftCompat.stopEmoteSafely(this);
                            net.sakura.weapons.compat.EmotecraftCompat.resetAnimStateSafely(this);
                        }
                    } else {
                        net.sakura.weapons.compat.EmotecraftCompat.ensureEmotePlaying(this, emote, mode);
                    }
                }
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (this.level().isClientSide()) {
            net.sakura.weapons.compat.EmotecraftCompat.stopEmoteSafely(this);
            net.sakura.weapons.compat.EmotecraftCompat.resetAnimStateSafely(this);
        }
        super.remove(reason);
    }

    public float getYawRotation() {
        return this.entityData.get(DATA_YAW_ROTATION);
    }

    public void setYawRotation(float yaw) {
        this.entityData.set(DATA_YAW_ROTATION, yaw);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    public boolean isSmall() {
        return this.entityData.get(DATA_IS_SMALL);
    }

    public void setSmall(boolean small) {
        this.entityData.set(DATA_IS_SMALL, small);
        this.refreshDimensions();
    }

    public boolean isLocked() {
        return this.entityData.get(DATA_IS_LOCKED);
    }

    public void setLocked(boolean locked) {
        this.entityData.set(DATA_IS_LOCKED, locked);
    }

    public boolean shouldShowName() {
        return this.entityData.get(DATA_SHOW_NAME);
    }

    public void setShowName(boolean showName) {
        this.entityData.set(DATA_SHOW_NAME, showName);
    }

    public ItemStack getWingsItem() {
        return this.entityData.get(DATA_WINGS_ITEM);
    }

    public void setWingsItem(ItemStack stack) {
        ItemStack item = stack != null ? stack : ItemStack.EMPTY;
        this.entityData.set(DATA_WINGS_ITEM, item);
        if (!isUpdatingInventory) {
            isUpdatingInventory = true;
            try {
                this.inventory.setItem(6, item);
            } finally {
                isUpdatingInventory = false;
            }
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!isUpdatingInventory) {
            isUpdatingInventory = true;
            try {
                switch (slot) {
                    case HEAD -> this.inventory.setItem(0, stack);
                    case CHEST -> this.inventory.setItem(1, stack);
                    case LEGS -> this.inventory.setItem(2, stack);
                    case FEET -> this.inventory.setItem(3, stack);
                    case MAINHAND -> this.inventory.setItem(4, stack);
                    case OFFHAND -> this.inventory.setItem(5, stack);
                }
            } finally {
                isUpdatingInventory = false;
            }
        }
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public void syncEquipmentFromInventory() {
        isUpdatingInventory = true;
        try {
            super.setItemSlot(EquipmentSlot.HEAD, this.inventory.getItem(0));
            super.setItemSlot(EquipmentSlot.CHEST, this.inventory.getItem(1));
            super.setItemSlot(EquipmentSlot.LEGS, this.inventory.getItem(2));
            super.setItemSlot(EquipmentSlot.FEET, this.inventory.getItem(3));
            super.setItemSlot(EquipmentSlot.MAINHAND, this.inventory.getItem(4));
            super.setItemSlot(EquipmentSlot.OFFHAND, this.inventory.getItem(5));
            this.entityData.set(DATA_WINGS_ITEM, this.inventory.getItem(6));
        } finally {
            isUpdatingInventory = false;
        }
    }

    public void syncInventoryFromEquipment() {
        isUpdatingInventory = true;
        try {
            this.inventory.setItem(0, this.getItemBySlot(EquipmentSlot.HEAD));
            this.inventory.setItem(1, this.getItemBySlot(EquipmentSlot.CHEST));
            this.inventory.setItem(2, this.getItemBySlot(EquipmentSlot.LEGS));
            this.inventory.setItem(3, this.getItemBySlot(EquipmentSlot.FEET));
            this.inventory.setItem(4, this.getItemBySlot(EquipmentSlot.MAINHAND));
            this.inventory.setItem(5, this.getItemBySlot(EquipmentSlot.OFFHAND));
            this.inventory.setItem(6, this.getWingsItem());
        } finally {
            isUpdatingInventory = false;
        }
    }

    public boolean isLyingDownPose() {
        int preset = getPosePreset();
        return preset >= 1 && preset <= 7;
    }

    public boolean isSittingPose() {
        int preset = getPosePreset();
        return (preset >= 8 && preset <= 11) || (preset >= 12 && preset <= 14);
    }

    public boolean isFloorSittingPose() {
        int preset = getPosePreset();
        return preset == 10 || preset == 11 || preset == 14;
    }

    /**
     * Applies standard rotation vectors for 32 distinct expressive presets categorized into:
     * - 1..7: 🛌 Tidur & Rebahan (Kasur, Tengkurap, Miring R/L, Rebahan, Peluk Senjata, Terkapar KO)
     * - 8..14: 🧘 Duduk & Bersimpuh (Kursi, Bersandar, Bersila, Meditasi, Jongkok, Berlutut, Seiza)
     * - 15..24: ⚔ Kuda-kuda Tempur & Senjata (Siaga, Combat, Iaido, Akimbo, Busur, Tombak, Palu, Perisai, Tusuk, Tebas)
     * - 25..32: 🎭 Gestur & Emote Statis (Sedekap, Hormat, Bungkuk, Menunjuk, Melambai, Menangis, Facepalm, Berpikir)
     * - 0: Bebas / Natural Relaxed Standing
     */
    public void applyPosePreset(int preset) {
        switch (preset) {
            // --- Kategori 1: 🛌 Tidur & Rebahan (1..7) ---
            case 1 -> { // Tidur Terlentang Kasur (Vanilla Bed Sleeping)
                setHeadPose(new Rotations(0f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-15f, -10f, 5f));
                setLeftArmPose(new Rotations(-15f, 10f, -5f));
                setRightLegPose(new Rotations(0f, 0f, 2f));
                setLeftLegPose(new Rotations(0f, 0f, -2f));
                setPose(Pose.SLEEPING);
            }
            case 2 -> { // Tidur Tengkurap (Prone Sleeping)
                setHeadPose(new Rotations(0f, 55f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-130f, 20f, -30f));
                setLeftArmPose(new Rotations(-130f, -20f, 30f));
                setRightLegPose(new Rotations(0f, 5f, 5f));
                setLeftLegPose(new Rotations(0f, -5f, -5f));
                setPose(Pose.SLEEPING);
            }
            case 3 -> { // Tidur Miring Kanan (Right Side Sleeping)
                setHeadPose(new Rotations(0f, 15f, 10f));
                setBodyPose(new Rotations(0f, 0f, 5f));
                setRightArmPose(new Rotations(-80f, 15f, 20f));
                setLeftArmPose(new Rotations(-20f, 10f, 5f));
                setRightLegPose(new Rotations(25f, 0f, 5f));
                setLeftLegPose(new Rotations(35f, 0f, 0f));
                setPose(Pose.SLEEPING);
            }
            case 4 -> { // Tidur Miring Kiri (Left Side Sleeping)
                setHeadPose(new Rotations(0f, -15f, -10f));
                setBodyPose(new Rotations(0f, 0f, -5f));
                setRightArmPose(new Rotations(-20f, -10f, -5f));
                setLeftArmPose(new Rotations(-80f, -15f, -20f));
                setRightLegPose(new Rotations(35f, 0f, 0f));
                setLeftLegPose(new Rotations(25f, 0f, -5f));
                setPose(Pose.SLEEPING);
            }
            case 5 -> { // Rebahan Santai Melamun (Lazing / Stargazing)
                setHeadPose(new Rotations(-5f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-140f, -35f, 45f));
                setLeftArmPose(new Rotations(-140f, 35f, -45f));
                setRightLegPose(new Rotations(30f, 20f, 15f));
                setLeftLegPose(new Rotations(0f, -5f, -5f));
                setPose(Pose.SLEEPING);
            }
            case 6 -> { // Tidur Memeluk Senjata (Warrior's Rest)
                setHeadPose(new Rotations(-10f, 10f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-45f, -25f, 15f));
                setLeftArmPose(new Rotations(-40f, 25f, -15f));
                setRightLegPose(new Rotations(5f, 0f, 5f));
                setLeftLegPose(new Rotations(0f, 0f, -5f));
                setPose(Pose.SLEEPING);
            }
            case 7 -> { // Terkapar Kalah KO (Battlefield Knocked Out)
                setHeadPose(new Rotations(15f, 40f, -10f));
                setBodyPose(new Rotations(0f, 5f, 0f));
                setRightArmPose(new Rotations(-30f, 40f, 60f));
                setLeftArmPose(new Rotations(-50f, -30f, -70f));
                setRightLegPose(new Rotations(20f, 15f, 25f));
                setLeftLegPose(new Rotations(-10f, -20f, -30f));
                setPose(Pose.SLEEPING);
            }

            // --- Kategori 2: 🧘 Duduk & Bersimpuh (8..14) ---
            case 8 -> { // Duduk di Kursi Santai (Chair Sitting)
                setHeadPose(new Rotations(0f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-25f, 0f, 10f));
                setLeftArmPose(new Rotations(-25f, 0f, -10f));
                setRightLegPose(new Rotations(-90f, 5f, 0f));
                setLeftLegPose(new Rotations(-90f, -5f, 0f));
                setPose(Pose.SITTING);
            }
            case 9 -> { // Duduk Bersandar (Leaning Sitting)
                setHeadPose(new Rotations(-10f, 0f, 0f));
                setBodyPose(new Rotations(-10f, 0f, 0f));
                setRightArmPose(new Rotations(-35f, 15f, 15f));
                setLeftArmPose(new Rotations(-35f, -15f, -15f));
                setRightLegPose(new Rotations(-80f, 15f, 0f));
                setLeftLegPose(new Rotations(-80f, -15f, 0f));
                setPose(Pose.SITTING);
            }
            case 10 -> { // Duduk Bersila di Lantai (Floor Cross-Legged)
                setHeadPose(new Rotations(0f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-35f, -15f, 10f));
                setLeftArmPose(new Rotations(-35f, 15f, -10f));
                setRightLegPose(new Rotations(-60f, 25f, 45f));
                setLeftLegPose(new Rotations(-60f, -25f, -45f));
                setPose(Pose.SITTING);
            }
            case 11 -> { // Meditasi Hening (Deep Meditation)
                setHeadPose(new Rotations(15f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-45f, -25f, 10f));
                setLeftArmPose(new Rotations(-45f, 25f, -10f));
                setRightLegPose(new Rotations(-70f, 30f, 50f));
                setLeftLegPose(new Rotations(-70f, -30f, -50f));
                setPose(Pose.SITTING);
            }
            case 12 -> { // Jongkok Siaga (Crouching Sentry)
                setHeadPose(new Rotations(0f, 0f, 0f));
                setBodyPose(new Rotations(25f, 0f, 0f));
                setRightArmPose(new Rotations(-40f, 10f, 5f));
                setLeftArmPose(new Rotations(-40f, -10f, -5f));
                setRightLegPose(new Rotations(-30f, 0f, 0f));
                setLeftLegPose(new Rotations(-30f, 0f, 0f));
                setPose(Pose.CROUCHING);
            }
            case 13 -> { // Berlutut Ksatria (One-Knee Kneeling)
                setHeadPose(new Rotations(15f, 0f, 0f));
                setBodyPose(new Rotations(10f, 0f, 0f));
                setRightArmPose(new Rotations(-30f, 0f, 10f));
                setLeftArmPose(new Rotations(-30f, 0f, -10f));
                setRightLegPose(new Rotations(-85f, 0f, 0f));
                setLeftLegPose(new Rotations(-35f, 0f, 0f));
                setPose(Pose.CROUCHING);
            }
            case 14 -> { // Bersimpuh Sopan / Seiza (Seiza Japanese Kneeling)
                setHeadPose(new Rotations(10f, 0f, 0f));
                setBodyPose(new Rotations(5f, 0f, 0f));
                setRightArmPose(new Rotations(-25f, 10f, 5f));
                setLeftArmPose(new Rotations(-25f, -10f, -5f));
                setRightLegPose(new Rotations(-75f, 0f, 0f));
                setLeftLegPose(new Rotations(-75f, 0f, 0f));
                setPose(Pose.CROUCHING);
            }

            // --- Kategori 3: ⚔ Kuda-kuda Tempur & Senjata (15..24) ---
            case 15 -> { // Siaga Berdiri (Standing Guard)
                setHeadPose(new Rotations(0f, 5f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-15f, 0f, 10f));
                setLeftArmPose(new Rotations(-10f, 0f, -10f));
                setRightLegPose(new Rotations(5f, 5f, 0f));
                setLeftLegPose(new Rotations(-5f, -5f, 0f));
                setPose(Pose.STANDING);
            }
            case 16 -> { // Siap Bertarung (Combat Stance)
                setHeadPose(new Rotations(-5f, 15f, 0f));
                setBodyPose(new Rotations(5f, 20f, 0f));
                setRightArmPose(new Rotations(-70f, 25f, 15f));
                setLeftArmPose(new Rotations(-45f, -20f, -10f));
                setRightLegPose(new Rotations(-15f, 15f, 0f));
                setLeftLegPose(new Rotations(20f, -10f, 0f));
                setPose(Pose.STANDING);
            }
            case 17 -> { // Iaido Katana Draw (Iaido Ready)
                setHeadPose(new Rotations(5f, -10f, 0f));
                setBodyPose(new Rotations(10f, -15f, 0f));
                setRightArmPose(new Rotations(-35f, -30f, 20f));
                setLeftArmPose(new Rotations(-15f, 35f, -20f));
                setRightLegPose(new Rotations(25f, 10f, 0f));
                setLeftLegPose(new Rotations(-20f, -10f, 0f));
                setPose(Pose.STANDING);
            }
            case 18 -> { // Senjata Ganda (Dual Wield Akimbo)
                setHeadPose(new Rotations(0f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-65f, -20f, 10f));
                setLeftArmPose(new Rotations(-65f, 20f, -10f));
                setRightLegPose(new Rotations(5f, 5f, 0f));
                setLeftLegPose(new Rotations(-5f, -5f, 0f));
                setPose(Pose.STANDING);
            }
            case 19 -> { // Membidik Busur (Bow Aiming)
                setHeadPose(new Rotations(0f, -75f, 0f));
                setBodyPose(new Rotations(0f, -75f, 0f));
                setRightArmPose(new Rotations(-85f, -45f, 45f));
                setLeftArmPose(new Rotations(-90f, 0f, 0f));
                setRightLegPose(new Rotations(-10f, 30f, 0f));
                setLeftLegPose(new Rotations(25f, -20f, 0f));
                setPose(Pose.STANDING);
            }
            case 20 -> { // Kuda-kuda Tombak (Spear Thrust Stance)
                setHeadPose(new Rotations(0f, 10f, 0f));
                setBodyPose(new Rotations(5f, 15f, 0f));
                setRightArmPose(new Rotations(-75f, 15f, 0f));
                setLeftArmPose(new Rotations(-60f, -25f, 20f));
                setRightLegPose(new Rotations(-25f, 10f, 0f));
                setLeftLegPose(new Rotations(30f, -10f, 0f));
                setPose(Pose.STANDING);
            }
            case 21 -> { // Mengangkat Palu Godam (Great Hammer Hold)
                setHeadPose(new Rotations(-5f, -10f, 0f));
                setBodyPose(new Rotations(5f, -15f, 0f));
                setRightArmPose(new Rotations(-120f, -25f, 20f));
                setLeftArmPose(new Rotations(-95f, 30f, -20f));
                setRightLegPose(new Rotations(15f, 10f, 0f));
                setLeftLegPose(new Rotations(-15f, -10f, 0f));
                setPose(Pose.STANDING);
            }
            case 22 -> { // Memasang Perisai (Shield Defense)
                setHeadPose(new Rotations(0f, 10f, 0f));
                setBodyPose(new Rotations(5f, 25f, 0f));
                setRightArmPose(new Rotations(-25f, 15f, 10f));
                setLeftArmPose(new Rotations(-65f, 10f, 25f));
                setRightLegPose(new Rotations(-10f, 15f, 0f));
                setLeftLegPose(new Rotations(15f, -10f, 0f));
                setPose(Pose.STANDING);
            }
            case 23 -> { // Tusukan Cepat (Dagger / Rapier Thrust)
                setHeadPose(new Rotations(-5f, 5f, 0f));
                setBodyPose(new Rotations(15f, 10f, 0f));
                setRightArmPose(new Rotations(-85f, 10f, 5f));
                setLeftArmPose(new Rotations(-25f, -15f, -20f));
                setRightLegPose(new Rotations(-20f, 5f, 0f));
                setLeftLegPose(new Rotations(25f, -5f, 0f));
                setPose(Pose.STANDING);
            }
            case 24 -> { // Tebasan Melompat (Overhead Slash)
                setHeadPose(new Rotations(-15f, 0f, 0f));
                setBodyPose(new Rotations(-10f, 0f, 0f));
                setRightArmPose(new Rotations(-165f, -15f, 10f));
                setLeftArmPose(new Rotations(-150f, 15f, -10f));
                setRightLegPose(new Rotations(20f, 10f, 5f));
                setLeftLegPose(new Rotations(-10f, -10f, -5f));
                setPose(Pose.STANDING);
            }

            // --- Kategori 4: 🎭 Gestur & Emote Statis (25..32) ---
            case 25 -> { // Bersedekap Dada (Arms Crossed)
                setHeadPose(new Rotations(-5f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-50f, 40f, -60f));
                setLeftArmPose(new Rotations(-50f, -40f, 60f));
                setRightLegPose(new Rotations(5f, 0f, 2f));
                setLeftLegPose(new Rotations(-5f, 0f, -2f));
                setPose(Pose.STANDING);
            }
            case 26 -> { // Hormat Ksatria (Knight's Salute)
                setHeadPose(new Rotations(5f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-65f, -35f, 40f));
                setLeftArmPose(new Rotations(-5f, 0f, -5f));
                setRightLegPose(new Rotations(0f, 0f, 2f));
                setLeftLegPose(new Rotations(0f, 0f, -2f));
                setPose(Pose.STANDING);
            }
            case 27 -> { // Membungkuk Sopan (Respectful Bow)
                setHeadPose(new Rotations(20f, 0f, 0f));
                setBodyPose(new Rotations(30f, 0f, 0f));
                setRightArmPose(new Rotations(-10f, 0f, 5f));
                setLeftArmPose(new Rotations(-10f, 0f, -5f));
                setRightLegPose(new Rotations(0f, 0f, 2f));
                setLeftLegPose(new Rotations(0f, 0f, -2f));
                setPose(Pose.STANDING);
            }
            case 28 -> { // Menunjuk Tegas (Heroic Pointing)
                setHeadPose(new Rotations(-5f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-90f, 0f, 0f));
                setLeftArmPose(new Rotations(-15f, 0f, -15f));
                setRightLegPose(new Rotations(10f, 5f, 0f));
                setLeftLegPose(new Rotations(-10f, -5f, 0f));
                setPose(Pose.STANDING);
            }
            case 29 -> { // Melambai Ramah (Friendly Wave)
                setHeadPose(new Rotations(-5f, 0f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-140f, 0f, 25f));
                setLeftArmPose(new Rotations(-5f, 0f, -5f));
                setRightLegPose(new Rotations(0f, 0f, 2f));
                setLeftLegPose(new Rotations(0f, 0f, -2f));
                setPose(Pose.STANDING);
            }
            case 30 -> { // Menangis Terisak (Weeping / Grief)
                setHeadPose(new Rotations(25f, 0f, 0f));
                setBodyPose(new Rotations(15f, 0f, 0f));
                setRightArmPose(new Rotations(-75f, 45f, -25f));
                setLeftArmPose(new Rotations(-75f, -45f, 25f));
                setRightLegPose(new Rotations(0f, 0f, 2f));
                setLeftLegPose(new Rotations(0f, 0f, -2f));
                setPose(Pose.STANDING);
            }
            case 31 -> { // Facepalm (Embarrassed)
                setHeadPose(new Rotations(10f, 10f, 0f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-120f, -30f, 25f));
                setLeftArmPose(new Rotations(-10f, 0f, -10f));
                setRightLegPose(new Rotations(5f, 0f, 2f));
                setLeftLegPose(new Rotations(-5f, 0f, -2f));
                setPose(Pose.STANDING);
            }
            case 32 -> { // Berpikir Keras (Deep Thinking)
                setHeadPose(new Rotations(10f, 15f, -5f));
                setBodyPose(new Rotations(0f, 0f, 0f));
                setRightArmPose(new Rotations(-80f, -25f, 20f));
                setLeftArmPose(new Rotations(-45f, 30f, -15f));
                setRightLegPose(new Rotations(0f, 0f, 2f));
                setLeftLegPose(new Rotations(0f, 0f, -2f));
                setPose(Pose.STANDING);
            }

            // --- Default: 0 Bebas / Natural Relaxed Standing ---
            default -> {
                setHeadPose(new Rotations(-1.5f, 2.0f, 0f));
                setBodyPose(new Rotations(1.0f, 0f, 0f));
                setRightArmPose(new Rotations(2.0f, 0f, 3.5f));
                setLeftArmPose(new Rotations(-1.0f, 0f, -3.5f));
                setRightLegPose(new Rotations(1.5f, 2.0f, 1.0f));
                setLeftLegPose(new Rotations(-1.5f, -2.0f, -1.0f));
                setPose(Pose.STANDING);
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    public void applyEntityPoseForPreset(int preset) {
        if (preset >= 1 && preset <= 7) {
            setPose(Pose.SLEEPING);
        } else if (preset >= 8 && preset <= 11) {
            setPose(Pose.SITTING);
        } else if (preset >= 12 && preset <= 14) {
            setPose(Pose.CROUCHING);
        } else {
            setPose(Pose.STANDING);
        }
    }

    // -----------------------------------------------------------------------------------------
    // Persistence (26.2 ValueOutput & ValueInput)
    // -----------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        output.putString("skin_url", getSkinUrl());
        output.putString("skin_model", getSkinModel());
        output.putString("entity_pose", getPose().name());
        output.store("head_pose", Rotations.CODEC, getHeadPose());
        output.store("body_pose", Rotations.CODEC, getBodyPose());
        output.store("left_arm_pose", Rotations.CODEC, getLeftArmPose());
        output.store("right_arm_pose", Rotations.CODEC, getRightArmPose());
        output.store("left_leg_pose", Rotations.CODEC, getLeftLegPose());
        output.store("right_leg_pose", Rotations.CODEC, getRightLegPose());

        output.putInt("pose_preset", getPosePreset());
        output.putFloat("bed_height_offset", getBedHeightOffset());
        output.putString("emote_id", getEmoteId());
        output.putByte("emote_play_mode", getEmotePlayMode());
        output.putFloat("yaw_rotation", getYawRotation());
        output.putBoolean("is_small", isSmall());
        output.putBoolean("is_locked", isLocked());
        output.putBoolean("show_name", shouldShowName());
        output.putByte("permission_mode", getPermissionMode());

        this.ownerUUID.ifPresent(uuid -> output.putString("owner_uuid", uuid.toString()));
        output.putString("owner_name", this.ownerName);

        output.putBoolean("pushable", isPushableMode());
        output.putFloat("push_strength", getPushStrength());
        output.putInt("push_permission", getPushPermissionId());
        output.putDouble("origin_x", this.originX);
        output.putDouble("origin_y", this.originY);
        output.putDouble("origin_z", this.originZ);
        output.putFloat("origin_yaw", this.originYaw);

        ItemStack wings = getWingsItem();
        if (!wings.isEmpty()) {
            output.store("wings_item", ItemStack.OPTIONAL_CODEC, wings);
        }

        // Save all 7 inventory slots
        for (int i = 0; i < 7; i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                output.store("inv_slot_" + i, ItemStack.OPTIONAL_CODEC, stack);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        setSkinUrl(input.getStringOr("skin_url", ""));
        setSkinModel(input.getStringOr("skin_model", "default"));

        boolean hasSavedHead = input.read("head_pose", Rotations.CODEC).map(rot -> {
            setHeadPose(rot);
            return true;
        }).orElse(false);
        input.read("body_pose", Rotations.CODEC).ifPresent(this::setBodyPose);
        input.read("left_arm_pose", Rotations.CODEC).ifPresent(this::setLeftArmPose);
        input.read("right_arm_pose", Rotations.CODEC).ifPresent(this::setRightArmPose);
        input.read("left_leg_pose", Rotations.CODEC).ifPresent(this::setLeftLegPose);
        input.read("right_leg_pose", Rotations.CODEC).ifPresent(this::setRightLegPose);

        int preset = input.getIntOr("pose_preset", 0);
        this.entityData.set(DATA_POSE_PRESET, preset);
        if (preset > 0) {
            applyPosePreset(preset);
        } else if (!hasSavedHead) {
            applyPosePreset(0);
        } else {
            String savedPoseName = input.getStringOr("entity_pose", "");
            if (!savedPoseName.isEmpty()) {
                try {
                    setPose(Pose.valueOf(savedPoseName));
                } catch (Exception ignored) {
                    applyEntityPoseForPreset(preset);
                }
            } else {
                applyEntityPoseForPreset(preset);
            }
        }

        setBedHeightOffset(input.getFloatOr("bed_height_offset", 0.0f));
        setEmoteId(input.getStringOr("emote_id", ""));
        setEmotePlayMode(input.getByteOr("emote_play_mode", EMOTE_MODE_LOOP));
        setYawRotation(input.getFloatOr("yaw_rotation", 0.0f));
        setSmall(input.getBooleanOr("is_small", false));
        setLocked(input.getBooleanOr("is_locked", false));
        setShowName(input.getBooleanOr("show_name", true));
        setPermissionMode(input.getByteOr("permission_mode", PERM_PRIVATE));

        input.getString("owner_uuid").ifPresent(str -> {
            try {
                this.ownerUUID = Optional.of(UUID.fromString(str));
            } catch (Exception ignored) {
                this.ownerUUID = Optional.empty();
            }
        });
        this.ownerName = input.getStringOr("owner_name", "");

        setPushableMode(input.getBooleanOr("pushable", input.getBooleanOr("Pushable", true)));
        setPushStrength(input.getFloatOr("push_strength", input.getFloatOr("PushStrength", 0.35f)));
        setPushPermissionId(input.getIntOr("push_permission", input.getIntOr("PushPermission", 2)));
        this.originX = input.getDoubleOr("origin_x", input.getDoubleOr("OriginX", this.getX()));
        this.originY = input.getDoubleOr("origin_y", input.getDoubleOr("OriginY", this.getY()));
        this.originZ = input.getDoubleOr("origin_z", input.getDoubleOr("OriginZ", this.getZ()));
        this.originYaw = input.getFloatOr("origin_yaw", input.getFloatOr("OriginYaw", this.getYRot()));

        // Load 7 inventory slots
        isUpdatingInventory = true;
        try {
            for (int i = 0; i < 7; i++) {
                final int slotIdx = i;
                input.read("inv_slot_" + slotIdx, ItemStack.OPTIONAL_CODEC).ifPresent(stack -> {
                    this.inventory.setItem(slotIdx, stack);
                });
            }
            // Backward compatibility with older saves storing only wings_item
            if (this.inventory.getItem(6).isEmpty()) {
                input.read("wings_item", ItemStack.OPTIONAL_CODEC).ifPresent(this::setWingsItem);
            }
        } finally {
            isUpdatingInventory = false;
        }

        // Synchronize loaded inventory to entity equipment slots & wings
        syncEquipmentFromInventory();
    }

    // -----------------------------------------------------------------------------------------
    // Player Interaction & Anti-Grief Physics Overrides
    // -----------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // Sneak + Wand: Dismantle NPC
        if (player.isCrouching() && heldItem.is(ModItems.NPC_SPAWNER_WAND)) {
            if (canPlayerManage(player)) {
                dismantle(player);
                return InteractionResult.SUCCESS;
            } else {
                player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.no_permission", this.ownerName));
                return InteractionResult.FAIL;
            }
        }

        // Wand click (without sneak) OR Empty hand click (crouching or standing): Open Customization GUI
        if (heldItem.is(ModItems.NPC_SPAWNER_WAND) || heldItem.isEmpty()) {
            if (!canPlayerManage(player)) {
                player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.no_permission", this.ownerName));
                return InteractionResult.FAIL;
            }

            if (isBeingEditedByOther(player)) {
                player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.currently_editing", getCurrentEditingPlayerName()));
                return InteractionResult.FAIL;
            }

            if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                if (tryAcquireEditingLock(serverPlayer)) {
                    serverPlayer.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<Integer>() {
                        @Override
                        public Integer getScreenOpeningData(ServerPlayer player) {
                            return TakashaNpcEntity.this.getId();
                        }

                        @Override
                        public Component getDisplayName() {
                            return TakashaNpcEntity.this.getDisplayName();
                        }

                        @Override
                        public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory playerInventory, Player p) {
                            return new net.sakura.weapons.inventory.TakashaNpcMenu(syncId, playerInventory, TakashaNpcEntity.this.getId());
                        }
                    });
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Quick equipment swap directly in world when holding equipment/weapon
        if (!heldItem.isEmpty()) {
            if (!canPlayerManage(player)) {
                player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.protected_view"));
                return InteractionResult.FAIL;
            }

            EquipmentSlot preferredSlot = this.getEquipmentSlotForItem(heldItem);
            ItemStack currentEquipped = this.getItemBySlot(preferredSlot);
            if (!this.level().isClientSide()) {
                this.setItemSlot(preferredSlot, heldItem.copy());
                player.setItemInHand(hand, currentEquipped);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // Immobile physics & anti-griefing protections with controllable push mechanics
    @Override
    public boolean isPushable() {
        return this.entityData.get(DATA_PUSHABLE);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return this.entityData.get(DATA_PUSHABLE);
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return this.isPushable();
    }

    @Override
    public void push(Entity entity) {
        if (!isPushable()) {
            return;
        }
        if (entity instanceof Player player) {
            NpcPushPermission perm = NpcPushPermission.fromId(this.entityData.get(DATA_PUSH_PERMISSION));
            if (!perm.canPush(player, this)) {
                return;
            }
        }

        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !this.noPhysics) {
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                double maxD = Mth.absMax(dx, dz);
                if (maxD >= 0.01) {
                    maxD = Math.sqrt(maxD);
                    dx /= maxD;
                    dz /= maxD;
                    double inv = 1.0 / maxD;
                    if (inv > 1.0) {
                        inv = 1.0;
                    }
                    float strength = getPushStrength();
                    dx *= inv * strength;
                    dz *= inv * strength;
                    this.push(-dx, 0.0, -dz);
                    this.hurtMarked = true;
                }
            }
        }
    }

    @Override
    public void push(double x, double y, double z) {
        if (!isPushable()) {
            return;
        }
        super.push(x, y, z);
        this.hurtMarked = true;
    }

    @Override
    protected void doPush(Entity other) {
        if (isPushable()) {
            if (other instanceof Player player) {
                NpcPushPermission perm = NpcPushPermission.fromId(this.entityData.get(DATA_PUSH_PERMISSION));
                if (!perm.canPush(player, this)) {
                    return;
                }
            }
            super.doPush(other);
        }
    }

    @Override
    protected void pushEntities() {
        if (isPushableMode()) {
            super.pushEntities();
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.entityData.get(DATA_PUSHABLE)) {
            return;
        }

        NpcPushPermission perm = NpcPushPermission.fromId(this.entityData.get(DATA_PUSH_PERMISSION));
        if (!perm.canPush(player, this)) {
            return;
        }

        double dx = this.getX() - player.getX();
        double dz = this.getZ() - player.getZ();
        double distSq = dx * dx + dz * dz;

        if (distSq < 0.0001 || distSq > 1.5) {
            return;
        }

        double dist = Math.sqrt(distSq);
        float strength = this.entityData.get(DATA_PUSH_STRENGTH);

        double nx = (dx / dist) * strength;
        double nz = (dz / dist) * strength;

        Vec3 current = this.getDeltaMovement();
        this.setDeltaMovement(current.x * 0.15 + nx, current.y, current.z * 0.15 + nz);
        this.hurtMarked = true;
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean attackable() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // Complete invulnerability to weapons, arrows, TNT, fire, void
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player player) {
            // Crouch-punch or creative punch dismantles the mannequin immediately
            if (player.isCrouching() || player.isCreative()) {
                if (canPlayerManage(player)) {
                    dismantle(player);
                    return true;
                } else {
                    player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.no_permission", this.ownerName));
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean isEffectiveAi() {
        return isPushableMode() && super.isEffectiveAi();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isPushableMode()) {
            super.travel(travelVector);
        }
        // Stationary: no gravity or pathfinding movement when not pushable
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (pose == Pose.SLEEPING) {
            return EntityDimensions.scalable(1.8f, 0.35f);
        }
        if (isFloorSittingPose()) {
            return EntityDimensions.scalable(0.7f, 0.85f).scale(isSmall() ? 0.5f : 1.0f);
        }
        if (isSittingPose()) {
            return EntityDimensions.scalable(0.6f, 1.25f).scale(isSmall() ? 0.5f : 1.0f);
        }
        return super.getDefaultDimensions(pose).scale(isSmall() ? 0.5f : 1.0f);
    }
}
