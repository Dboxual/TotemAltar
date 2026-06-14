/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.persistence.PersistentDataType
 */
package com.totemaltars.utils;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class LegacyTotemUtil {
    public static final NamespacedKey LEGACY_TOTEM_TYPE_KEY = Objects.requireNonNull(NamespacedKey.fromString((String)"totemaltars:totem_type"));
    public static final NamespacedKey LEGACY_INGREDIENT_TYPE_KEY = Objects.requireNonNull(NamespacedKey.fromString((String)"totemaltars:ingredient_type"));
    public static final NamespacedKey LEGACY_GUARDIAN_LINK_ID_KEY = Objects.requireNonNull(NamespacedKey.fromString((String)"totemaltars:guardian_link_id"));
    private static final Set<Material> LEGACY_INGREDIENT_MATERIALS = Set.of(Material.BONE, Material.PALE_OAK_LOG, Material.CHORUS_FRUIT, Material.ECHO_SHARD, Material.ENDER_EYE);

    public static boolean isLegacyTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(LEGACY_TOTEM_TYPE_KEY, PersistentDataType.STRING);
    }

    public static LegacyTotemType getLegacyTotemType(ItemStack item) {
        return LegacyTotemType.from(LegacyTotemUtil.getLegacyTotemTypeRaw(item));
    }

    public static String getLegacyTotemTypeRaw(ItemStack item) {
        if (!LegacyTotemUtil.isLegacyTotem(item)) {
            return null;
        }
        return (String)item.getItemMeta().getPersistentDataContainer().get(LEGACY_TOTEM_TYPE_KEY, PersistentDataType.STRING);
    }

    public static boolean isLegacyIngredient(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        if (!LEGACY_INGREDIENT_MATERIALS.contains(item.getType())) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(LEGACY_INGREDIENT_TYPE_KEY, PersistentDataType.STRING);
    }

    public static String getLegacyIngredientType(ItemStack item) {
        if (!LegacyTotemUtil.isLegacyIngredient(item)) {
            return null;
        }
        return (String)item.getItemMeta().getPersistentDataContainer().get(LEGACY_INGREDIENT_TYPE_KEY, PersistentDataType.STRING);
    }

    public static boolean hasGuardianLink(ItemStack item) {
        if (!LegacyTotemUtil.isLegacyTotem(item)) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(LEGACY_GUARDIAN_LINK_ID_KEY, PersistentDataType.STRING);
    }

    public static UUID getGuardianLinkId(ItemStack item) {
        if (!LegacyTotemUtil.isLegacyTotem(item)) {
            return null;
        }
        String raw = (String)item.getItemMeta().getPersistentDataContainer().get(LEGACY_GUARDIAN_LINK_ID_KEY, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean migrateLegacyTotemIfSafe(ItemStack item) {
        return false;
    }

    private LegacyTotemUtil() {
    }

    public static enum LegacyTotemType {
        BLAST,
        SHADOW,
        STORM,
        SWAP,
        GUARDIAN;


        public static LegacyTotemType from(String value) {
            if (value == null) {
                return null;
            }
            try {
                return LegacyTotemType.valueOf(value.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                return null;
            }
        }

        public String pdc() {
            return this.name().toLowerCase();
        }
    }
}

