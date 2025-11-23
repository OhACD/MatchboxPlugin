package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.logging.Logger;

public class ChatListener implements Listener {
    private final HologramManager hologramManager;
    private final GameManager gameManager;

    public ChatListener(HologramManager manager, GameManager gameManager) {
        this.hologramManager = manager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        // Check using isAsynchronous() player triggers run asynchronously
        if (!event.isAsynchronous()) return;
        
        // Only use holograms during SWIPE phase
        // In DISCUSSION, VOTING, and other phases, use normal chat
        if (gameManager.phaseManager.getCurrentPhase() != GamePhase.SWIPE) {
            // Normal chat - don't cancel, let it work normally
            return;
        }

        // During SWIPE phase: Cancel normal chat and show hologram instead
        event.setCancelled(true);
        // Render the message over the player for 100 ticks or 5 seconds
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
        Logger.getLogger(msg);
        hologramManager.showTextAbove(event.getPlayer(), msg, 100);
    }
}
