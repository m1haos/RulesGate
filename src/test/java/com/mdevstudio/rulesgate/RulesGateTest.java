package com.mdevstudio.rulesgate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.rulesgate.gate.Acceptance;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class RulesGateTest {

    private static final Instant ACCEPTED_AT = Instant.parse("2026-09-01T12:00:00Z");

    private ServerMock server;
    private RulesGate plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(RulesGate.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void rulesFollowTheClientLanguage() {
        assertEquals("Правила сервера", plain(plugin.rules().forLocale("ru_ru").title()));
        assertEquals("Server rules", plain(plugin.rules().forLocale("en_GB").title()));
    }

    @Test
    void unknownLanguageGetsTheConfiguredOne() {
        assertEquals("Server rules", plain(plugin.rules().forLocale("de_de").title()));
        assertEquals("Server rules", plain(plugin.rules().forLocale(null).title()));
    }

    @Test
    void addedRulesFileIsPickedUpOnReload() throws IOException {
        Path german = plugin.getDataFolder().toPath().resolve("rules/de.yml");
        Files.writeString(german, "title: \"Serverregeln\"\nbody:\n  - \"Sei nett.\"\n");

        plugin.reload();

        assertEquals("Serverregeln", plain(plugin.rules().forLocale("de_de").title()));
    }

    @Test
    void acceptanceGivenBeforeJoiningIsSavedWithThePlayer() {
        PlayerMock player = new PlayerMock(server, "Alex");
        plugin.acceptances().acceptBeforeJoin(player.getUniqueId(), new Acceptance(1, ACCEPTED_AT));

        server.addPlayer(player);
        plugin.acceptances().forget(player.getUniqueId());

        assertEquals(new Acceptance(1, ACCEPTED_AT), plugin.acceptances().read(player));
        assertTrue(plugin.hasAccepted(player, 1));
    }

    @Test
    void olderAcceptanceDoesNotCountForNewRules() {
        PlayerMock player = server.addPlayer();
        plugin.acceptances().accept(player, new Acceptance(1, ACCEPTED_AT));

        assertTrue(plugin.hasAccepted(player, 1));
        assertFalse(plugin.hasAccepted(player, 2));
    }

    @Test
    void playerWhoNeverAcceptedHasNoRecord() {
        PlayerMock player = server.addPlayer();

        assertNull(plugin.acceptances().read(player));
        assertFalse(plugin.hasAccepted(player, 1));
    }

    @Test
    void checkCommandTellsWhichVersionWasAccepted() {
        PlayerMock admin = server.addPlayer("Admin");
        admin.setOp(true);
        PlayerMock player = server.addPlayer("Steve");

        server.dispatchCommand(admin, "rules check Steve");
        assertTrue(nextMessage(admin).contains("has not accepted the rules yet"));

        plugin.acceptances().accept(player, new Acceptance(1, ACCEPTED_AT));
        server.dispatchCommand(admin, "rules check Steve");
        assertTrue(nextMessage(admin).contains("accepted the current rules (version 1)"));
    }

    private static String nextMessage(PlayerMock player) {
        Component message = player.nextComponentMessage();
        return message != null ? plain(message) : "";
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
