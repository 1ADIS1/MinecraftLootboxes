package com.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;

public final class CaseCatalog {
    public record Drop(String id, String material, String rarity, int weight, double chance, String image) {}
    private static final Map<String, List<Drop>> CASES = Map.of(
            "armour", build(ArmorCase.values()),
            "weapon", build(WeaponCase.values()),
            "tool", build(ToolCase.values()));

    private CaseCatalog() {}

    public static Map<String, List<Drop>> all() { return CASES; }

    public static List<Drop> items(String caseId) {
        List<Drop> drops = CASES.get(caseId);
        if (drops == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown case");
        return drops;
    }

    public static Drop select(String caseId, RandomGenerator random) {
        return atRoll(items(caseId), random.nextInt(10_000));
    }

    static Drop atRoll(List<Drop> drops, int roll) {
        if (roll < 0 || roll >= 10_000) throw new IllegalArgumentException("Roll outside probability range");
        for (Drop drop : drops) {
            roll -= drop.weight();
            if (roll < 0) return drop;
        }
        throw new IllegalStateException("Case weights must total 10,000");
    }

    private static List<Drop> build(CaseItem[] items) {
        List<Drop> drops = new ArrayList<>();
        for (Rarity rarity : Rarity.values()) {
            List<CaseItem> group = Arrays.stream(items).filter(item -> item.rarity() == rarity).toList();
            if (group.isEmpty()) throw new IllegalStateException("Missing rarity: " + rarity);
            // Divide each rarity budget into integer basis points so displayed
            // percentages sum to exactly 100.00 and equal the sampling weights.
            for (int i = 0; i < group.size(); i++) {
                CaseItem item = group.get(i);
                int weight = rarity.basisPoints() / group.size()
                        + (i < rarity.basisPoints() % group.size() ? 1 : 0);
                String id = item.name().toLowerCase(Locale.ROOT);
                String image = id.equals("crossbow") ? "crossbow_standby" : id;
                drops.add(new Drop(id, item.name(), rarity.name().toLowerCase(Locale.ROOT),
                        weight, weight / 100.0, image));
            }
        }
        return List.copyOf(drops);
    }
}
