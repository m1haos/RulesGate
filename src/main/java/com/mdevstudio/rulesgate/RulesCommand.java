package com.mdevstudio.rulesgate;

import com.mdevstudio.rulesgate.config.Messages;
import com.mdevstudio.rulesgate.gate.Acceptance;
import com.mdevstudio.rulesgate.gate.RulesDialogs;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class RulesCommand {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private RulesCommand() {
    }

    static void register(Commands registrar, RulesGate plugin) {
        var root = Commands.literal("rules")
                .requires(source -> source.getSender().hasPermission(Permissions.RULES))
                .executes(context -> show(plugin, context.getSource()))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(Permissions.RELOAD))
                        .executes(context -> reload(plugin, context.getSource())))
                .then(Commands.literal("check")
                        .requires(source -> source.getSender().hasPermission(Permissions.CHECK))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String typed = builder.getRemainingLowerCase();
                                    plugin.getServer().getOnlinePlayers().stream()
                                            .map(Player::getName)
                                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> check(plugin, context.getSource(),
                                        StringArgumentType.getString(context, "player")))));

        registrar.register(root.build(), "Server rules", List.of());
    }

    private static int show(RulesGate plugin, CommandSourceStack source) {
        if (source.getSender() instanceof Player player) {
            player.showDialog(RulesDialogs.reading(plugin.rules().forLocale(player.locale().toString())));
        } else {
            plugin.messages().send(source.getSender(), Messages.PLAYERS_ONLY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(RulesGate plugin, CommandSourceStack source) {
        int asked = plugin.reload();
        if (asked > 0) {
            plugin.messages().send(source.getSender(), Messages.RELOADED_NEW_VERSION,
                    Placeholder.unparsed("version", String.valueOf(plugin.settings().rulesVersion())),
                    Placeholder.unparsed("players", String.valueOf(asked)));
        } else {
            plugin.messages().send(source.getSender(), Messages.RELOADED);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int check(RulesGate plugin, CommandSourceStack source, String name) {
        OfflinePlayer target = plugin.getServer().getOfflinePlayerIfCached(name);
        if (target == null) {
            plugin.messages().send(source.getSender(), Messages.PLAYER_NOT_FOUND, Placeholder.unparsed("player", name));
            return 0;
        }

        var player = Placeholder.unparsed("player", target.getName() != null ? target.getName() : name);
        Acceptance acceptance = plugin.acceptances().read(target);
        if (acceptance == null) {
            plugin.messages().send(source.getSender(), Messages.CHECK_NEVER, player);
            return Command.SINGLE_SUCCESS;
        }

        int current = plugin.settings().rulesVersion();
        plugin.messages().send(source.getSender(),
                acceptance.version() >= current ? Messages.CHECK_CURRENT : Messages.CHECK_OUTDATED,
                player,
                Placeholder.unparsed("version", String.valueOf(acceptance.version())),
                Placeholder.unparsed("current", String.valueOf(current)),
                Placeholder.unparsed("date", DATE.format(acceptance.acceptedAt())));
        return Command.SINGLE_SUCCESS;
    }
}
