package com.ohacd.matchbox.game.utils.Managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.ability.MedicSecondaryAbility;
import com.ohacd.matchbox.game.ability.SparkSecondaryAbility;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.PlayerNameUtils;
import com.ohacd.matchbox.game.utils.Role;

import java.util.*;

/**
 * Manages game inventories for all players.
 * Sets up identical inventory layouts with role-specific papers and fixed items.
 * 
 * Note: Uses deprecated ItemMeta methods (setDisplayName/setLore) which are still
 * functional in Bukkit 1.21. These warnings can be safely ignored.
 */
@SuppressWarnings("deprecation")
public class InventoryManager {
    private final Plugin plugin;
    
    // Slot constants
    private static final int ROLE_PAPER_SLOT = 17; // Top rightmost slot in inventory (top row, rightmost)
    private static final int SWIPE_CURE_PAPER_SLOT = 27; // Above hotbar slot 0
    private static final int VISION_SIGHT_PAPER_SLOT = 28; // Above hotbar slot 1 (also used for Spark Swap)
    private static final int CROSSBOW_HOTBAR_SLOT = 7; // Hotbar slot 7 (second from right)
    private static final int ARROW_HOTBAR_SLOT = 8; // Hotbar slot 8 (rightmost)
    private static final int VOTING_PAPER_START_SLOT = 0; // First slot for voting papers
    private static final int VOTING_PAPER_END_SLOT = 6; // Last slot before crossbow (slot 7)

    private static NamespacedKey VOTE_TARGET_KEY;
    
    // Track players who have used their arrow this round
    private final Set<UUID> usedArrowThisRound = new HashSet<>();
    
    // Track voting papers (player UUID -> voting paper item)
    private final Map<UUID, ItemStack> votingPapers = new HashMap<>();
    
    public InventoryManager(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        if (VOTE_TARGET_KEY == null) {
            VOTE_TARGET_KEY = new NamespacedKey(plugin, "vote-target");
        }
    }
    
