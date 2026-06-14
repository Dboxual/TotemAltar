/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.Recipe
 *  org.bukkit.inventory.ShapedRecipe
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.totemaltars;

import com.totemaltars.commands.TotemAltarsCommand;
import com.totemaltars.listeners.AffinityListener;
import com.totemaltars.listeners.AltarInteractionListener;
import com.totemaltars.listeners.AltarPlacementListener;
import com.totemaltars.listeners.BedrockBreakListener;
import com.totemaltars.listeners.BedrockRelicListener;
import com.totemaltars.listeners.BedrockRelicLootListener;
import com.totemaltars.listeners.BedrockRelicPlacementListener;
import com.totemaltars.listeners.GuardianListener;
import com.totemaltars.listeners.MobDropListener;
import com.totemaltars.listeners.RitualListener;
import com.totemaltars.listeners.ShardPickupListener;
import com.totemaltars.listeners.TotemActivationListener;
import com.totemaltars.managers.AltarEffectsManager;
import com.totemaltars.managers.AltarManager;
import com.totemaltars.managers.BedrockRelicBlockManager;
import com.totemaltars.managers.ConfigManager;
import com.totemaltars.managers.CooldownManager;
import com.totemaltars.managers.LegacyIngredientCleanup;
import com.totemaltars.managers.PlayerDataManager;
import com.totemaltars.managers.RitualManager;
import com.totemaltars.managers.ShadowArmorManager;
import com.totemaltars.utils.ItemUtil;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class TotemAltars
extends JavaPlugin {
    private static TotemAltars instance;
    private ConfigManager configManager;
    private ItemUtil itemUtil;
    private AltarManager altarManager;
    private AltarEffectsManager altarEffectsManager;
    private RitualManager ritualManager;
    private CooldownManager cooldownManager;
    private ShadowArmorManager shadowArmorManager;
    private PlayerDataManager playerDataManager;
    private LegacyIngredientCleanup legacyIngredientCleanup;
    private BedrockRelicBlockManager bedrockRelicBlockManager;

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.configManager = new ConfigManager(this);
        this.itemUtil = new ItemUtil(this);
        this.altarManager = new AltarManager(this);
        this.altarEffectsManager = new AltarEffectsManager(this);
        this.ritualManager = new RitualManager(this);
        this.cooldownManager = new CooldownManager();
        this.shadowArmorManager = new ShadowArmorManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.legacyIngredientCleanup = new LegacyIngredientCleanup(this);
        this.bedrockRelicBlockManager = new BedrockRelicBlockManager(this);
        this.getServer().getPluginManager().registerEvents((Listener)new BedrockRelicListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BedrockBreakListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BedrockRelicLootListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new BedrockRelicPlacementListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new MobDropListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AffinityListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AltarPlacementListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AltarInteractionListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new RitualListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new TotemActivationListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GuardianListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ShardPickupListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.altarEffectsManager, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.legacyIngredientCleanup, (Plugin)this);
        this.altarEffectsManager.start();
        this.removeLegacyIngredientRecipes();
        this.registerAltarRecipe();
        TotemAltarsCommand cmd = new TotemAltarsCommand(this);
        this.getCommand("totemaltars").setExecutor((CommandExecutor)cmd);
        this.getCommand("totemaltars").setTabCompleter((TabCompleter)cmd);
        this.getServer().getScheduler().runTask((Plugin)this, () -> this.legacyIngredientCleanup.runOnEnableCleanup());
        this.getLogger().info("TotemAltars v" + this.getPluginMeta().getVersion() + " enabled.");
    }

    public void onDisable() {
        if (this.altarEffectsManager != null) {
            this.altarEffectsManager.shutdown();
        }
        if (this.shadowArmorManager != null) {
            this.shadowArmorManager.disable();
        }
        if (this.ritualManager != null) {
            this.ritualManager.shutdown();
        }
        this.getLogger().info("TotemAltars disabled.");
    }

    public static TotemAltars getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ItemUtil getItemUtil() {
        return this.itemUtil;
    }

    public AltarManager getAltarManager() {
        return this.altarManager;
    }

    public AltarEffectsManager getAltarEffectsManager() {
        return this.altarEffectsManager;
    }

    public RitualManager getRitualManager() {
        return this.ritualManager;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public ShadowArmorManager getShadowArmorManager() {
        return this.shadowArmorManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    public LegacyIngredientCleanup getLegacyIngredientCleanup() {
        return this.legacyIngredientCleanup;
    }

    public BedrockRelicBlockManager getBedrockRelicBlockManager() {
        return this.bedrockRelicBlockManager;
    }

    private void registerAltarRecipe() {
        Map<Character, Material> ingredients;
        List<String> shape = this.configManager.getAltarRecipeShape();
        if (!this.isRecipeConfigValid(shape, ingredients = this.configManager.getAltarRecipeIngredients())) {
            this.getLogger().warning("[recipe] Config missing or invalid \u2014 using built-in default recipe.");
            this.getLogger().warning("[recipe]   shape entries: " + shape.size() + ", ingredient entries: " + ingredients.size());
            shape = List.of("OOO", "DED", "ONO");
            ingredients = Map.of(Character.valueOf('D'), Material.DIAMOND, Character.valueOf('E'), Material.ECHO_SHARD, Character.valueOf('N'), Material.NETHER_STAR, Character.valueOf('O'), Material.OBSIDIAN);
        }
        NamespacedKey key = new NamespacedKey((Plugin)this, "totem_altar_recipe");
        boolean removed = this.getServer().removeRecipe(key);
        if (removed) {
            this.getLogger().info("[recipe] Removed stale registration before re-adding.");
        }
        ItemStack result = this.itemUtil.createAltar();
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(new String[]{shape.get(0), shape.get(1), shape.get(2)});
        for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
            recipe.setIngredient(entry.getKey().charValue(), entry.getValue());
        }
        boolean added = this.getServer().addRecipe((Recipe)recipe);
        this.getLogger().info("[recipe] key       : " + String.valueOf(key));
        this.getLogger().info("[recipe] shape     : [" + shape.get(0) + "] [" + shape.get(1) + "] [" + shape.get(2) + "]");
        ingredients.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> this.getLogger().info("[recipe] ingredient: '" + String.valueOf(e.getKey()) + "' = " + ((Material)e.getValue()).name() + (e.getValue() == Material.ECHO_SHARD ? "  <- vanilla ECHO_SHARD (not a custom Totem Shard)" : "")));
        this.getLogger().info("[recipe] result    : " + result.getType().name() + " + PDC totemaltars:totem_altar");
        if (added) {
            this.getLogger().info("[recipe] status    : REGISTERED OK");
        } else {
            this.getLogger().warning("[recipe] status    : FAILED \u2014 addRecipe() returned false. Full server restart required.");
        }
    }

    private void removeLegacyIngredientRecipes() {
        ArrayList<NamespacedKey> keysToRemove = new ArrayList<NamespacedKey>();
        Iterator<Recipe> iterator = this.getServer().recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (!(recipe instanceof Keyed keyed)) {
                continue;
            }
            if (!this.legacyIngredientCleanup.isLegacyIngredientItem(recipe.getResult())) {
                continue;
            }
            keysToRemove.add(keyed.getKey());
        }
        for (NamespacedKey key : keysToRemove) {
            if (this.getServer().removeRecipe(key)) {
                this.getLogger().info("[legacy-cleanup] Removed legacy ingredient recipe: " + key);
            }
        }
    }

    private boolean isRecipeConfigValid(List<String> shape, Map<Character, Material> ingredients) {
        if (shape.size() != 3 || ingredients.isEmpty()) {
            return false;
        }
        int rowLen = shape.get(0).length();
        for (String row : shape) {
            if (row.length() != rowLen) {
                return false;
            }
            for (char c : row.toCharArray()) {
                if (c == ' ' || ingredients.containsKey(Character.valueOf(c))) continue;
                return false;
            }
        }
        return true;
    }
}
