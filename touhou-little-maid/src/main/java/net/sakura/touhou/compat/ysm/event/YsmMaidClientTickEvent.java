package net.sakura.touhou.compat.ysm.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.sakura.touhou.entity.EntityMaid;

/**
 * YsmMaidClientTickEvent
 * Dipicu setiap client tick agar YSM dapat memperbarui pose dan bounding box maid.
 */
public class YsmMaidClientTickEvent {

    public static final Event<Callback> CALLBACK = EventFactory.createArrayBacked(Callback.class,
            callbacks -> event -> {
                for (Callback callback : callbacks) {
                    callback.post(event);
                }
            });

    private final EntityMaid maid;

    public YsmMaidClientTickEvent(EntityMaid maid) {
        this.maid = maid;
    }

    public EntityMaid getMaid() {
        return maid;
    }

    @FunctionalInterface
    public interface Callback {
        void post(YsmMaidClientTickEvent event);
    }
}
