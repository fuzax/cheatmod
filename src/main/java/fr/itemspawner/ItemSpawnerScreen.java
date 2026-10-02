package fr.itemspawner;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

public class ItemSpawnerScreen extends Screen {
    private static final int ENTRY_HEIGHT = 18;
    private static final int MAX_QUANTITY = 64;
    private static final Gson GSON = new Gson();
    private static final Type FAVORITES_TYPE = new TypeToken<Set<String>>() { }.getType();
    private static final Category[] CATEGORIES = Category.values();
    private final List<Item> items = StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
            .filter(item -> item != Items.AIR)
            .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
            .toList();
    private final Set<ResourceLocation> favoriteIds = loadFavorites();

    private List<Item> filteredItems = List.of();
    private Category category = Category.ALL;
    private String searchText = "";
    private Item selectedItem;
    private final Map<Enchantment, Integer> selectedEnchantments = new LinkedHashMap<>();
    private int quantity = 1;
    private int scrollOffset;
    private int maxScroll;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private EditBox quantityInput;

    public ItemSpawnerScreen() {
        super(Component.translatable("itemspawner.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        listX = 16;
        listWidth = this.width - 32;
        listY = 91;
        listHeight = Math.max(36, this.height - 176);

        EditBox searchBox = new EditBox(this.font, listX, 42, listWidth, 20, Component.translatable("itemspawner.search"));
        searchBox.setMaxLength(100);
        searchBox.setHint(Component.translatable("itemspawner.search"));
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value;
            recalcScroll();
        });
        this.addRenderableWidget(searchBox);

        int buttonGap = 2;
        int[] categoryButtonWidths = new int[CATEGORIES.length];
        int totalCategoryWidth = buttonGap * (CATEGORIES.length - 1);
        for (int index = 0; index < CATEGORIES.length; index++) {
            categoryButtonWidths[index] = this.font.width(CATEGORIES[index].label()) + 12;
            totalCategoryWidth += categoryButtonWidths[index];
        }
        int extraWidth = Math.max(0, listWidth - totalCategoryWidth) / CATEGORIES.length;
        int buttonX = listX;
        for (int index = 0; index < CATEGORIES.length; index++) {
            Category buttonCategory = CATEGORIES[index];
            int categoryButtonWidth = categoryButtonWidths[index] + extraWidth;
            this.addRenderableWidget(Button.builder(buttonCategory.label(), button -> {
                category = buttonCategory;
                recalcScroll();
            }).bounds(buttonX, 66, categoryButtonWidth, 20).build());
            buttonX += categoryButtonWidth + buttonGap;
        }

        int controlsY = listY + listHeight + 4;

        quantityInput = new EditBox(this.font, listX + 82, controlsY, 42, 20, Component.translatable("itemspawner.quantity.label"));
        quantityInput.setMaxLength(2);
        quantityInput.setFilter(value -> value.isEmpty() || value.matches("\\d{1,2}"));
        quantityInput.setValue(Integer.toString(quantity));
        quantityInput.setResponder(value -> {
            if (!value.isEmpty()) {
                quantity = Mth.clamp(Integer.parseInt(value), 1, MAX_QUANTITY);
            }
        });
        this.addRenderableWidget(quantityInput);

        int giveWidth = 66;
        int controlGap = 4;
        int enchantmentButtonWidth = listWidth - giveWidth - controlGap;
        Button enchantmentButton = Button.builder(enchantmentButtonLabel(), button -> openEnchantmentPicker())
                .bounds(listX, controlsY + 23, enchantmentButtonWidth, 20).build();
        this.addRenderableWidget(enchantmentButton);

        int giveX = listX + listWidth - giveWidth;
        this.addRenderableWidget(Button.builder(Component.translatable("itemspawner.give"), button -> giveSelectedItem())
                .bounds(giveX, controlsY + 23, giveWidth, 20)
                .build());

        recalcScroll();
    }

    private Component enchantmentButtonLabel() {
        return Component.translatable("itemspawner.enchantment.count", selectedEnchantments.size());
    }

