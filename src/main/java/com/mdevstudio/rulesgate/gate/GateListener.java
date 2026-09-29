package com.mdevstudio.rulesgate.gate;

import com.destroystokyo.paper.ClientOption;
import com.mdevstudio.rulesgate.RulesGate;
import com.mdevstudio.rulesgate.config.Messages;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class GateListener implements Listener {

    private final RulesGate plugin;

    public GateListener(RulesGate plugin) {
        this.plugin = plugin;
    }

    // Runs while the client is still in the configuration phase, before it has entered any world,
    // so a player who hasn't accepted the rules is never on the server at all.
    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID id = connection.getProfile().getId();
        int version = plugin.settings().rulesVersion();
        if (id == null || plugin.hasAccepted(plugin.getServer().getOfflinePlayer(id), version)) {
            return;
        }

        Duration timeout = plugin.settings().answerTimeout();
        CompletableFuture<Boolean> answer = new CompletableFuture<>();
        String locale = connection.getClientOption(ClientOption.LOCALE);
        connection.getAudience().showDialog(
                RulesDialogs.confirmation(plugin.rules().forLocale(locale), timeout, answer::complete));

        Boolean accepted = await(connection, answer, timeout);
        Messages messages = plugin.messages();
        if (accepted == null) {
            if (connection.isConnected()) {
                connection.disconnect(messages.component(Messages.TIMED_OUT));
            }
        } else if (!accepted) {
            connection.disconnect(messages.component(Messages.DECLINED));
        } else {
            connection.getAudience().closeDialog();
            plugin.acceptances().acceptBeforeJoin(id, new Acceptance(version, Instant.now()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.acceptances().saveWaiting(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.acceptances().forget(event.getPlayer().getUniqueId());
    }

    private static Boolean await(PlayerConfigurationConnection connection, CompletableFuture<Boolean> answer,
                                 Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        // Waiting in one-second slices frees the thread soon after a player quits with the dialog open.
        while (connection.isConnected() && System.nanoTime() < deadline) {
            try {
                return answer.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException ignored) {
                // No answer yet, check the connection again.
            } catch (ExecutionException e) {
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
