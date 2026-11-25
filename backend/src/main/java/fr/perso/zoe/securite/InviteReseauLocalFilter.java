package fr.perso.zoe.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Connecte automatiquement en consultation (rôle INVITE) les appareils du réseau local qui n'ont pas de session.
 * <p>
 * Le mode invité n'est jamais enregistré en session : il est recalculé à chaque requête. Se connecter avec un compte
 * (par ex. administrateur) crée une session, qui prend le pas ; la déconnexion ramène en mode invité.
 * <p>
 * Sécurité : nginx écrit l'en-tête {@value #ENTETE} (adresses de toute la chaîne de proxys, ajoutées à ce que le client
 * a envoyé, jamais retirées) et le backend n'est pas joignable sans passer par nginx. L'appareil n'est local que si
 * <strong>toutes</strong> les adresses de la chaîne sont dans les réseaux autorisés : un visiteur venu d'Internet par le
 * reverse proxy du NAS apporte son adresse publique et n'est donc jamais considéré comme local, même s'il falsifie
 * X-Forwarded-For.
 * <p>
 * Un élément de la configuration peut être un nom de domaine (ex. le nom DynDNS du NAS) : ses adresses sont résolues
 * périodiquement. Utile quand la box renvoie l'adresse publique pour un accès local par ce nom (hairpin NAT) : seuls les
 * appareils derrière cette box ont alors cette adresse source.
 */
final class InviteReseauLocalFilter extends OncePerRequestFilter {

    static final String ENTETE = "X-Zoe-Chaine-Ip";
    static final String PRINCIPAL = "invité";

    /** Adresse IPv4 ou IPv6 en toutes lettres (jamais de nom : on ne résout rien venant d'un en-tête du client). */
    private static final Pattern ADRESSE = Pattern.compile("[0-9a-fA-F:.]{2,45}");
    private static final Pattern CIDR = Pattern.compile("[0-9a-fA-F:.]{2,45}(/\\d{1,3})?");

    private final List<IpAddressMatcher> reseaux;
    private final List<HoteDynamique> hotes;

    InviteReseauLocalFilter(String configuration) {
        this(configuration, nom -> {
            try {
                return new HashSet<>(Arrays.asList(InetAddress.getAllByName(nom)));
            } catch (java.net.UnknownHostException e) {
                throw new IllegalStateException(e);
            }
        }, Clock.systemUTC(), Duration.ofMinutes(5));
    }

    InviteReseauLocalFilter(String configuration, Function<String, Set<InetAddress>> resolveur, Clock clock,
                            Duration validite) {
        List<String> elements = configuration == null ? List.<String>of() : Arrays.stream(configuration.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.reseaux = elements.stream().filter(e -> CIDR.matcher(e).matches()).map(IpAddressMatcher::new).toList();
        this.hotes = elements.stream().filter(e -> !CIDR.matcher(e).matches())
                .map(nom -> new HoteDynamique(nom, resolveur, clock, validite)).toList();
    }

    boolean actif() {
        return !reseaux.isEmpty() || !hotes.isEmpty();
    }

    /** Adresses de la chaîne de la requête : celles de l'en-tête nginx, sinon l'adresse de la connexion. */
    static String chaine(HttpServletRequest request) {
        String entete = request.getHeader(ENTETE);
        return entete != null ? entete : request.getRemoteAddr();
    }

    boolean estLocal(String chaine) {
        if (chaine == null) return false;
        List<String> adresses = Arrays.stream(chaine.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return !adresses.isEmpty() && adresses.stream().allMatch(this::dansUnReseau);
    }

    private boolean dansUnReseau(String adresse) {
        if (!ADRESSE.matcher(adresse).matches()) return false;
        for (IpAddressMatcher m : reseaux) {
            try {
                if (m.matches(adresse)) return true;
            } catch (IllegalArgumentException e) {
                // adresse illisible : considérée comme non locale
            }
        }
        if (hotes.isEmpty()) return false;
        try {
            InetAddress a = InetAddress.getByName(adresse); // adresse en toutes lettres : pas de résolution DNS
            return hotes.stream().anyMatch(h -> h.adresses().contains(a));
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }

    /** Adresses d'un nom de domaine, résolues au plus une fois par période ; en cas d'échec on garde les dernières connues. */
    private final class HoteDynamique {
        private final String nom;
        private final Function<String, Set<InetAddress>> resolveur;
        private final Clock clock;
        private final Duration validite;
        private Set<InetAddress> adresses = Set.of();
        private Instant expiration = Instant.MIN;

        HoteDynamique(String nom, Function<String, Set<InetAddress>> resolveur, Clock clock, Duration validite) {
            this.nom = nom;
            this.resolveur = resolveur;
            this.clock = clock;
            this.validite = validite;
        }

        synchronized Set<InetAddress> adresses() {
            Instant maintenant = clock.instant();
            if (maintenant.isAfter(expiration)) {
                try {
                    adresses = resolveur.apply(nom);
                    expiration = maintenant.plus(validite);
                } catch (RuntimeException e) {
                    logger.warn("Résolution de \"" + nom + "\" impossible, dernières adresses conservées : " + e.getMessage());
                    expiration = maintenant.plus(Duration.ofSeconds(30));
                }
            }
            return adresses;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (actif() && SecurityContextHolder.getContext().getAuthentication() == null && estLocal(chaine(request))) {
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_" + SecuriteConfig.INVITE))));
        }
        chain.doFilter(request, response);
    }
}
