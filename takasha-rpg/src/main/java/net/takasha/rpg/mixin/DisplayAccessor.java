package net.takasha.rpg.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {

    @Invoker("setTransformation")
    void callSetTransformation(Transformation transformation);

    @Invoker("setTransformationInterpolationDuration")
    void callSetTransformationInterpolationDuration(int duration);

    @Invoker("setTransformationInterpolationDelay")
    void callSetTransformationInterpolationDelay(int delay);

    @Invoker("setBillboardConstraints")
    void callSetBillboardConstraints(Display.BillboardConstraints constraints);

    @Invoker("setBrightnessOverride")
    void callSetBrightnessOverride(net.minecraft.util.Brightness brightness);
}
