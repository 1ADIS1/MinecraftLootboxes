package com.application;

/** Unenchanted Minecraft Java 1.21.1 items. Names match Bukkit Material constants. */
public enum ToolCase implements CaseItem {
    WOODEN_PICKAXE(Rarity.COMMON),
    WOODEN_AXE(Rarity.COMMON),
    WOODEN_SHOVEL(Rarity.COMMON),
    WOODEN_HOE(Rarity.COMMON),
    STONE_PICKAXE(Rarity.COMMON),
    STONE_AXE(Rarity.COMMON),
    STONE_SHOVEL(Rarity.COMMON),
    STONE_HOE(Rarity.COMMON),
    GOLDEN_PICKAXE(Rarity.UNCOMMON),
    GOLDEN_AXE(Rarity.UNCOMMON),
    GOLDEN_SHOVEL(Rarity.UNCOMMON),
    GOLDEN_HOE(Rarity.UNCOMMON),
    IRON_PICKAXE(Rarity.UNCOMMON),
    IRON_AXE(Rarity.UNCOMMON),
    IRON_SHOVEL(Rarity.UNCOMMON),
    IRON_HOE(Rarity.UNCOMMON),
    DIAMOND_PICKAXE(Rarity.EPIC),
    DIAMOND_AXE(Rarity.EPIC),
    DIAMOND_SHOVEL(Rarity.EPIC),
    DIAMOND_HOE(Rarity.EPIC),
    NETHERITE_PICKAXE(Rarity.LEGENDARY),
    NETHERITE_AXE(Rarity.LEGENDARY),
    NETHERITE_SHOVEL(Rarity.LEGENDARY),
    NETHERITE_HOE(Rarity.LEGENDARY),
    SHEARS(Rarity.COMMON),
    FLINT_AND_STEEL(Rarity.COMMON),
    FISHING_ROD(Rarity.COMMON),
    BRUSH(Rarity.UNCOMMON),
    CARROT_ON_A_STICK(Rarity.UNCOMMON),
    WARPED_FUNGUS_ON_A_STICK(Rarity.UNCOMMON),
    ELYTRA(Rarity.LEGENDARY);

    private final Rarity rarity;
    ToolCase(Rarity rarity) { this.rarity = rarity; }
    @Override public Rarity rarity() { return rarity; }
}
