package com.application;

/** Unenchanted Minecraft Java 1.21.1 items. Names match Bukkit Material constants. */
public enum WeaponCase implements CaseItem {
    WOODEN_SWORD(Rarity.COMMON),
    WOODEN_AXE(Rarity.COMMON),
    STONE_SWORD(Rarity.COMMON),
    STONE_AXE(Rarity.COMMON),
    GOLDEN_SWORD(Rarity.UNCOMMON),
    GOLDEN_AXE(Rarity.UNCOMMON),
    IRON_SWORD(Rarity.UNCOMMON),
    IRON_AXE(Rarity.UNCOMMON),
    DIAMOND_SWORD(Rarity.EPIC),
    DIAMOND_AXE(Rarity.EPIC),
    NETHERITE_SWORD(Rarity.LEGENDARY),
    NETHERITE_AXE(Rarity.LEGENDARY),
    BOW(Rarity.COMMON),
    CROSSBOW(Rarity.UNCOMMON),
    TRIDENT(Rarity.EPIC),
    MACE(Rarity.LEGENDARY);

    private final Rarity rarity;
    WeaponCase(Rarity rarity) { this.rarity = rarity; }
    @Override public Rarity rarity() { return rarity; }
}
