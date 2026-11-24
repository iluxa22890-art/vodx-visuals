package com.vodx.visuals;

public enum Category {
    HUD("HUD"), COMBAT("Combat"), EFFECTS("Effects"), MISC("Misc");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
