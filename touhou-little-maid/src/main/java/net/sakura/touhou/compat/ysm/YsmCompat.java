package net.sakura.touhou.compat.ysm;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;

/**
 * YsmCompat
 * Deteksi dan adapter kompatibilitas Yes Steve Model (YSM) untuk Touhou Little Maid 26.2 Fabric.
 * Persis seperti arsitektur sumber tartaricacid/touhoulittlemaid.
 */
public class YsmCompat {

    public static final String MOD_ID = "yes_steve_model";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static YsmMaidInfo getYsmMaidInfo(CompoundTag nbt) {
        if (isInstalled() && nbt != null) {
            boolean isYsm = nbt.getBooleanOr("IsYsmModel", false);
            String modelId = nbt.getStringOr("YsmModelId", "");
            String texture = nbt.getStringOr("YsmModelTexture", "");
            String name = nbt.getStringOr("YsmModelName", "");
            return new YsmMaidInfo(isYsm, modelId, texture, name);
        }
        return YsmMaidInfo.EMPTY;
    }
}
