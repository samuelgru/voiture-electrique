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
import java.util.Arrays;
import java.util.List;

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
 */
final class InviteReseauLocalFilter extends OncePerRequestFilter {

    static final String ENTETE = "X-Zoe-Chaine-Ip";
    static final String PRINCIPAL = "invité";

    private final List<IpAddressMatcher> reseaux;

    InviteReseauLocalFilter(String reseauxCidr) {
        this.reseaux = reseauxCidr == null ? List.of() : Arrays.stream(reseauxCidr.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(IpAddressMatcher::new).toList();
    }

    boolean actif() {
        return !reseaux.isEmpty();
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
        for (IpAddressMatcher m : reseaux) {
            try {
                if (m.matches(adresse)) return true;
            } catch (IllegalArgumentException e) {
                // adresse illisible : considérée comme non locale
            }
        }
        return false;
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
