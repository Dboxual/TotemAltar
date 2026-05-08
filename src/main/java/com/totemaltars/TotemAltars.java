package com.totemaltars;

import com.totemaltars.commands.TotemAltarsCommand;
import com.totemaltars.expansion.TotemAltarsExpansion;
import com.totemaltars.gui.AltarGUI;
import com.totemaltars.listeners.AltarListener;
import com.totemaltars.listeners.GuardianListener;
import com.totemaltars.listeners.ItemMigrationListener;
import com.totemaltars.listeners.MobDropListener;
import com.totemaltars.listeners.TotemActivationListener;
import com.totemaltars.managers.AltarManager;
import com.totemaltars.managers.ConfigManager;
import com.totemaltars.managers.ConfigMigrator;
import com.totemaltars.managers.CooldownManager;
import com.totemaltars.managers.ShadowArmorManager;
import com.totemaltars.utils.ItemUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class TotemAltars extends JavaPlugin {

    private static TotemAltars instance;

    private ConfigManager      configManager;
    private CooldownManager    cooldownManager;
    private ItemUtil           itemUtil;
    private ShadowArmorManager shadowArmorManager;
    private AltarManager       altarManager;
    private AltarGUI           altarGUI;

    @Override
    public void onEnable() {
        instance = this;

        // Write defaults for keys that don't exist yet, then migrate old configs.
        // This never overwrites keys the server owner has already set.
        saveDefaultConfig();
        ConfigMigrator.migrate(this);

        configManager      = new ConfigManager(this);
        cooldownManager    = new CooldownManager();
        itemUtil           = new ItemUtil(this);
        shadowArmorManager = new ShadowArmorManager(this);
        altarManager       = new AltarManager(this);
        altarGUI           = new AltarGUI(this);

        getServer().getPluginManager().registerEvents(new MobDropListener(this),         this);
        getServer().getPluginManager().registerEvents(new TotemActivationListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarListener(this),           this);
        getServer().getPluginManager().registerEvents(new GuardianListener(this),        this);
        getServer().getPluginManager().registerEvents(new ItemMigrationListener(this),   this);
        getServer().getPluginManager().registerEvents(altarManager,                      this);

        TotemAltarsCommand cmd = new TotemAltarsCommand(this);
        getCommand("totemaltars").setExecutor(cmd);
        getCommand("totemaltars").setTabCompleter(cmd);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TotemAltarsExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("TotemAltars v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (shadowArmorManager != null) shadowArmorManager.disable();
        if (altarManager       != null) altarManager.disable();
        getLogger().info("TotemAltars disabled.");
    }

    public static TotemAltars getInstance()            { return instance; }
    public ConfigManager       getConfigManager()      { return configManager; }
    public CooldownManager     getCooldownManager()    { return cooldownManager; }
    public ItemUtil            getItemUtil()            { return itemUtil; }
    public ShadowArmorManager  getShadowArmorManager() { return shadowArmorManager; }
    public AltarManager        getAltarManager()       { return altarManager; }
    public AltarGUI            getAltarGUI()           { return altarGUI; }
}
