package com.ohacd.matchbox.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration class for game sessions.
 * 
 * <p>Provides customizable settings for game duration, abilities, cosmetics, and other
 * game behavior. Use the Builder class to create custom configurations.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * GameConfig config = new GameConfig.Builder()
 *     .swipeDuration(120) // 2 minutes
 *     .sparkAbility("hunter_vision") // Force specific ability
 *     .randomSkins(true)
 *     .build();
 * }</pre>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public final class GameConfig {
    
    private final int swipeDuration;
    private final int discussionDuration;
    private final int votingDuration;
    private final String sparkSecondaryAbility;
    private final String medicSecondaryAbility;
    private final boolean randomSkinsEnabled;
    private final boolean useSteveSkins;
    
    /**
     * Creates a new game configuration.
     * 
     * @param swipeDuration duration of swipe phase in seconds
     * @param discussionDuration duration of discussion phase in seconds
     * @param votingDuration duration of voting phase in seconds
     * @param sparkSecondaryAbility Spark's secondary ability setting
     * @param medicSecondaryAbility Medic's secondary ability setting
     * @param randomSkinsEnabled whether to use random skins
     * @param useSteveSkins whether to force Steve skins
     */
    public GameConfig(int swipeDuration, int discussionDuration, int votingDuration,
                   String sparkSecondaryAbility, String medicSecondaryAbility,
                   boolean randomSkinsEnabled, boolean useSteveSkins) {
        this.swipeDuration = swipeDuration;
        this.discussionDuration = discussionDuration;
        this.votingDuration = votingDuration;
        this.sparkSecondaryAbility = sparkSecondaryAbility;
        this.medicSecondaryAbility = medicSecondaryAbility;
        this.randomSkinsEnabled = randomSkinsEnabled;
        this.useSteveSkins = useSteveSkins;
    }
    
    /**
     * Gets the swipe phase duration in seconds.
     * 
     * @return swipe duration, must be positive
     */
    public int getSwipeDuration() {
        return swipeDuration;
    }
    
    /**
     * Gets the discussion phase duration in seconds.
     * 
     * @return discussion duration, must be positive
     */
    public int getDiscussionDuration() {
        return discussionDuration;
    }
    
    /**
     * Gets the voting phase duration in seconds.
     * 
     * @return voting duration, must be positive
     */
    public int getVotingDuration() {
        return votingDuration;
    }
    
    /**
     * Gets the Spark secondary ability setting.
     * 
     * @return spark ability setting ("random", "hunter_vision", "spark_swap", "delusion")
     */
    @Nullable
    public String getSparkSecondaryAbility() {
        return sparkSecondaryAbility;
    }
    
    /**
     * Gets the Medic secondary ability setting.
     * 
     * @return medic ability setting ("random", "healing_sight") or null if unset
     */
    @Nullable
    public String getMedicSecondaryAbility() {
        return medicSecondaryAbility;
    }
    
    /**
     * Gets whether random skins are enabled.
     * 
     * @return true if random skins are enabled
     */
    public boolean isRandomSkinsEnabled() {
        return randomSkinsEnabled;
    }
    
    /**
     * Gets whether Steve skins are forced.
     * 
     * @return true if Steve skins are forced
     */
    public boolean isUseSteveSkins() {
        return useSteveSkins;
    }
    
    /**
     * Builder class for creating GameConfig instances.
     * 
     * <p>Provides a fluent interface for building game configurations with validation
     * and sensible defaults.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * GameConfig config = new GameConfig.Builder()
     *     .swipeDuration(120)
     *     .discussionDuration(60)
     *     .votingDuration(30)
     *     .sparkAbility("hunter_vision")
     *     .medicAbility("healing_sight")
     *     .randomSkins(true)
     *     .steveSkins(false)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private int swipeDuration = 180; // Default 3 minutes
        private int discussionDuration = 60; // Default 1 minute
        private int votingDuration = 30; // Default 30 seconds
        private String sparkSecondaryAbility = "random"; // Default random
        private String medicSecondaryAbility = "random"; // Default random
        private boolean randomSkinsEnabled = false; // Default disabled
        private boolean useSteveSkins = true; // Default true
        
        /**
         * Creates a new builder with default values.
         */
        public Builder() {
        }
        
        /**
         * Sets the swipe phase duration.
         * 
         * @param seconds duration in seconds, must be positive
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if seconds is not positive
         */
        public Builder swipeDuration(int seconds) {
            if (seconds <= 0) {
                throw new IllegalArgumentException("Swipe duration must be positive");
            }
            this.swipeDuration = seconds;
            return this;
        }
        
        /**
         * Sets the discussion phase duration.
         * 
         * @param seconds duration in seconds, must be positive
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if seconds is not positive
         */
        public Builder discussionDuration(int seconds) {
            if (seconds <= 0) {
                throw new IllegalArgumentException("Discussion duration must be positive");
            }
            this.discussionDuration = seconds;
            return this;
        }
        
        /**
         * Sets the voting phase duration.
         * 
         * @param seconds duration in seconds, must be positive
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if seconds is not positive
         */
        public Builder votingDuration(int seconds) {
            if (seconds <= 0) {
                throw new IllegalArgumentException("Voting duration must be positive");
            }
            this.votingDuration = seconds;
            return this;
        }
        
        /**
         * Sets the Spark secondary ability.
         * 
         * @param ability the ability to use ("random", "hunter_vision", "spark_swap", "delusion")
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if ability is invalid
         */
        public Builder sparkAbility(String ability) {
            if (ability != null && !isValidSparkAbility(ability)) {
                throw new IllegalArgumentException("Invalid Spark ability: " + ability + 
                                               ". Valid values: random, hunter_vision, spark_swap, delusion");
            }
            this.sparkSecondaryAbility = ability;
            return this;
        }
        
        /**
         * Sets the Medic secondary ability.
         * 
         * @param ability the ability to use ("random", "healing_sight")
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if ability is invalid
         */
        public Builder medicAbility(String ability) {
            if (ability != null && !isValidMedicAbility(ability)) {
                throw new IllegalArgumentException("Invalid Medic ability: " + ability + 
                                               ". Valid values: random, healing_sight");
            }
            this.medicSecondaryAbility = ability;
            return this;
        }
        
        /**
         * Sets whether random skins are enabled.
         * 
         * @param enabled true to enable random skins
         * @return this builder instance for method chaining
         */
        public Builder randomSkins(boolean enabled) {
            this.randomSkinsEnabled = enabled;
            return this;
        }
        
        /**
         * Sets whether Steve skins are forced.
         * 
         * @param enabled true to force Steve skins
         * @return this builder instance for method chaining
         */
        public Builder steveSkins(boolean enabled) {
            this.useSteveSkins = enabled;
            return this;
        }
        
        /**
         * Builds the GameConfig instance.
         * 
         * @return the created configuration
         */
        public GameConfig build() {
            return new GameConfig(
                swipeDuration,
                discussionDuration,
                votingDuration,
                sparkSecondaryAbility,
                medicSecondaryAbility,
                randomSkinsEnabled,
                useSteveSkins
            );
        }
        
        private boolean isValidSparkAbility(String ability) {
            return "random".equals(ability) || 
                   "hunter_vision".equals(ability) || 
                   "spark_swap".equals(ability) || 
                   "delusion".equals(ability);
        }
        
        private boolean isValidMedicAbility(String ability) {
            return "random".equals(ability) || 
                   "healing_sight".equals(ability);
        }
    }
}
