package com.application;

/** Unenchanted Minecraft Java 1.21.1 items. Names match Bukkit Material constants. */
public enum ArmorCase implements CaseItem {
    LEATHER_HELMET(Rarity.COMMON),
    LEATHER_CHESTPLATE(Rarity.COMMON),
    LEATHER_LEGGINGS(Rarity.COMMON),
    LEATHER_BOOTS(Rarity.COMMON),
    CHAINMAIL_HELMET(Rarity.COMMON),
    CHAINMAIL_CHESTPLATE(Rarity.COMMON),
    CHAINMAIL_LEGGINGS(Rarity.COMMON),
    CHAINMAIL_BOOTS(Rarity.COMMON),
    GOLDEN_HELMET(Rarity.UNCOMMON),
    GOLDEN_CHESTPLATE(Rarity.UNCOMMON),
    GOLDEN_LEGGINGS(Rarity.UNCOMMON),
    GOLDEN_BOOTS(Rarity.UNCOMMON),
    IRON_HELMET(Rarity.UNCOMMON),
    IRON_CHESTPLATE(Rarity.UNCOMMON),
    IRON_LEGGINGS(Rarity.UNCOMMON),
    IRON_BOOTS(Rarity.UNCOMMON),
    DIAMOND_HELMET(Rarity.EPIC),
    DIAMOND_CHESTPLATE(Rarity.EPIC),
    DIAMOND_LEGGINGS(Rarity.EPIC),
    DIAMOND_BOOTS(Rarity.EPIC),
    NETHERITE_HELMET(Rarity.LEGENDARY),
    NETHERITE_CHESTPLATE(Rarity.LEGENDARY),
    NETHERITE_LEGGINGS(Rarity.LEGENDARY),
    NETHERITE_BOOTS(Rarity.LEGENDARY),
    TURTLE_HELMET(Rarity.EPIC),
    LEATHER_HORSE_ARMOR(Rarity.COMMON),
    IRON_HORSE_ARMOR(Rarity.UNCOMMON),
    GOLDEN_HORSE_ARMOR(Rarity.UNCOMMON),
    DIAMOND_HORSE_ARMOR(Rarity.EPIC),
    WOLF_ARMOR(Rarity.UNCOMMON);

    private final Rarity rarity;
    ArmorCase(Rarity rarity) { this.rarity = rarity; }
    @Override public Rarity rarity() { return rarity; }
}
