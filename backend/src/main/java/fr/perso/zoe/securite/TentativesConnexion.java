package fr.perso.zoe.securite;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bloque un identifiant pendant 15 minutes après 5 mots de passe erronés consécutifs. */
@Component
public class TentativesConnexion {

    static final int MAX_ECHECS = 5;
    static final Duration BLOCAGE = Duration.ofMinutes(15);

    private record Etat(int echecs, Instant bloqueJusqua) {}

    private final Map<String, Etat> etats = new ConcurrentHashMap<>();
    private final Clock clock;

    public TentativesConnexion() {
        this(Clock.systemUTC());
    }

    TentativesConnexion(Clock clock) {
        this.clock = clock;
    }

    public boolean estBloque(String identifiant) {
        Etat e = etats.get(cle(identifiant));
        return e != null && e.bloqueJusqua() != null && clock.instant().isBefore(e.bloqueJusqua());
    }

    public void echec(String identifiant) {
        etats.compute(cle(identifiant), (k, e) -> {
            int echecs = (e == null || (e.bloqueJusqua() != null && !clock.instant().isBefore(e.bloqueJusqua())))
                    ? 1 : e.echecs() + 1;
            return new Etat(echecs, echecs >= MAX_ECHECS ? clock.instant().plus(BLOCAGE) : null);
        });
    }

    public void succes(String identifiant) {
        etats.remove(cle(identifiant));
    }

    private static String cle(String identifiant) {
        return identifiant == null ? "" : identifiant.trim().toLowerCase(Locale.ROOT);
    }
}
