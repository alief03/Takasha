package net.sakura.touhou.compat.ysm.event;

import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.sakura.touhou.entity.EntityMaid;

/**
 * UpdateRemoteStructEvent
 * Dipicu ketika server menyinkronkan variabel roaming Molang YSM ke client.
 */
public class UpdateRemoteStructEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            callbacks -> event -> {
                for (Callback callback : callbacks) {
                    callback.post(event);
                }
            });

    private final EntityMaid maid;
    private final Object2FloatOpenHashMap<String> roamingVars;

    public UpdateRemoteStructEvent(EntityMaid maid, Object2FloatOpenHashMap<String> roamingVars) {
        this.maid = maid;
        this.roamingVars = roamingVars;
    }

    public EntityMaid getMaid() {
        return maid;
    }

    public Object2FloatOpenHashMap<String> getRoamingVars() {
        return roamingVars;
    }

    @FunctionalInterface
    public interface Callback {
        void post(UpdateRemoteStructEvent event);
    }
}
