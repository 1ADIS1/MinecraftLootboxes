package com.application;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CaseCatalogTest {
    @Test
    void allRollsMatchDisplayedProbabilitiesAndRarityBudgets() {
        for (var drops : CaseCatalog.all().values()) {
            assertEquals(10_000, drops.stream().mapToInt(CaseCatalog.Drop::weight).sum());
            assertEquals(100.0, drops.stream().mapToDouble(CaseCatalog.Drop::chance).sum(), 0.00001);
            Map<String, Integer> observed = new HashMap<>();
            for (int roll = 0; roll < 10_000; roll++) {
                observed.merge(CaseCatalog.atRoll(drops, roll).id(), 1, Integer::sum);
            }
            for (var drop : drops) {
                assertEquals(drop.weight(), observed.get(drop.id()));
                assertEquals(drop.weight() / 100.0, drop.chance());
            }
            for (Rarity rarity : Rarity.values()) {
                assertEquals(rarity.basisPoints(), drops.stream()
                        .filter(drop -> drop.rarity().equals(rarity.name().toLowerCase(java.util.Locale.ROOT)))
                        .mapToInt(CaseCatalog.Drop::weight).sum());
            }
        }
    }

    @Test
    void everyArmourSetAndToolTierIsComplete() {
        for (String material : new String[]{"LEATHER", "CHAINMAIL", "IRON", "GOLDEN", "DIAMOND", "NETHERITE"}) {
            for (String slot : new String[]{"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"}) {
                assertNotNull(ArmorCase.valueOf(material + "_" + slot));
            }
        }
        for (String material : new String[]{"WOODEN", "STONE", "IRON", "GOLDEN", "DIAMOND", "NETHERITE"}) {
            for (String tool : new String[]{"PICKAXE", "AXE", "SHOVEL", "HOE"}) {
                assertNotNull(ToolCase.valueOf(material + "_" + tool));
            }
            assertNotNull(WeaponCase.valueOf(material + "_SWORD"));
            assertNotNull(WeaponCase.valueOf(material + "_AXE"));
        }
        assertNotNull(ArmorCase.TURTLE_HELMET);
        assertNotNull(ArmorCase.WOLF_ARMOR);
        assertNotNull(WeaponCase.MACE);
        assertNotNull(WeaponCase.TRIDENT);
    }

    @Test
    void everyDropHasAnImageAndUniqueId() {
        for (var drops : CaseCatalog.all().values()) {
            assertEquals(drops.size(), drops.stream().map(CaseCatalog.Drop::id).distinct().count());
            for (var drop : drops) {
                assertTrue(Files.isRegularFile(Path.of("src/main/resources/static/images/item", drop.image() + ".png")), drop.id());
            }
        }
    }

    @Test
    void rejectsUnknownCaseAndInvalidRolls() {
        assertThrows(ResponseStatusException.class, () -> CaseCatalog.items("made-up"));
        assertThrows(IllegalArgumentException.class, () -> CaseCatalog.atRoll(CaseCatalog.items("armour"), -1));
        assertThrows(IllegalArgumentException.class, () -> CaseCatalog.atRoll(CaseCatalog.items("armour"), 10_000));
    }
}
