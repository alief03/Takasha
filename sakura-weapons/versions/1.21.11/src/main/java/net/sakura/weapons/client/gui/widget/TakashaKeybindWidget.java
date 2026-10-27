package net.sakura.weapons.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class TakashaKeybindWidget extends AbstractButton {

    private final String labelPrefix;
    private final KeyMapping keyMapping;
    private boolean listening = false;

    public TakashaKeybindWidget(int x, int y, int width, int height, String labelPrefix, KeyMapping keyMapping) {
        super(x, y, width, height, Component.empty());
        this.labelPrefix = labelPrefix;
        this.keyMapping = keyMapping;
        updateLabel();
    }

    public void startListening() {
        this.listening = true;
        this.setMessage(Component.literal(labelPrefix + ": §e[ Tekan Tombol... ]"));
    }

    public boolean isListening() {
        return listening;
    }

    public void cancelListening() {
        this.listening = false;
        updateLabel();
    }

    public void updateLabel() {
        if (keyMapping == null || keyMapping.isUnbound()) {
            this.setMessage(Component.literal(labelPrefix + ": §7[ Tidak Terikat ]"));
        } else {
            String keyName = keyMapping.getTranslatedKeyMessage().getString();
            this.setMessage(Component.literal(labelPrefix + ": §b[ " + keyName + " ]"));
        }
    }

    @Override
    public void onPress(InputWithModifiers input) {
        startListening();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isFocused) {
        if (this.visible && this.isHovered() && event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (keyMapping != null) {
                keyMapping.setKey(InputConstants.UNKNOWN);
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
                this.listening = false;
                updateLabel();
                return true;
            }
        }
        return super.mouseClicked(event, isFocused);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderDefaultSprite(guiGraphics);
        int textColor = this.listening ? 0xFFFFD54F : (this.isHovered() ? 0xFFFFFFFF : 0xFFE0E0E0);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, textColor);
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening && keyMapping != null) {
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                // Unbind on ESC, Backspace, or Delete
                keyMapping.setKey(InputConstants.UNKNOWN);
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
                this.listening = false;
                updateLabel();
                return true;
            }

            InputConstants.Key mappedKey = InputConstants.getKey(event);
            keyMapping.setKey(mappedKey);
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();

            this.listening = false;
            updateLabel();
            return true;
        }
        return super.keyPressed(event);
    }
}
