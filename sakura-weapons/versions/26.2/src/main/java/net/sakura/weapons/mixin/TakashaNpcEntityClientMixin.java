package net.sakura.weapons.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.sakura.weapons.client.skin.NpcSkinManager;
import net.sakura.weapons.entity.TakashaNpcEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to implement ClientAvatarEntity on TakashaNpcEntity in client environments.
 * Prevents ClassCastExceptions from third-party client mods such as 3D Skin Layers (skinlayers3d / TRansition)
 * which cast Avatar instances to ClientAvatarEntity during model animation and skin setup.
 */
@Environment(EnvType.CLIENT)
@Mixin(TakashaNpcEntity.class)
public abstract class TakashaNpcEntityClientMixin implements ClientAvatarEntity {

    @Unique
    private final ClientAvatarState takasha$avatarState = new ClientAvatarState();

    @Override
    public PlayerSkin getSkin() {
        TakashaNpcEntity npc = (TakashaNpcEntity) (Object) this;
        String skinInput = npc.getSkinUrl();
        Identifier texture = NpcSkinManager.getSkinTexture(skinInput);
        PlayerModelType modelType = NpcSkinManager.getSkinModel(skinInput, npc.getSkinModel());

        if (texture != null) {
            return new PlayerSkin(
                new ClientAsset.ResourceTexture(texture, texture),
                null,
                null,
                modelType,
                true
            );
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public ClientAvatarState avatarState() {
        return this.takasha$avatarState;
    }

    @Override
    public Parrot.Variant getParrotVariantOnShoulder(boolean isLeft) {
        return null;
    }

    @Override
    public boolean showExtraEars() {
        return false;
    }
}
