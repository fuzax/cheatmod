package fr.itemspawner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

public class ItemSpawnerScreen extends Screen {
    private static final int ENTRY_HEIGHT = 18;
    private final List<Item> items = StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
            .filter(item -> item != Items.AIR)
            .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
            .toList();

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;

    public ItemSpawnerScreen() {
        super(Component.translatable("itemspawner.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        listX = 20;
        listY = 55;
        listWidth = this.width - 40;
        listHeight = this.height - 90;
        recalcScroll();
    }

    private void recalcScroll() {
        int visibleEntries = Math.max(1, listHeight / ENTRY_HEIGHT);
        maxScroll = Math.max(0, items.size() - visibleEntries);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("itemspawner.screen.title"),
                this.width / 2,
                18,
                0xFFFFFF
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("itemspawner.screen.subtitle"),
                this.width / 2,
                32,
                0xAAAAAA
        );

        int listRight = listX + listWidth;
        int listBottom = listY + listHeight;
        guiGraphics.fill(listX, listY, listRight, listBottom, 0x80000000);
        guiGraphics.fill(listX, listY, listRight, listY + 1, 0xFF555555);
        guiGraphics.fill(listX, listBottom - 1, listRight, listBottom, 0xFF555555);

        int visibleStart = scrollOffset;
        int visibleEnd = Math.min(items.size(), scrollOffset + (listHeight / ENTRY_HEIGHT) + 2);

        for (int index = visibleStart; index < visibleEnd; index++) {
            int y = listY + (index - visibleStart) * ENTRY_HEIGHT;
            Item item = items.get(index);
            boolean hovered = mouseY >= y && mouseY < y + ENTRY_HEIGHT && mouseX >= listX && mouseX <= listRight;

            if (hovered) {
                guiGraphics.fill(listX + 1, y, listRight - 1, y + ENTRY_HEIGHT, 0x5533AAFF);
            }

            ItemStack stack = new ItemStack(item);
            guiGraphics.renderItem(stack, listX + 6, y + 1);
            guiGraphics.drawString(
                    this.font,
                    Component.translatable(item.getDescriptionId()),
                    listX + 28,
                    y + 5,
                    0xFFFFFF
            );
        }

        int scrollbarX = listRight - 8;
        int scrollbarY = listY + 4;
        int scrollbarHeight = listHeight - 8;
        int thumbHeight = Math.max(20, (int) ((float) listHeight / Math.max(1, items.size()) * scrollbarHeight));
        int thumbY = scrollbarY + (int) ((float) scrollOffset / Math.max(1, maxScroll) * Math.max(1, scrollbarHeight - thumbHeight));

        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xFF222222);
        guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF888888);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int rowIndex = scrollOffset + (int) ((mouseY - listY) / ENTRY_HEIGHT);
            if (rowIndex >= 0 && rowIndex < items.size()) {
                Item item = items.get(rowIndex);
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                if (itemId != null) {
                    ItemSpawnerNetwork.sendToServer(itemId);
                    this.onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            scrollOffset = Mth.clamp(scrollOffset - (int) (delta * 3), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}