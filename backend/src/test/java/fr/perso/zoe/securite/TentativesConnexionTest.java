package fr.perso.zoe.securite;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TentativesConnexionTest {

    private final AtomicReference<Instant> maintenant = new AtomicReference<>(Instant.parse("2026-11-23T10:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant.get(); }
    };
    private final TentativesConnexion tentatives = new TentativesConnexion(clock);

    @Test
    void bloqueApresCinqEchecsPuisDebloqueApres15Minutes() {
        for (int i = 0; i < 4; i++) tentatives.echec("Admin");
        assertThat(tentatives.estBloque("admin")).isFalse();
        tentatives.echec("admin");
        assertThat(tentatives.estBloque("ADMIN")).isTrue();

        maintenant.set(maintenant.get().plus(Duration.ofMinutes(15)));
        assertThat(tentatives.estBloque("admin")).isFalse();
        tentatives.echec("admin");
        assertThat(tentatives.estBloque("admin")).isFalse();
    }

    @Test
    void unSuccesRemetLeCompteurAZero() {
        for (int i = 0; i < 4; i++) tentatives.echec("admin");
        tentatives.succes("admin");
        tentatives.echec("admin");
        assertThat(tentatives.estBloque("admin")).isFalse();
    }
}
