package com.totemaltars.gui;

import com.totemaltars.TotemAltars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AltarGUI {

    public static final int TOTEM_SLOT      = 11;
    public static final int INGREDIENT_SLOT = 15;
    public static final int OUTPUT_SLOT     = 13;

    public static final Component TITLE = Component.text("Totem Altar")
            .color(NamedTextColor.DARK_PURPLE);

    private final TotemAltars plugin;

    public AltarGUI(TotemAltars plugin) {
        this.plugin = plugin;
    }

    public void openFor(Player player) {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = makeFiller();
        for (int i = 0; i < 27; i++) {
            if (i != TOTEM_SLOT && i != INGREDIENT_SLOT && i != OUTPUT_SLOT) {
                inv.setItem(i, filler);
            }
        }
        inv.setItem(OUTPUT_SLOT, makePlaceholder());
        return inv;
    }

    /** Recomputes the output slot from whatever is currently in the input slots. */
    public void updateOutput(Inventory inv) {
        ItemStack totemSlot      = inv.getItem(TOTEM_SLOT);
        ItemStack ingredientSlot = inv.getItem(INGREDIENT_SLOT);

        boolean hasVanillaTotem = plugin.getItemUtil().isVanillaTotem(totemSlot);
        String  ingredientType  = plugin.getItemUtil().getIngredientType(ingredientSlot);

        if (hasVanillaTotem && ingredientType != null) {
            inv.setItem(OUTPUT_SLOT, plugin.getItemUtil().createTotem(ingredientType));
        } else {
            inv.setItem(OUTPUT_SLOT, makePlaceholder());
        }
    }

    private ItemStack makePlaceholder() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(
                Component.text("Place a Totem of Undying + Ingredient")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }
}
