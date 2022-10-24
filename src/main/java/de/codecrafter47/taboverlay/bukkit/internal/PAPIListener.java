package de.codecrafter47.taboverlay.bukkit.internal;

import me.clip.placeholderapi.events.ExpansionRegisterEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.clip.placeholderapi.events.ExpansionsLoadedEvent;

public class PAPIListener implements Listener {
    
    private final Runnable callback;
    private final Runnable softReloadCallback;
    private boolean first = true;

    public PAPIListener(Runnable callback, Runnable softReloadCallback) {
        this.callback = callback;
        this.softReloadCallback = softReloadCallback;
    }

    @EventHandler
    public void onExpansionsLoaded(ExpansionsLoadedEvent event) {
        if (first) {
            callback.run();
            first = false;
        } else {
            softReloadCallback.run();
        }
    }
    
    @EventHandler
    public void onExpansionRegister(ExpansionRegisterEvent event) {
        if (!first) {
            softReloadCallback.run();
        }
    }
}
