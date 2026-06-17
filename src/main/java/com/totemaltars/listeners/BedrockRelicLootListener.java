package com.totemaltars.listeners;

import com.totemaltars.TotemAltars;
import java.util.Random;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.loot.LootTable;

public class BedrockRelicLootListener implements Listener {
    private final TotemAltars plugin;
    private final Random random = new Random();

    public BedrockRelicLootListener(TotemAltars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        LootTable lootTable = event.getLootTable();
        if (lootTable == null) return;

        String namespace = lootTable.getKey().getNamespace();
        String key = lootTable.getKey().getKey();

        if (!"minecraft".equals(namespace)) return;

        double chance = getChanceForLootTable(key);
        if (chance <= 0.0) return;
        if (random.nextDouble() >= chance) return;

        event.getLoot().add(plugin.getItemUtil().createBedrockRelic());
    }

    private double getChanceForLootTable(String key) {
        if (key.startsWith("chests/bastion_")) {
            return plugin.getConfigManager().getBedrockRelicBastionChance();
        }
        if (key.startsWith("chests/ancient_city")) {
            return plugin.getConfigManager().getBedrockRelicAncientCityChance();
        }
        if (key.equals("chests/nether_bridge")) {
            return plugin.getConfigManager().getBedrockRelicNetherFortressChance();
        }
        if (key.startsWith("chests/stronghold_")) {
            return plugin.getConfigManager().getBedrockRelicStrongholdChance();
        }
        return 0.0;
    }
}
