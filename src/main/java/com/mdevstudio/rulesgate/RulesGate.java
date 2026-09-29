package com.mdevstudio.rulesgate;

import com.mdevstudio.rulesgate.config.Messages;
import com.mdevstudio.rulesgate.config.PluginConfig;
import com.mdevstudio.rulesgate.config.Rules;
import com.mdevstudio.rulesgate.gate.Acceptance;
import com.mdevstudio.rulesgate.gate.AcceptanceStore;
import com.mdevstudio.rulesgate.gate.GateListener;
import com.mdevstudio.rulesgate.gate.RulesDialogs;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Instant;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RulesGate extends JavaPlugin {

    private PluginConfig settings;
    private Messages messages;
    private Rules rules;
    private AcceptanceStore acceptances;

    @Override
    public void onEnable() {
        acceptances = new AcceptanceStore(this);
        reload();
        getServer().getPluginManager().registerEvents(new GateListener(this), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> RulesCommand.register(event.registrar(), this));
    }

    /**
     * Reloads everything and asks online players who accepted an older version to accept the current one.
     *
     * @return how many players were asked
     */
    public int reload() {
        settings = PluginConfig.load(this);
        messages = Messages.load(this, settings.language());
        rules = Rules.load(this, settings.language());

        int asked = 0;
        for (Player player : getServer().getOnlinePlayers()) {
            if (!hasAccepted(player, settings.rulesVersion())) {
                askInGame(player);
                asked++;
            }
        }
        return asked;
    }

    public boolean hasAccepted(OfflinePlayer player, int version) {
        Acceptance acceptance = acceptances.read(player);
        return acceptance != null && acceptance.version() >= version;
    }

    public PluginConfig settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public Rules rules() {
        return rules;
    }

    public AcceptanceStore acceptances() {
        return acceptances;
    }

    private void askInGame(Player player) {
        int version = settings.rulesVersion();
        player.showDialog(RulesDialogs.confirmation(rules.forLocale(player.locale().toString()),
                settings.answerTimeout(), accepted -> getServer().getScheduler().runTask(this, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (accepted) {
                        acceptances.accept(player, new Acceptance(version, Instant.now()));
                        player.closeDialog();
                    } else {
                        player.kick(messages.component(Messages.DECLINED));
                    }
                })));
    }
}
