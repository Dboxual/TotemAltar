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
        if (!key.startsWith("chests/bastion_")) return;

        double chance = plugin.getConfigManager().getBedrockRelicSpawnChance();
        if (random.nextDouble() >= chance) return;

        event.getLoot().add(plugin.getItemUtil().createBedrockRelic());
    }
}
