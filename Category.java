package com.nova.module;

public enum Category {
    MOVEMENT("Movement"), RENDER("Render"), PLAYER("Player"), MISC("Misc");
    public final String label;
    Category(String label) { this.label = label; }
}
