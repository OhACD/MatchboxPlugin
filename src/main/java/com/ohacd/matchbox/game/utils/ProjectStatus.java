package com.ohacd.matchbox.game.utils;

public enum ProjectStatus {
    STABLE("Stable"),
    DEVELOPMENT("Development"),
    BETA("Beta"),
    ALPHA("Alpha"),
    RELEASE_CANDIDATE("Release Candidate"),
    RELEASE("Release");

    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}