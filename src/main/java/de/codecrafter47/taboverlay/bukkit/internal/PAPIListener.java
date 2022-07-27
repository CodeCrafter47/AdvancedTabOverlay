package de.codecrafter47.taboverlay.bukkit.internal;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.clip.placeholderapi.events.ExpansionsLoadedEvent;

public class PAPIListener implements Listener {
    
    private final Runnable callback;
    private boolean first = true;

    public PAPIListener(Runnable callback) {
        this.callback = callback;
    }

    @EventHandler
    public void onExpansionsLoaded(ExpansionsLoadedEvent event) {
        if (first) {
            callback.run();
            first = false;
        }
    }
}