    /**
     * Sets up the game inventory for a player based on their role.
     * All players get identical layouts with role-specific papers.
     */
    public void setupPlayerInventory(Player player, Role role, SparkSecondaryAbility sparkAbility, MedicSecondaryAbility medicAbility) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Cannot setup inventory for null or offline player");
            return;
        }
        
        if (role == null) {
            plugin.getLogger().warning("Cannot setup inventory for player " + player.getName() + " with null role");
            return;
        }
        
        try {
            PlayerInventory inv = player.getInventory();
            if (inv == null) {
                plugin.getLogger().warning("Player inventory is null for " + player.getName());
                return;
            }
            
            // Clear inventory first
            inv.clear();
            
            // Set role paper in top rightmost slot (slot 8)
            ItemStack rolePaper = createRolePaper(player, role);
            inv.setItem(ROLE_PAPER_SLOT, rolePaper);
            
            // Set ability papers based on role
            if (role == Role.SPARK) {
                // Spark: Swipe paper in slot 27, secondary ability in slot 28
                ItemStack swipePaper = createSwipePaper();
                ItemStack visionPaper = createHunterVisionPaper();
                ItemStack swapPaper = createSparkSwapPaper();
                ItemStack delusionPaper = createDelusionPaper();
                inv.setItem(SWIPE_CURE_PAPER_SLOT, swipePaper);
                if (sparkAbility == SparkSecondaryAbility.SPARK_SWAP) {
                    inv.setItem(VISION_SIGHT_PAPER_SLOT, swapPaper);
                } else if (sparkAbility == SparkSecondaryAbility.DELUSION) {
                    inv.setItem(VISION_SIGHT_PAPER_SLOT, delusionPaper);
                } else {
                    inv.setItem(VISION_SIGHT_PAPER_SLOT, visionPaper);
                }
            } else if (role == Role.MEDIC) {
                // Medic: Healing Touch in slot 27, secondary ability in slot 28
                ItemStack curePaper = createHealingTouchPaper();
                ItemStack sightPaper = createHealingSightPaper();
                inv.setItem(SWIPE_CURE_PAPER_SLOT, curePaper);
                if (medicAbility == MedicSecondaryAbility.HEALING_SIGHT) {
                    inv.setItem(VISION_SIGHT_PAPER_SLOT, sightPaper);
                } else {
                    // Default to healing sight if unknown ability
                    inv.setItem(VISION_SIGHT_PAPER_SLOT, sightPaper);
                }
            } else {
                // Innocent: Empty slots (or placeholder papers if needed)
                // For now, leave empty for innocents
            }
            
            // Set fixed crossbow in hotbar slot 7
            ItemStack crossbow = createFixedCrossbow();
            inv.setItem(CROSSBOW_HOTBAR_SLOT, crossbow);
            
            // Set fixed arrow in hotbar slot 8 (rightmost)
            ItemStack arrow = createFixedArrow();
            inv.setItem(ARROW_HOTBAR_SLOT, arrow);
            
            // Update inventory
            player.updateInventory();
            
            plugin.getLogger().info("Set up inventory for " + player.getName() + " (Role: " + role + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting up inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restores the primary ability paper (slot 27) based on the player's role.
     * Used when an activation window expires without being used.
     */
    public void refreshAbilityPaper(Player player, Role role) {
        if (player == null || role == null) {
            return;
        }

        try {
            PlayerInventory inv = player.getInventory();
            if (inv == null) {
                return;
            }

            ItemStack paper = null;
            if (role == Role.SPARK) {
                paper = createSwipePaper();
            } else if (role == Role.MEDIC) {
                paper = createHealingTouchPaper();
            }

            if (paper == null) {
                return;
            }

            inv.setItem(SWIPE_CURE_PAPER_SLOT, paper);
            player.updateInventory();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to refresh ability paper for " + player.getName() + ": " + e.getMessage());
        }
    }


    /**
     * Restores the secondary ability paper (Slot 28) based on the player's role.
     * Used when an activation window expires without being used
     * 
     */
    public void refreshSecondaryAbilityPaper(Player player, Role role, SessionGameContext context) {
        try {
            PlayerInventory inv = player.getInventory();
            if (inv == null) return;

            GameState gameState = context.getGameState();
            if (gameState == null) return;

            ItemStack paper = null;

            if (role == Role.SPARK) {
                // Switch statement to set the secondary ability paper based on the game state
                switch (gameState.getSparkSecondaryAbility()) {
                    case HUNTER_VISION:
                        paper = createHunterVisionPaper();
                        break;
                    case SPARK_SWAP:
                        paper = createSparkSwapPaper();
                        break;
                    case DELUSION:
                        paper = createDelusionPaper();
                        break;
                    default:
                        paper = createHunterVisionPaper();
                        break;
                }
            } else if (role == Role.MEDIC) {
                // Switch statement to set the secondary ability paper based on the game state
                switch (gameState.getMedicSecondaryAbility()) {
                    case HEALING_SIGHT:
                        paper = createHealingSightPaper();
                        break;
                }
            } else {
                // Default to healing sight if unknown ability
                paper = createHealingSightPaper();
            }
            inv.setItem(VISION_SIGHT_PAPER_SLOT, paper);
            player.updateInventory();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to refresh secondary ability paper for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sets up inventories for all players in a collection.
     */
    public void setupInventories(Collection<Player> players, Map<UUID, Role> roles, SparkSecondaryAbility sparkAbility, MedicSecondaryAbility medicAbility) {
        if (players == null || roles == null) {
            plugin.getLogger().warning("Cannot setup inventories: players or roles is null");
            return;
        }
        
        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            Role role = roles.get(player.getUniqueId());
            if (role != null) {
                setupPlayerInventory(player, role, sparkAbility, medicAbility);
            }
        }
    }
    
    /**
     * Clears game items from player inventory.
     * Should be called when game ends or player is eliminated.
     */
    public void clearGameItems(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        try {
            PlayerInventory inv = player.getInventory();
            if (inv == null) {
                return;
            }
            
            // Remove game items
            inv.setItem(ROLE_PAPER_SLOT, null);
            inv.setItem(SWIPE_CURE_PAPER_SLOT, null);
            inv.setItem(VISION_SIGHT_PAPER_SLOT, null);
            inv.setItem(CROSSBOW_HOTBAR_SLOT, null);
            inv.setItem(ARROW_HOTBAR_SLOT, null);
            
            player.updateInventory();
        } catch (Exception e) {
            plugin.getLogger().warning("Error clearing game items for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Resets arrow usage for a new round.
     */
    public void resetArrowUsage() {
        usedArrowThisRound.clear();
    }
    
    /**
     * Gives voting papers to a player for all alive players.
     * Papers fill slots 0-6 (before crossbow slot 7).
     * Each paper corresponds to one alive player.
     */
    public void giveVotingPapers(Player voter, Collection<Player> alivePlayers) {
        if (voter == null || !voter.isOnline()) {
            plugin.getLogger().warning("Cannot give voting papers to null or offline player");
            return;
        }
        
        if (alivePlayers == null || alivePlayers.isEmpty()) {
            plugin.getLogger().warning("Cannot give voting papers: no alive players");
            return;
        }
        
        try {
            PlayerInventory inv = voter.getInventory();
            if (inv == null) {
                plugin.getLogger().warning("Player inventory is null for " + voter.getName());
                return;
            }
            
            // Clear voting papers from previous rounds
            clearVotingPapers(voter);
            
            // Filter out the voter themselves and offline players
            List<Player> voteablePlayers = new ArrayList<>();
            for (Player player : alivePlayers) {
                if (player != null && player.isOnline() && !player.getUniqueId().equals(voter.getUniqueId())) {
                    voteablePlayers.add(player);
                }
            }
            
            // Limit to max 7 players (slots 0-6)
            int maxPapers = Math.min(voteablePlayers.size(), 7);
            
            // Give voting papers starting from slot 0
            for (int i = 0; i < maxPapers; i++) {
                Player target = voteablePlayers.get(i);
                ItemStack votePaper = createVotingPaper(target);
                inv.setItem(VOTING_PAPER_START_SLOT + i, votePaper);
                votingPapers.put(target.getUniqueId(), votePaper);
            }
            
            voter.updateInventory();
            plugin.getLogger().info("Gave " + maxPapers + " voting papers to " + voter.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Error giving voting papers to " + voter.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clears voting papers from a player's inventory.
     */
    public void clearVotingPapers(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        try {
            PlayerInventory inv = player.getInventory();
            if (inv == null) {
                return;
            }
            
            // Clear slots 0-6
            for (int i = VOTING_PAPER_START_SLOT; i <= VOTING_PAPER_END_SLOT; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() == Material.PAPER && isVotingPaper(item)) {
                    inv.setItem(i, null);
                }
            }
            
            player.updateInventory();
        } catch (Exception e) {
            plugin.getLogger().warning("Error clearing voting papers for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Clears voting papers from all players.
     */
    public void clearAllVotingPapers(Collection<Player> players) {
        if (players == null) {
            return;
        }
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                clearVotingPapers(player);
            }
        }
        votingPapers.clear();
    }
    
    /**
     * Creates a voting paper for a specific player.
     */
    private ItemStack createVotingPaper(Player target) {
        if (target == null) {
            return null;
        }
        
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }
        
        String displayName = PlayerNameUtils.displayName(target);
        meta.setDisplayName("§eVote: " + displayName);
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click this paper to vote");
        lore.add("§7for " + displayName);
        lore.add("§7during voting phase.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        // Store target UUID in persistent data container for identification
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (VOTE_TARGET_KEY != null) {
            data.set(VOTE_TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
        }
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }
    
    /**
     * Checks if an item is a voting paper.
     */
    public static boolean isVotingPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        String displayName = meta.getDisplayName();
        if (displayName == null) {
            return false;
        }
        
        return displayName.startsWith("§eVote: ");
    }
    
    /**
     * Gets the target player UUID from a voting paper.
     * Falls back to display name parsing when no UUID is stored (legacy papers).
     */
    public static UUID getVotingPaperTargetId(ItemStack paper) {
        if (paper == null || !isVotingPaper(paper)) {
            return null;
        }

        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return null;
        }

        if (VOTE_TARGET_KEY != null) {
            String rawId = meta.getPersistentDataContainer().get(VOTE_TARGET_KEY, PersistentDataType.STRING);
            if (rawId != null) {
                try {
                    return UUID.fromString(rawId);
                } catch (IllegalArgumentException ignored) {
                    // fall back to display name parsing
                }
            }
        }

        // Legacy fallback: parse display name
        String displayName = meta.getDisplayName();
        if (displayName == null || !displayName.startsWith("§eVote: ")) {
            return null;
        }
        // Not ideal, but allows legacy papers to continue to function when names are unique
        return null;
    }

    /**
     * Gets the target display text from a voting paper (for UI/logging).
     */
    public static String getVotingPaperTargetDisplay(ItemStack paper) {
        if (paper == null || !isVotingPaper(paper)) {
            return null;
        }
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return null;
        }
        String displayName = meta.getDisplayName();
        if (displayName == null || !displayName.startsWith("§eVote: ")) {
            return null;
        }
        return displayName.substring(8);
    }
    
    /**
     * Marks that a player has used their arrow this round.
     */
    public void markArrowUsed(UUID playerId) {
        if (playerId != null) {
            usedArrowThisRound.add(playerId);
        }
    }
    
    /**
     * Checks if a player has used their arrow this round.
     */
    public boolean hasUsedArrow(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return usedArrowThisRound.contains(playerId);
    }
    
    /**
     * Gives a new arrow to the player if they haven't used it this round.
     */
    public void giveArrowIfNeeded(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        if (hasUsedArrow(playerId)) {
            return; // Already used arrow this round
        }
        
        try {
            PlayerInventory inv = player.getInventory();
            if (inv == null) {
                return;
            }
            
            // Check if arrow slot is empty or has no arrows
            ItemStack currentArrow = inv.getItem(ARROW_HOTBAR_SLOT);
            if (currentArrow == null || currentArrow.getType() != Material.ARROW || currentArrow.getAmount() == 0) {
                ItemStack arrow = createFixedArrow();
                inv.setItem(ARROW_HOTBAR_SLOT, arrow);
                player.updateInventory();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error giving arrow to " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Creates a role paper with player name, role, and role-specific description.
     */
    private ItemStack createRolePaper(Player player, Role role) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }
        
        String playerName = player != null ? player.getName() : "Unknown";
        meta.setDisplayName("§6" + playerName + "'s Role");
        List<String> lore = new ArrayList<>();
        
        // Add player name
        lore.add("§7Player: §f" + playerName);
        lore.add(""); // Empty line
        
        switch (role) {
            case SPARK:
                lore.add("§c§lROLE: SPARK");
                lore.add("");
                lore.add("§7You are the impostor!");
                lore.add("§7Infect players to eliminate them.");
                lore.add("§7Use your abilities to stay hidden.");
                lore.add("§7Win by eliminating all other players.");
                break;
            case MEDIC:
                lore.add("§a§lROLE: MEDIC");
                lore.add("");
                lore.add("§7You can save infected players!");
                lore.add("§7Use Healing Touch to cure players.");
                lore.add("§7Use Healing Sight to see who's infected.");
                lore.add("§7Win by eliminating the Spark.");
                break;
            case INNOCENT:
                lore.add("§f§lROLE: INNOCENT");
                lore.add("");
                lore.add("§7You are a regular player.");
                lore.add("§7Work with others to find the Spark.");
                lore.add("§7Use your arrow to reveal nametags.");
                lore.add("§7Win by eliminating the Spark.");
                break;
        }
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }
    
    /**
     * Creates the Swipe ability paper.
     */
    private ItemStack createSwipePaper() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }
        
        meta.setDisplayName("§cSwipe Ability");
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to activate.");
        lore.add("§7Then right-click a player to infect them.");
        lore.add("§7You have 8 seconds to use it.");
        lore.add("§7Once per round.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }
    
    /**
     * Creates the Hunter Vision ability paper.
     */
    private ItemStack createHunterVisionPaper() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }
        
        meta.setDisplayName("§6Hunter Vision");
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to see all players.");
        lore.add("§7Lasts for 15 seconds.");
        lore.add("§7Once per round.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }

    /**
     * Creates the Spark Swap ability paper.
     */
    private ItemStack createSparkSwapPaper() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }

        meta.setDisplayName("§cSpark Swap");
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to silently swap");
        lore.add("§7positions with a random player.");
        lore.add("§7Keeps both velocities.");
        lore.add("§7Once per round.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }

    /**
     * Creates the Delusion ability paper.
     */
    private ItemStack createDelusionPaper() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }

        meta.setDisplayName("§5Delusion");
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to activate an 8s window.");
        lore.add("§7Then right-click a player to apply");
        lore.add("§7a fake infection that medic can see.");
        lore.add("§7Decays after 1 minute.");
        lore.add("§7Once per round.");

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }
    
    /**
     * Creates the Healing Touch (Cure) ability paper.
     */
    private ItemStack createHealingTouchPaper() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }
        
        meta.setDisplayName("§aHealing Touch");
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to activate.");
        lore.add("§7Then right-click an infected player to cure them.");
        lore.add("§7You have 8 seconds to use it.");
        lore.add("§7Once per round.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }
    
    /**
     * Creates the Healing Sight ability paper.
     */
    private ItemStack createHealingSightPaper() {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta == null) {
            return paper;
        }
        
        meta.setDisplayName("§aHealing Sight");
        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to see infected players.");
        lore.add("§7Shows subtle highlight particles on infected players.");
        lore.add("§7Lasts for 15 seconds.");
        lore.add("§7Once per round.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        paper.setItemMeta(meta);
        return makeUnmovable(paper);
    }
    
    /**
     * Creates a fixed crossbow that cannot be moved or dropped.
     */
    private ItemStack createFixedCrossbow() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();
        if (meta == null) {
            return crossbow;
        }
        
        meta.setDisplayName("§7Crossbow");
        List<String> lore = new ArrayList<>();
        lore.add("§7Use to shoot arrows.");
        lore.add("§7Reveals player nametags on hit.");
        lore.add("§7One arrow per round.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        crossbow.setItemMeta(meta);
        return makeUnmovable(crossbow);
    }
    
    /**
     * Creates a fixed arrow that cannot be moved or dropped.
     */
    private ItemStack createFixedArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = arrow.getItemMeta();
        if (meta == null) {
            return arrow;
        }
        
        meta.setDisplayName("§7Reveal Arrow");
        List<String> lore = new ArrayList<>();
        lore.add("§7Shoot a player to reveal their nametag.");
        lore.add("§7One use per round.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        arrow.setItemMeta(meta);
        return makeUnmovable(arrow);
    }
    
    /**
     * Makes an item unmovable and undroppable by adding custom NBT or using persistent data.
     * For now, we'll use a custom NBT tag that we'll check in event handlers.
     */
    private ItemStack makeUnmovable(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        // Add a persistent data container tag to mark it as game item
        // This will be checked in event handlers to prevent moving/dropping
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Checks if an item is a game item (should be fixed).
     */
    public static boolean isGameItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // Voting papers are game items but can be used (right-click)
        if (isVotingPaper(item)) {
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check if it's unbreakable (our marker for game items)
        // Also check material and lore
        if (meta.isUnbreakable()) {
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                // Game items have lore
                return true;
            }
        }
        
        // Also check by material and slot position in event handlers
        Material type = item.getType();
        return type == Material.PAPER || type == Material.CROSSBOW || type == Material.ARROW;
    }
    
    // Getters for slot constants (for use in listeners)
    public static int getRolePaperSlot() {
        return ROLE_PAPER_SLOT;
    }
    
    public static int getSwipeCurePaperSlot() {
        return SWIPE_CURE_PAPER_SLOT;
    }
    
    public static int getVisionSightPaperSlot() {
        return VISION_SIGHT_PAPER_SLOT;
    }
    
    public static int getCrossbowHotbarSlot() {
        return CROSSBOW_HOTBAR_SLOT;
    }
    
    public static int getArrowHotbarSlot() {
        return ARROW_HOTBAR_SLOT;
    }
    
    /**
     * Creates a gray dye item with the same metadata as the given paper item.
     * Used to indicate that an ability/vote has been used.
     */
    public static ItemStack createUsedIndicator(ItemStack paper) {
        if (paper == null) {
            return null;
        }
        
        ItemStack grayDye = new ItemStack(Material.GRAY_DYE);
        ItemMeta dyeMeta = grayDye.getItemMeta();
        ItemMeta paperMeta = paper.getItemMeta();
        
        if (dyeMeta == null || paperMeta == null) {
            return grayDye;
        }
        
        // Copy display name and lore from paper
        dyeMeta.setDisplayName(paperMeta.getDisplayName());
        if (paperMeta.getLore() != null) {
            List<String> lore = new ArrayList<>(paperMeta.getLore());
            // Add "Used" indicator at the end
            lore.add("§8§l[USED]");
            dyeMeta.setLore(lore);
        } else {
            List<String> lore = new ArrayList<>();
            lore.add("§8§l[USED]");
            dyeMeta.setLore(lore);
        }
        
        dyeMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        dyeMeta.setUnbreakable(true);
        // Preserve persistent data such as vote target UUID
        PersistentDataContainer source = paperMeta.getPersistentDataContainer();
        PersistentDataContainer target = dyeMeta.getPersistentDataContainer();
        if (VOTE_TARGET_KEY != null) {
            String rawId = source.get(VOTE_TARGET_KEY, PersistentDataType.STRING);
            if (rawId != null) {
                target.set(VOTE_TARGET_KEY, PersistentDataType.STRING, rawId);
            }
        }
        grayDye.setItemMeta(dyeMeta);
        
        return grayDye;
    }
}

