package net.takasha.rpg.modelengine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.mixin.DisplayAccessor;
import net.takasha.rpg.mixin.TextDisplayAccessor;

import java.util.ArrayList;
import java.util.List;

public class TransientTextDisplay {

    private final Display.TextDisplay entity;
    private final List<String> frames;
    private int currentTick = 0;
    private final int maxTicks;
    private boolean discarded = false;

    public TransientTextDisplay(ServerLevel level, Vec3 pos, List<String> frames) {
        this.frames = (frames != null && !frames.isEmpty()) ?
                new ArrayList<>(frames) :
                List.of("\u1780", "\u1781", "\u1782", "\u1783", "\u1784", "\u1785", "\u1786");
        this.maxTicks = this.frames.size();
        this.entity = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
        this.entity.setPos(pos.x, pos.y, pos.z);
        this.entity.addTag("takasha_temp_vfx");

        ((DisplayAccessor) this.entity).callSetBillboardConstraints(Display.BillboardConstraints.CENTER);
        ((DisplayAccessor) this.entity).callSetBrightnessOverride(new net.minecraft.util.Brightness(15, 15));
        ((TextDisplayAccessor) this.entity).callSetBackgroundColor(0);

        if (!this.frames.isEmpty()) {
            ((TextDisplayAccessor) this.entity).callSetText(Component.literal(this.frames.get(0)));
        }

        level.addFreshEntity(this.entity);
    }

    public boolean tick() {
        if (discarded || entity.isRemoved()) {
            return true;
        }

        currentTick++;
        if (currentTick >= maxTicks) {
            discard();
            return true;
        }

        String frame = frames.get(currentTick);
        ((TextDisplayAccessor) entity).callSetText(Component.literal(frame));
        return false;
    }

    public void setPos(Vec3 pos) {
        if (entity != null && !entity.isRemoved()) {
            entity.setPos(pos.x, pos.y, pos.z);
        }
    }

    public void discard() {
        if (!discarded) {
            discarded = true;
            if (entity != null && !entity.isRemoved()) {
                entity.discard();
            }
        }
    }

    public Display.TextDisplay getEntity() {
        return entity;
    }
}
