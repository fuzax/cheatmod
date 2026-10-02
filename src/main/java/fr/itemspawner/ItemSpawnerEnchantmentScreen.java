package fr.itemspawner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ItemSpawnerEnchantmentScreen extends Screen {
    private static final int ENTRY_HEIGHT = 20;
    private final Screen parent;
    private final Item item;
    private final Consumer<Map<Enchantment, Integer>> onSelect;
    private final List<Enchantment> enchantments;
    private final Map<Enchantment, Integer> selectedEnchantments;
    private List<Enchantment> filteredEnchantments;
    private String searchText = "";
    private int scrollOffset;
    private int maxScroll;
    private int listY;
    private int listHeight;
    private Button applyButton;

    public ItemSpawnerEnchantmentScreen(
            Screen parent,
            Item item,
            Map<Enchantment, Integer> selectedEnchantments,
            Consumer<Map<Enchantment, Integer>> onSelect
    ) {
        super(Component.translatable("itemspawner.enchantment.title"));
        this.parent = parent;
        this.item = item;
        this.onSelect = onSelect;
        this.selectedEnchantments = new LinkedHashMap<>(selectedEnchantments);
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
        listY = 82;
        listHeight = Math.max(40, this.height - 113);

        EditBox searchBox = new EditBox(this.font, listX, 40, listWidth, 20, Component.translatable("itemspawner.search"));
        searchBox.setHint(Component.translatable("itemspawner.search.enchantment"));
        searchBox.setMaxLength(100);
        searchBox.setValue(searchText);
        searchBox.setResponder(this::filterEnchantments);
        this.addRenderableWidget(searchBox);
        applyButton = Button.builder(applyLabel(), button -> {
            onSelect.accept(new LinkedHashMap<>(selectedEnchantments));
            Minecraft.getInstance().setScreen(parent);
        }).bounds(listX, this.height - 26, listWidth, 20).build();
        this.addRenderableWidget(applyButton);
        filterEnchantments(searchText);
    }

    private Component applyLabel() {
        return Component.translatable("itemspawner.enchantment.apply", selectedEnchantments.size());
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
        graphics.drawCenteredString(this.font, Component.translatable("itemspawner.enchantment.help"), this.width / 2, 30 + this.font.lineHeight, 0xAAAAAA);

        int left = 20;
        int right = this.width - 20;
        graphics.fill(left, listY, right, listY + listHeight, 0x80000000);
        graphics.enableScissor(left, listY, right, listY + listHeight);
        int visibleRows = Math.min(listHeight / ENTRY_HEIGHT, filteredEnchantments.size() - scrollOffset);
        for (int row = 0; row < visibleRows; row++) {
            int y = listY + row * ENTRY_HEIGHT;
            Enchantment enchantment = filteredEnchantments.get(scrollOffset + row);
            boolean selected = selectedEnchantments.containsKey(enchantment);
            if (mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + ENTRY_HEIGHT) {
                graphics.fill(left + 1, y, right - 1, y + ENTRY_HEIGHT, 0x5533AAFF);
            }
            graphics.drawString(this.font, selected ? "[x]" : "[ ]", left + 6, y + 6, selected ? 0x55FF55 : 0xFFFFFF);
            String enchantmentName = Component.translatable(enchantment.getDescriptionId()).getString();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(enchantmentName, right - left - 112), left + 30, y + 6, 0xFFFFFF);
            String level = selected ? Integer.toString(selectedEnchantments.get(enchantment)) : "-";
            String levelText = selected ? level + "/" + enchantment.getMaxLevel() : level;
            graphics.drawString(this.font, "-", right - 65, y + 6, selected ? 0xFFFFFF : 0x777777);
            graphics.drawString(this.font, levelText, right - 48, y + 6, selected ? 0xFFFFFF : 0x777777);
            graphics.drawString(this.font, "+", right - 20, y + 6, selected ? 0xFFFFFF : 0x777777);
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
                Enchantment enchantment = filteredEnchantments.get(index);
                int right = this.width - 20;
                if (selectedEnchantments.containsKey(enchantment) && mouseX >= right - 68 && mouseX < right - 48) {
                    selectedEnchantments.computeIfPresent(enchantment, (key, level) -> Math.max(1, level - 1));
                } else if (selectedEnchantments.containsKey(enchantment) && mouseX >= right - 24 && mouseX < right - 4) {
                    selectedEnchantments.computeIfPresent(enchantment, (key, level) -> Math.min(key.getMaxLevel(), level + 1));
                } else if (mouseX < right - 72) {
                    if (selectedEnchantments.containsKey(enchantment)) {
                        selectedEnchantments.remove(enchantment);
                    } else {
                        selectedEnchantments.put(enchantment, 1);
                    }
                }
                applyButton.setMessage(applyLabel());
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