package com.application;

public enum Rarity {
    COMMON(4500), UNCOMMON(3500), EPIC(1500), LEGENDARY(500);

    private final int basisPoints;
    Rarity(int basisPoints) { this.basisPoints = basisPoints; }
    public int basisPoints() { return basisPoints; }
}
