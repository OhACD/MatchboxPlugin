package com.ohacd.matchbox.game.action;

import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.ParticleUtils;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.vote.VoteManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

/**
 * Handles player actions during gameplay (swipe, cure, vote).
 * Separated from GameManager to improve code organization.
 */
public class PlayerActionHandler {
    private final Plugin plugin;
    
    public PlayerActionHandler(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handles a swipe action from a player.
     */
    public void handleSwipe(SessionGameContext context, Player shooter, Player target) {
        if (context == null || shooter == null || target == null) return;
        
        GameState gameState = context.getGameState();
        
        // Game must be active
        if (!gameState.isGameActive()) return;
        
        // Both players must be in the same session
        if (!gameState.getAllParticipatingPlayerIds().contains(target.getUniqueId())) {
            return;
        }
        
        com.ohacd.matchbox.game.phase.PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeSwipeWindow = context.getActiveSwipeWindow();
        
        // Phase and permission checks
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        
        UUID shooterId = shooter.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Only a Spark can swipe
        if (gameState.getRole(shooterId) != Role.SPARK) {
            return;
        }
        
        // Must have an active swipe window
        if (!isSwipeWindowActive(context, shooterId)) {
            return;
        }
        
        // If target already has pending death, ignore duplicate
        if (gameState.hasPendingDeath(targetId)) {
            activeSwipeWindow.remove(shooterId);
            return;
        }
        
        // Mark as swiped and infected
        gameState.markSwiped(shooterId);
        gameState.markInfected(targetId);
        gameState.setPendingDeath(targetId, System.currentTimeMillis());
        
        // Show subtle lime particles
        ParticleUtils.showColoredParticlesToEveryone(
            target,
            org.bukkit.Color.fromRGB(50, 205, 50), // Lime green
            8,
            plugin
        );
        
        // Close the swipe window
        activeSwipeWindow.remove(shooterId);
        
        plugin.getLogger().info("Swipe registered in session '" + context.getSessionName() + "': " + shooter.getName() + " swiped " + target.getName());
    }
    
    /**
     * Handles a cure action from a medic.
     */
    public void handleCure(SessionGameContext context, Player medic, Player target) {
        if (context == null || medic == null || target == null) return;
        
        GameState gameState = context.getGameState();
        
        // Game must be active
        if (!gameState.isGameActive()) return;
        
        // Both players must be in the same session
        if (!gameState.getAllParticipatingPlayerIds().contains(target.getUniqueId())) {
            return;
        }
        
        PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeCureWindow = context.getActiveCureWindow();
        
        // Phase and permission checks
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        
        UUID medicId = medic.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Only a Medic can cure
        if (gameState.getRole(medicId) != Role.MEDIC) {
            return;
        }
        
        // Must have an active cure window
        if (!isCureWindowActive(context, medicId)) {
            return;
        }
        
        // Check if target has a pending death OR delusion infection to cure
        boolean hasRealInfection = gameState.hasPendingDeath(targetId);
        boolean hasDelusionInfection = gameState.isDelusionInfected(targetId);
        
        if (!hasRealInfection && !hasDelusionInfection) {
            activeCureWindow.remove(medicId);
            return;
        }
        
        // Mark as cured
        gameState.markCured(medicId);
        
        // Remove real infection if present
        if (hasRealInfection) {
            gameState.removePendingDeath(targetId);
            // NEW: 0.8.7 - mark the target as been cured
            gameState.markBeenCured(targetId);
        }
        
        // Remove delusion infection if present (wastes the cure)
        if (hasDelusionInfection) {
            gameState.removeDelusionInfection(targetId);
        }
        
        // Show subtle blue particles
        ParticleUtils.showColoredParticlesToEveryone(
            target,
            org.bukkit.Color.fromRGB(0, 100, 255), // Blue
            8,
            plugin
        );
        
        // Close the cure window
        activeCureWindow.remove(medicId);
        
        plugin.getLogger().info("Medic " + medic.getName() + " cured " + target.getName() + " in session '" + context.getSessionName() + "'");
    }
    
    /**
     * Handles a vote action from a player.
     */
    public boolean handleVote(SessionGameContext context, Player voter, Player target) {
        if (context == null || voter == null || target == null) return false;
        
        GameState gameState = context.getGameState();
        
        // Game must be active
        if (!gameState.isGameActive()) return false;
        
        // Both players must be in the same session
        if (!gameState.getAllParticipatingPlayerIds().contains(target.getUniqueId())) {
            return false;
        }
        
        com.ohacd.matchbox.game.phase.PhaseManager phaseManager = context.getPhaseManager();
        VoteManager voteManager = context.getVoteManager();
        
        // Phase check
        if (!phaseManager.isPhase(GamePhase.VOTING)) {
            return false;
        }
        
        UUID voterId = voter.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Check if voter and target are alive
        if (!gameState.isAlive(voterId) || !gameState.isAlive(targetId)) {
            return false;
        }
        
        // Register the vote
        boolean success = voteManager.registerVote(voterId, targetId);
        
        if (success) {
            plugin.getLogger().info("Vote registered in session '" + context.getSessionName() + "': " + voter.getName() + " voted for " + target.getName());
        }
        
        return success;
    }
    
    /**
     * Checks if a player has an active swipe window.
     */
    public boolean isSwipeWindowActive(SessionGameContext context, UUID playerId) {
        if (context == null) return false;
        Map<UUID, Long> activeSwipeWindow = context.getActiveSwipeWindow();
        Long expiry = activeSwipeWindow.get(playerId);
        if (expiry == null) return false;
        if (expiry <= System.currentTimeMillis()) {
            activeSwipeWindow.remove(playerId);
            return false;
        }
        return true;
    }
    
    /**
     * Checks if a player has an active cure window.
     */
    public boolean isCureWindowActive(SessionGameContext context, UUID playerId) {
        if (context == null) return false;
        Map<UUID, Long> activeCureWindow = context.getActiveCureWindow();
        Long expiry = activeCureWindow.get(playerId);
        if (expiry == null) return false;
        if (expiry <= System.currentTimeMillis()) {
            activeCureWindow.remove(playerId);
            return false;
        }
        return true;
    }
    
    /**
     * Handles a delusion action from a spark.
     * Applies a fake infection that medic can see but doesn't cause elimination.
     */
    public void handleDelusion(SessionGameContext context, Player spark, Player target) {
        if (context == null || spark == null || target == null) return;
        
        GameState gameState = context.getGameState();
        
        // Game must be active
        if (!gameState.isGameActive()) return;
        
        // Both players must be in the same session
        if (!gameState.getAllParticipatingPlayerIds().contains(target.getUniqueId())) {
            return;
        }
        
        PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeDelusionWindow = context.getActiveDelusionWindow();
        
        // Phase and permission checks
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        
        UUID sparkId = spark.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Only a Spark can use delusion
        if (gameState.getRole(sparkId) != Role.SPARK) {
            return;
        }
        
        // Must have an active delusion window
        if (!isDelusionWindowActive(context, sparkId)) {
            return;
        }
        
        // If target already has delusion infection, ignore duplicate
        if (gameState.isDelusionInfected(targetId)) {
            activeDelusionWindow.remove(sparkId);
            return;
        }
        
        // Mark as used and apply delusion infection
        gameState.markUsedDelusion(sparkId);
        gameState.markDelusionInfected(targetId);
        
        // Show subtle lime particles (same as real infection to fool medic)
        ParticleUtils.showColoredParticlesToEveryone(
            target,
            org.bukkit.Color.fromRGB(50, 205, 50), // Lime green
            8,
            plugin
        );
        
        // Close the delusion window
        activeDelusionWindow.remove(sparkId);
        
        // Schedule decay after 1 minute (60 seconds)
        final UUID targetIdFinal = targetId;
        final String sessionName = context.getSessionName();
        final SessionGameContext contextFinal = context;
        org.bukkit.scheduler.BukkitRunnable decayTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                // Check if game is still active and player still has delusion infection
                if (contextFinal == null || !contextFinal.getGameState().isGameActive()) {
                    return;
                }
                
                GameState state = contextFinal.getGameState();
                if (state.isDelusionInfected(targetIdFinal)) {
                    // Remove delusion infection after 1 minute
                    state.removeDelusionInfection(targetIdFinal);
                    plugin.getLogger().info("Delusion infection decayed for player " + targetIdFinal + " in session '" + sessionName + "'");
                }
            }
        };
        decayTask.runTaskLater(plugin, 20L * 60); // 60 seconds = 1200 ticks
        
        plugin.getLogger().info("Delusion registered in session '" + context.getSessionName() + "': " + spark.getName() + " applied delusion to " + target.getName() + " (will decay in 1 minute)");
    }
    
    /**
     * Checks if a player has an active delusion window.
     */
    public boolean isDelusionWindowActive(SessionGameContext context, UUID playerId) {
        if (context == null) return false;
        Map<UUID, Long> activeDelusionWindow = context.getActiveDelusionWindow();
        Long expiry = activeDelusionWindow.get(playerId);
        if (expiry == null) return false;
        if (expiry <= System.currentTimeMillis()) {
            activeDelusionWindow.remove(playerId);
            return false;
        }
        return true;
    }
}

