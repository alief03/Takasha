package net.sakura.touhou;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.sakura.touhou.compat.ysm.YsmCompat;
import net.sakura.touhou.network.message.SyncYsmMaidDataPackage;
import net.sakura.touhou.network.message.YsmMaidModelPackage;
import net.sakura.touhou.registry.ModEffects;
import net.sakura.touhou.registry.ModEntities;
import net.sakura.touhou.registry.ModItems;
import net.sakura.touhou.registry.ModMenuTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TouhouLittleMaidMod implements ModInitializer {

    public static final String MOD_ID = "touhou_little_maid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Touhou Little Maid 26.2 (Takasha-RPG Companion Engine)");
        ModEntities.initialize();
        ModItems.initialize();
        ModEffects.initialize();
        ModMenuTypes.initialize();
        YsmCompat.init();

        // Register custom networking payloads (26.2 Fabric Payload API)
        PayloadTypeRegistry.serverboundPlay().register(YsmMaidModelPackage.TYPE, YsmMaidModelPackage.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncYsmMaidDataPackage.TYPE, SyncYsmMaidDataPackage.STREAM_CODEC);

        // Register server packet receivers
        ServerPlayNetworking.registerGlobalReceiver(YsmMaidModelPackage.TYPE, YsmMaidModelPackage::handle);
    }
}
