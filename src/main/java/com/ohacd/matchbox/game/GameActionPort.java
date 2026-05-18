package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.cosmetic.SkinManager;
import com.ohacd.matchbox.game.ability.HunterVisionAdapter;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Full action surface for listeners that trigger game state changes.
 *
 * <p>Extends {@link GameSessionQuery} with mutation methods. Ability listeners,
 * vote listeners, quit/sign listeners, and other action-triggering components
 * should accept this interface.</p>
 */
public interface GameActionPort extends GameSessionQuery {

    // ── Vote ─────────────────────────────────────────────────────────────────
    boolean handleVote(Player voter, Player target);

    // ── Swipe ability ────────────────────────────────────────────────────────
    Long startSwipeWindow(Player spark, int seconds);
    void endSwipeWindow(UUID playerId);
    boolean isSwipeWindowActive(UUID playerId);
    void handleSwipe(Player shooter, Player target);

    // ── Cure ability ─────────────────────────────────────────────────────────
    Long startCureWindow(Player medic, int seconds);
    void endCureWindow(UUID playerId);
    boolean isCureWindowActive(UUID playerId);
    void handleCure(Player medic, Player target);

    // ── Delusion ability ─────────────────────────────────────────────────────
    Long startDelusionWindow(Player spark, int seconds);
    void endDelusionWindow(UUID playerId);
    boolean isDelusionWindowActive(UUID playerId);
    void handleDelusion(Player spark, Player target);

    // ── Vision abilities ─────────────────────────────────────────────────────
    void activateHunterVision(Player spark);
    void activateHealingSight(Player medic);

    // ── Ability paper ────────────────────────────────────────────────────────
    void restoreAbilityPaper(Player player);
    void restoreSecondaryAbilityPaper(Player player);

    // ── Session lifecycle ────────────────────────────────────────────────────
    boolean checkForWin(String sessionName);
    void endGame(String sessionName);
    Set<String> getActiveSessionNames();

    // ── Logging ──────────────────────────────────────────────────────────────
    void logSessionEvent(String sessionName, String category, String message);
    void logSignMessage(String sessionName, UUID senderId, String senderName, String message);

    // ── Player restoration ───────────────────────────────────────────────────
    void restorePlayerNick(Player player);
    SkinManager getSkinManager();
    HunterVisionAdapter getHunterVisionAdapter();
}
