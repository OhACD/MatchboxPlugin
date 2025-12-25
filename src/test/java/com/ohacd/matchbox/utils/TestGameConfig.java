package com.ohacd.matchbox.utils;

/**
 * Test configuration object for unit testing.
 * Provides mutable configuration for testing different scenarios.
 */
public class TestGameConfig {
    
    private int minPlayers = 2;
    private int maxPlayers = 7;
    private int swipeDuration = 180;
    private int discussionDuration = 60;
    private int votingDuration = 30;
    private boolean randomSkinsEnabled = false;
    private boolean useSteveSkins = true;
    private String sparkSecondaryAbility = "random";
    private String medicSecondaryAbility = "random";
    
    // Getters and setters
    public int getMinPlayers() {
        return minPlayers;
    }
    
    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public int getSwipeDuration() {
        return swipeDuration;
    }
    
    public void setSwipeDuration(int swipeDuration) {
        this.swipeDuration = swipeDuration;
    }
    
    public int getDiscussionDuration() {
        return discussionDuration;
    }
    
    public void setDiscussionDuration(int discussionDuration) {
        this.discussionDuration = discussionDuration;
    }
    
    public int getVotingDuration() {
        return votingDuration;
    }
    
    public void setVotingDuration(int votingDuration) {
        this.votingDuration = votingDuration;
    }
    
    public boolean isRandomSkinsEnabled() {
        return randomSkinsEnabled;
    }
    
    public void setRandomSkinsEnabled(boolean randomSkinsEnabled) {
        this.randomSkinsEnabled = randomSkinsEnabled;
    }
    
    public boolean isUseSteveSkins() {
        return useSteveSkins;
    }
    
    public void setUseSteveSkins(boolean useSteveSkins) {
        this.useSteveSkins = useSteveSkins;
    }
    
    public String getSparkSecondaryAbility() {
        return sparkSecondaryAbility;
    }
    
    public void setSparkSecondaryAbility(String sparkSecondaryAbility) {
        this.sparkSecondaryAbility = sparkSecondaryAbility;
    }
    
    public String getMedicSecondaryAbility() {
        return medicSecondaryAbility;
    }
    
    public void setMedicSecondaryAbility(String medicSecondaryAbility) {
        this.medicSecondaryAbility = medicSecondaryAbility;
    }
    
    /**
     * Creates a copy of this configuration.
     */
    public TestGameConfig copy() {
        TestGameConfig copy = new TestGameConfig();
        copy.minPlayers = this.minPlayers;
        copy.maxPlayers = this.maxPlayers;
        copy.swipeDuration = this.swipeDuration;
        copy.discussionDuration = this.discussionDuration;
        copy.votingDuration = this.votingDuration;
        copy.randomSkinsEnabled = this.randomSkinsEnabled;
        copy.useSteveSkins = this.useSteveSkins;
        copy.sparkSecondaryAbility = this.sparkSecondaryAbility;
        copy.medicSecondaryAbility = this.medicSecondaryAbility;
        return copy;
    }
    
    /**
     * Creates a minimal valid configuration.
     */
    public static TestGameConfig minimal() {
        TestGameConfig config = new TestGameConfig();
        config.setMinPlayers(2);
        config.setMaxPlayers(2);
        config.setSwipeDuration(30);
        config.setDiscussionDuration(10);
        config.setVotingDuration(10);
        return config;
    }
    
    /**
     * Creates a maximum size configuration.
     */
    public static TestGameConfig maximum() {
        TestGameConfig config = new TestGameConfig();
        config.setMinPlayers(20);
        config.setMaxPlayers(20);
        config.setSwipeDuration(600);
        config.setDiscussionDuration(300);
        config.setVotingDuration(120);
        return config;
    }
    
    /**
     * Creates an invalid configuration (min > max).
     */
    public static TestGameConfig invalid() {
        TestGameConfig config = new TestGameConfig();
        config.setMinPlayers(10);
        config.setMaxPlayers(5); // Invalid: min > max
        return config;
    }
}