    private void openEnchantmentPicker() {
        if (selectedItem == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new ItemSpawnerEnchantmentScreen(
                this,
                selectedItem,
                selectedEnchantments,
                enchantments -> {
                    selectedEnchantments.clear();
                    selectedEnchantments.putAll(enchantments);
                    Minecraft.getInstance().setScreen(this);
                }
        ));
    }

    private void giveSelectedItem() {
        if (selectedItem == null) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(selectedItem);
        if (itemId != null) {
            int safeQuantity = Mth.clamp(quantity, 1, MAX_QUANTITY);
            Map<ResourceLocation, Integer> enchantments = new LinkedHashMap<>();
            selectedEnchantments.forEach((enchantment, level) -> {
                ResourceLocation enchantmentId = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
                if (enchantmentId != null) {
                    enchantments.put(enchantmentId, Mth.clamp(level, 1, enchantment.getMaxLevel()));
                }
            });
            ItemSpawnerNetwork.sendToServer(itemId, safeQuantity, enchantments);
        }
    }

    private void recalcScroll() {
        String query = searchText.strip().toLowerCase(Locale.ROOT);
        filteredItems = items.stream()
                .filter(this::matchesCategory)
                .filter(item -> matchesSearch(item, query))
                .toList();
        int visibleEntries = Math.max(1, listHeight / ENTRY_HEIGHT);
        maxScroll = Math.max(0, filteredItems.size() - visibleEntries);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    private boolean matchesCategory(Item item) {
        return switch (category) {
            case ALL -> true;
            case BLOCKS -> item instanceof BlockItem;
            case TOOLS -> isTool(item);
            case FOOD -> new ItemStack(item).isEdible();
            case FAVORITES -> {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                yield itemId != null && favoriteIds.contains(itemId);
            }
        };
    }

    private boolean isTool(Item item) {
        ItemStack stack = new ItemStack(item);
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES);
    }

