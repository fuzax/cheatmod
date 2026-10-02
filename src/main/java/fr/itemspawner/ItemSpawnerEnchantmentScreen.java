package fr.itemspawner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ItemSpawnerEnchantmentScreen extends Screen {
    private static final int ENTRY_HEIGHT = 20;
    private final Screen parent;
    private final Item item;
    private final Consumer<Enchantment> onSelect;
    private final List<Enchantment> enchantments;
    private List<Enchantment> filteredEnchantments;
    private String searchText = "";
    private int scrollOffset;
    private int maxScroll;
    private int listY;
    private int listHeight;
    private EditBox searchBox;

    public ItemSpawnerEnchantmentScreen(Screen parent, Item item, Consumer<Enchantment> onSelect) {
        super(Component.translatable("itemspawner.enchantment.title"));
        this.parent = parent;
        this.item = item;
        this.onSelect = onSelect;
        ItemStack stack = new ItemStack(item);
        this.enchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .filter(enchantment -> enchantment.canEnchant(stack))
                .sorted(Comparator.comparing(enchantment -> Component.translatable(enchantment.getDescriptionId()).getString()))
                .toList();
        this.filteredEnchantments = this.enchantments;
    }

    @Override
    protected void init() {
        super.init();
        int listX = 20;
        int listWidth = this.width - 40;
        listY = 65;
        listHeight = this.height - 85;

        searchBox = new EditBox(this.font, listX, 38, listWidth, 20, Component.translatable("itemspawner.search"));
        searchBox.setHint(Component.translatable("itemspawner.search.enchantment"));
        searchBox.setMaxLength(100);
        searchBox.setValue(searchText);
        searchBox.setResponder(this::filterEnchantments);
        this.addRenderableWidget(searchBox);
        filterEnchantments(searchText);
    }

    private void filterEnchantments(String query) {
        searchText = query;
        String normalizedQuery = query.strip().toLowerCase(Locale.ROOT);
        filteredEnchantments = enchantments.stream()
                .filter(enchantment -> normalizedQuery.isEmpty()
                        || Component.translatable(enchantment.getDescriptionId()).getString().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
        int visibleRows = Math.max(1, listHeight / ENTRY_HEIGHT);
        maxScroll = Math.max(0, filteredEnchantments.size() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("itemspawner.enchantment.for_item", Component.translatable(item.getDescriptionId())),
                this.width / 2,
                25,
                0xAAAAAA
        );

        int left = 20;
        int right = this.width - 20;
        graphics.fill(left, listY, right, listY + listHeight, 0x80000000);
        graphics.enableScissor(left, listY, right, listY + listHeight);
        int visibleRows = Math.min(listHeight / ENTRY_HEIGHT, filteredEnchantments.size() - scrollOffset);
        for (int row = 0; row < visibleRows; row++) {
            int y = listY + row * ENTRY_HEIGHT;
            Enchantment enchantment = filteredEnchantments.get(scrollOffset + row);
            if (mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + ENTRY_HEIGHT) {
                graphics.fill(left + 1, y, right - 1, y + ENTRY_HEIGHT, 0x5533AAFF);
            }
            graphics.drawString(this.font, Component.translatable(enchantment.getDescriptionId()), left + 8, y + 6, 0xFFFFFF);
            graphics.drawString(
                    this.font,
                    Component.translatable("itemspawner.enchantment.max_level", enchantment.getMaxLevel()),
                    right - 66,
                    y + 6,
                    0xAAAAAA
            );
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= 20 && mouseX < this.width - 20 && mouseY >= listY && mouseY < listY + listHeight) {
            int row = (int) ((mouseY - listY) / ENTRY_HEIGHT);
            int index = scrollOffset + row;
            if (row < listHeight / ENTRY_HEIGHT && index >= 0 && index < filteredEnchantments.size()) {
                onSelect.accept(filteredEnchantments.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listY && mouseY < listY + listHeight) {
            scrollOffset = Mth.clamp(scrollOffset + (delta > 0 ? -3 : 3), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}