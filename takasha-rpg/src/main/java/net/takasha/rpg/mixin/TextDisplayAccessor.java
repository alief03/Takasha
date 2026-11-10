package net.takasha.rpg.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {

    @Invoker("setText")
    void callSetText(Component text);

    @Invoker("setBackgroundColor")
    void callSetBackgroundColor(int color);
}