    private boolean matchesSearch(Item item, String query) {
        if (query.isEmpty()) {
            return true;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String id = itemId == null ? "" : itemId.toString().toLowerCase(Locale.ROOT);
        String name = Component.translatable(item.getDescriptionId()).getString().toLowerCase(Locale.ROOT);
        return id.contains(query) || name.contains(query);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 9, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("itemspawner.screen.subtitle"),
                this.width / 2,
                24,
                0xAAAAAA
        );

        int listRight = listX + listWidth;
        int listBottom = listY + listHeight;
        guiGraphics.fill(listX, listY, listRight, listBottom, 0x80000000);
        guiGraphics.fill(listX, listY, listRight, listY + 1, 0xFF555555);
        guiGraphics.fill(listX, listBottom - 1, listRight, listBottom, 0xFF555555);

        guiGraphics.enableScissor(listX, listY, listRight, listBottom);
        int visibleEntries = Math.min(listHeight / ENTRY_HEIGHT, filteredItems.size() - scrollOffset);
        for (int row = 0; row < visibleEntries; row++) {
            int itemIndex = scrollOffset + row;
            int y = listY + row * ENTRY_HEIGHT;
            Item item = filteredItems.get(itemIndex);
            boolean hovered = mouseY >= y && mouseY < y + ENTRY_HEIGHT && mouseX >= listX && mouseX < listRight - 8;

            if (hovered || item == selectedItem) {
                guiGraphics.fill(listX + 1, y, listRight - 8, y + ENTRY_HEIGHT, 0x5533AAFF);
            }

            guiGraphics.renderItem(new ItemStack(item), listX + 6, y + 1);
            guiGraphics.drawString(
                    this.font,
                    Component.translatable(item.getDescriptionId()),
                    listX + 28,
                    y + 5,
                    0xFFFFFF
            );

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            boolean favorite = itemId != null && favoriteIds.contains(itemId);
            guiGraphics.drawString(this.font, favorite ? "*" : "+", listRight - 23, y + 5, favorite ? 0xFFFF55 : 0xAAAAAA);
        }
        guiGraphics.disableScissor();

        int scrollbarX = listRight - 6;
        int scrollbarY = listY + 4;
        int scrollbarHeight = Math.max(1, listHeight - 8);
        int thumbHeight = Math.max(16, (int) ((float) listHeight / Math.max(1, filteredItems.size()) * scrollbarHeight));
        thumbHeight = Math.min(thumbHeight, scrollbarHeight);
        int thumbTravel = Math.max(0, scrollbarHeight - thumbHeight);
        int thumbY = scrollbarY + (int) ((float) scrollOffset / Math.max(1, maxScroll) * thumbTravel);
        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 3, scrollbarY + scrollbarHeight, 0xFF222222);
        guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbHeight, 0xFF888888);

        int controlsY = listY + listHeight + 4;
        guiGraphics.drawString(this.font, Component.translatable("itemspawner.quantity.label"), listX, controlsY + 6, 0xFFFFFF);
        if (selectedItem == null) {
            guiGraphics.drawString(this.font, Component.translatable("itemspawner.selection.none"), listX + 132, controlsY + 6, 0xAAAAAA);
        } else {
            ItemStack preview = new ItemStack(selectedItem, quantity);
            guiGraphics.renderItem(preview, listX + 132, controlsY + 1);
            guiGraphics.renderItemDecorations(this.font, preview, listX + 132, controlsY + 1);
            guiGraphics.drawString(
                this.font,
                Component.translatable("itemspawner.selection.preview", Component.translatable(selectedItem.getDescriptionId()), quantity),
                listX + 152,
                controlsY + 6,
                0xFFFFFF
            );
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listX && mouseX < listX + listWidth - 8 && mouseY >= listY && mouseY < listY + listHeight) {
            int rowOffset = (int) ((mouseY - listY) / ENTRY_HEIGHT);
            if (rowOffset >= listHeight / ENTRY_HEIGHT) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            int rowIndex = scrollOffset + rowOffset;
            if (rowIndex >= 0 && rowIndex < filteredItems.size()) {
                Item item = filteredItems.get(rowIndex);
                if (mouseX >= listX + listWidth - 31) {
                    toggleFavorite(item);
                } else {
                    selectedItem = item;
                    selectedEnchantments.clear();
                    this.clearWidgets();
                    this.init(this.minecraft, this.width, this.height);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleFavorite(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return;
        }
        if (!favoriteIds.add(itemId)) {
            favoriteIds.remove(itemId);
        }
        saveFavorites();
        recalcScroll();
    }

    private static Set<ResourceLocation> loadFavorites() {
        Path favoritesFile = FMLPaths.CONFIGDIR.get().resolve("itemspawner_favorites.json");
        try {
            if (!Files.exists(favoritesFile)) {
                return new HashSet<>();
            }
            Set<String> savedIds = GSON.fromJson(Files.readString(favoritesFile), FAVORITES_TYPE);
            Set<ResourceLocation> result = new HashSet<>();
            if (savedIds != null) {
                for (String savedId : savedIds) {
                    ResourceLocation itemId = ResourceLocation.tryParse(savedId);
                    if (itemId != null) {
                        result.add(itemId);
                    }
                }
            }
            return result;
        } catch (IOException | JsonParseException exception) {
            return new HashSet<>();
        }
    }

    private void saveFavorites() {
        Path favoritesFile = FMLPaths.CONFIGDIR.get().resolve("itemspawner_favorites.json");
        try {
            Files.createDirectories(favoritesFile.getParent());
            List<String> savedIds = favoriteIds.stream().map(ResourceLocation::toString).sorted().toList();
            Files.writeString(favoritesFile, GSON.toJson(savedIds));
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (quantityInput != null && quantityInput.isFocused() && keyCode == 257) {
            quantityInput.setValue(Integer.toString(Mth.clamp(quantity, 1, MAX_QUANTITY)));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int scrollAmount = delta > 0 ? -3 : 3;
            scrollOffset = Mth.clamp(scrollOffset + scrollAmount, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Category {
        ALL("itemspawner.category.all"),
        BLOCKS("itemspawner.category.blocks"),
        TOOLS("itemspawner.category.tools"),
        FOOD("itemspawner.category.food"),
        FAVORITES("itemspawner.category.favorites");

        private final String translationKey;

        Category(String translationKey) {
            this.translationKey = translationKey;
        }

        private Component label() {
            return Component.translatable(translationKey);
        }
    }
}