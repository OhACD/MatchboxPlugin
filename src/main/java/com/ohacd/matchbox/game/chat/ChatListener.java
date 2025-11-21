package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.game.hologram.HologramManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final HologramManager hologramManager;

    public ChatListener(HologramManager manager) {
        this.hologramManager = manager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) { // Check using isAsynchronous() player triggers run asynchronously
        // Cancel normal chat
        event.setCancelled(true);

        // Render the message over the player for 100 ticks or 5 seconds
        String msg = event.message().toString();
        hologramManager.showTextAbove(event.getPlayer(), msg, 100);
    }
}
