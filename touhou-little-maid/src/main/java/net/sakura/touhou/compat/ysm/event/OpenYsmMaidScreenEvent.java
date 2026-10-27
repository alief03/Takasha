package net.sakura.touhou.compat.ysm.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.sakura.touhou.entity.EntityMaid;

/**
 * OpenYsmMaidScreenEvent
 * Dipicu ketika tombol skin di GUI Maid diklik dan mod YSM terpasang.
 * Mod YSM mendengarkan event ini untuk membuka antarmuka pemilihan model YSM.
 */
public class OpenYsmMaidScreenEvent {

    public static final Event<Callback> CALLBACK = EventFactory.createArrayBacked(Callback.class,
            callbacks -> event -> {
                for (Callback callback : callbacks) {
                    callback.post(event);
                }
            });

    private final EntityMaid maid;

    public OpenYsmMaidScreenEvent(EntityMaid maid) {
        this.maid = maid;
    }

    public EntityMaid getMaid() {
        return maid;
    }

    @FunctionalInterface
    public interface Callback {
        void post(OpenYsmMaidScreenEvent event);
    }
}
