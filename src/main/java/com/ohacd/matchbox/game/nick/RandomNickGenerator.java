package com.ohacd.matchbox.game.nick;

import java.util.Random;
import java.util.Set;

/**
 * Generates realistic-looking random usernames for the nicking system.
 *
 * <p>Format is one of several styles modelled on real Minecraft IGNs:
 * <ul>
 *   <li>{@code FrostWolf}       — PascalCase, no separator  (most common)</li>
 *   <li>{@code frost_wolf}      — all-lowercase with underscore</li>
 *   <li>{@code Frost_Wolf}      — title-case with underscore</li>
 *   <li>{@code frostWolf}       — camelCase, first part lowercase</li>
 *   <li>{@code FROST_wolf}      — uppercase adjective, lowercase noun, underscore</li>
 *   <li>{@code frost_WOLF}      — lowercase adjective, uppercase noun, underscore</li>

 * </ul>
 * A numeric suffix (2 digits) is appended on collision, matching the common
 * {@code FrostWolf42} IGN pattern.
 */
public final class RandomNickGenerator {

    private static final Random RANDOM = new Random();

    private static final String[] ADJECTIVES = {
        "shadow", "ember", "frost", "stone", "void", "iron", "crimson", "silent",
        "swift", "dark", "storm", "bright", "hollow", "silver", "grim", "ashen",
        "scarlet", "obsidian", "molten", "crystal", "jade", "copper", "blazing",
        "frozen", "wicked", "noble", "phantom", "rusted", "gilded", "lunar",
        "solar", "cursed", "bitter", "wild", "sage", "dusk", "dawn", "arcane",
        "rogue", "stealth", "cyber", "neon", "blood", "toxic", "electric",
        "cosmic", "brutal", "acid", "hyper", "static"
    };

    private static final String[] NOUNS = {
        "blade", "wolf", "raven", "arrow", "fox", "spark", "viper", "falcon",
        "lynx", "cobra", "hawk", "claw", "fang", "wisp", "shade", "wraith",
        "specter", "crow", "pyre", "coil", "tide", "gust", "pulse", "shard",
        "vault", "drift", "flare", "strike", "surge", "rift", "crest", "dagger",
        "shield", "thorn", "dust", "veil", "skull", "forge", "bolt", "rider",
        "ghost", "hunter", "sniper", "reaper", "knight", "striker", "agent",
        "phantom", "ranger", "warden"
    };

    // ---------------------------------------------------------------------------
    // Style enum
    // ---------------------------------------------------------------------------

    private enum Style {
        /** FrostWolf */
        PASCAL_NO_SEP,
        /** frost_wolf */
        LOWER_UNDERSCORE,
        /** Frost_Wolf */
        TITLE_UNDERSCORE,
        /** frostWolf */
        CAMEL,
        /** FROST_wolf */
        UPPER_ADJ_LOWER_NOUN_UNDERSCORE,
        /** frost_WOLF */
        LOWER_ADJ_UPPER_NOUN_UNDERSCORE;

        // Weights: PascalCase is the most common real-IGN pattern
        private static final Style[] WEIGHTED = {
            PASCAL_NO_SEP, PASCAL_NO_SEP, PASCAL_NO_SEP,
            LOWER_UNDERSCORE,
            TITLE_UNDERSCORE, TITLE_UNDERSCORE,
            CAMEL, CAMEL,
            UPPER_ADJ_LOWER_NOUN_UNDERSCORE,
            LOWER_ADJ_UPPER_NOUN_UNDERSCORE
        };

        static Style random(Random rng) {
            return WEIGHTED[rng.nextInt(WEIGHTED.length)];
        }
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    private RandomNickGenerator() {}

    /**
     * Generates a random username without collision awareness.
     */
    public static String generate() {
        String adj  = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        return applyStyle(adj, noun, Style.random(RANDOM));
    }

    /**
     * Generates a username not present in {@code takenLower} (lower-case comparison).
     * Appends a numeric suffix on collision; guaranteed to return a value.
     *
     * @param takenLower lower-case set of nicks already in use
     * @return a unique nick candidate
     */
    public static String generateUnique(Set<String> takenLower) {
        for (int attempt = 0; attempt < 12; attempt++) {
            String candidate = generate();
            if (!takenLower.contains(candidate.toLowerCase())) {
                return candidate;
            }
            // Numeric suffix — matches real IGN patterns like FrostWolf42
            String suffixed = candidate + (10 + RANDOM.nextInt(90));
            if (!takenLower.contains(suffixed.toLowerCase())) {
                return suffixed;
            }
        }
        // Guaranteed-unique fallback
        return "Player" + (1000 + RANDOM.nextInt(9000));
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private static String applyStyle(String adj, String noun, Style style) {
        return switch (style) {
            case PASCAL_NO_SEP                   -> cap(adj) + cap(noun);
            case LOWER_UNDERSCORE                -> adj + "_" + noun;
            case TITLE_UNDERSCORE                -> cap(adj) + "_" + cap(noun);
            case CAMEL                           -> adj + cap(noun);
            case UPPER_ADJ_LOWER_NOUN_UNDERSCORE -> adj.toUpperCase() + "_" + noun;
            case LOWER_ADJ_UPPER_NOUN_UNDERSCORE -> adj + "_" + noun.toUpperCase();
        };
    }

    /** Capitalises only the first character of a lower-case word. */
    private static String cap(String word) {
        if (word == null || word.isEmpty()) return word;
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
