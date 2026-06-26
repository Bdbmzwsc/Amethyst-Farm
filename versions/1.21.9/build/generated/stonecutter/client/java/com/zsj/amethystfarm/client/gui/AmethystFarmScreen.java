package com.zsj.amethystfarm.client.gui;

import com.zsj.amethystfarm.gui.AmethystFarmMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

//? if >=1.21.9
import net.minecraft.client.input.MouseButtonEvent;

public class AmethystFarmScreen extends AbstractContainerScreen<AmethystFarmMenu> {
    private static final int TITLE_COLOR = 0xFFE1BEE7;
    private static final int SECTION_COLOR = 0xFFCE93D8;
    private static final int HINT_COLOR = 0xFFAAAAAA;
    private static final int INVENTORY_COLOR = 0xFF404040;

    public AmethystFarmScreen(AmethystFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 184;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 82;
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int button = event.button();
        if (handleButtonClick(event.x(), event.y(), button)) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleButtonClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    *///?}

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (slotId >= 0 && slotId < AmethystFarmMenu.SLOT_COUNT) {
            sendButtonSlotClick(slot, mouseButton, clickType);
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    private boolean handleButtonClick(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            Slot slot = findButtonSlotAt(mouseX, mouseY);
            if (slot != null) {
                sendButtonSlotClick(slot, button, ClickType.PICKUP);
                return true;
            }
        }
        return false;
    }

    private Slot findButtonSlotAt(double mouseX, double mouseY) {
        if (isButtonSlot(hoveredSlot)) {
            return hoveredSlot;
        }
        for (int i = 0; i < AmethystFarmMenu.SLOT_COUNT && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (isMouseOverSlot(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private boolean isMouseOverSlot(Slot slot, double mouseX, double mouseY) {
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    private boolean isButtonSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        int index = menu.slots.indexOf(slot);
        return index >= 0 && index < AmethystFarmMenu.SLOT_COUNT;
    }

    private void sendButtonSlotClick(Slot slot, int mouseButton, ClickType clickType) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode == null || client.player == null) {
            return;
        }
        int slotId = menu.slots.indexOf(slot);
        if (slotId < 0 || slotId >= AmethystFarmMenu.SLOT_COUNT) {
            return;
        }
        client.gameMode.handleInventoryMouseClick(
            menu.containerId,
            slotId,
            mouseButton,
            clickType,
            client.player
        );
        client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xC0101010);
        graphics.fill(x + 6, y + 6, x + imageWidth - 6, y + 88, 0xFF2A1240);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TITLE_COLOR, false);
        graphics.drawString(font, Component.literal("\u5de5\u4f5c\u6a21\u5f0f / \u519c\u573a\u603b\u5f00\u5173"), 8, 17, SECTION_COLOR, false);
        graphics.drawString(font, Component.literal("\u6316\u6398\u767d\u540d\u5355\uff08\u70b9\u51fb\u5207\u6362\uff09"), 8, 47, SECTION_COLOR, false);
        graphics.drawString(font, Component.literal("\u4ec5\u4fa7\u9762\uff1a\u53ea\u6316\u6bcd\u5ca9\u56db\u4fa7"), 8, 75, HINT_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INVENTORY_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
