package fr.perso.zoe.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Trois rôles :
 * <ul>
 *   <li>ADMIN : tout ;</li>
 *   <li>USER : synthèse, recharges et kilométrage, en lecture et saisie (pas les tarifs, les lieux, les paramètres, l'import ni l'export) ;</li>
 *   <li>INVITE : consultation seule, attribué automatiquement aux appareils du réseau local sans session.</li>
 * </ul>
 * Connexion par formulaire sur /api/auth/login, session en base, protection CSRF par cookie XSRF-TOKEN (lu par Angular).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecuriteProperties.class)
public class SecuriteConfig {

    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    /** Appareil du réseau local sans session : consultation seule. */
    public static final String INVITE = "INVITE";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecuriteProperties props) throws Exception {
        InviteReseauLocalFilter invite = new InviteReseauLocalFilter(props.reseauLocal());
        http
                .csrf(csrf -> csrf.spa())
                // Pas de mémorisation de la requête refusée : évite de créer une session par visiteur anonyme
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterBefore(invite, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/tarifs/**", "/api/parametres/**", "/api/import/**", "/api/export/**").hasRole(ADMIN)
                        // Lieux de recharge : lecture pour tous (formulaire de recharge), écriture admin
                        .requestMatchers(HttpMethod.POST, "/api/lieux/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/lieux/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/lieux/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(ADMIN, USER, INVITE)
                        .requestMatchers("/api/**").hasAnyRole(ADMIN, USER)
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((req, res, auth) -> {
                            // Session administrateur courte : on retombe en consultation après une période d'inactivité
                            if (ADMIN.equals(role(auth)) && props.dureeAdminMinutes() > 0) {
                                req.getSession().setMaxInactiveInterval(props.dureeAdminMinutes() * 60);
                            }
                            ecrireUtilisateur(res, auth);
                        })
                        .failureHandler(SecuriteConfig::ecrireEchec))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(SecuriteProperties props, PasswordEncoder encoder,
                                                  TentativesConnexion tentatives) {
        List<UserDetails> comptes = new ArrayList<>();
        if (props.admin() == null || !props.admin().actif()) {
            throw new IllegalStateException(
                    "Compte administrateur non configuré : définir ADMIN_PASSWORD (et éventuellement ADMIN_USERNAME)");
        }
        comptes.add(User.withUsername(props.admin().identifiant().trim())
                .password(encoder.encode(props.admin().motDePasse())).roles(ADMIN).build());
        if (props.utilisateur() != null && props.utilisateur().actif()) {
            if (props.utilisateur().identifiant().trim().equalsIgnoreCase(props.admin().identifiant().trim())) {
                throw new IllegalStateException("USER_USERNAME doit être différent de ADMIN_USERNAME");
            }
            comptes.add(User.withUsername(props.utilisateur().identifiant().trim())
                    .password(encoder.encode(props.utilisateur().motDePasse())).roles(USER).build());
        }
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(comptes));
        dao.setPasswordEncoder(encoder);
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) {
                String identifiant = authentication.getName();
                if (tentatives.estBloque(identifiant)) {
                    throw new LockedException("Trop de tentatives : réessaie dans 15 minutes");
                }
                try {
                    Authentication resultat = dao.authenticate(authentication);
                    tentatives.succes(identifiant);
                    return resultat;
                } catch (BadCredentialsException e) {
                    tentatives.echec(identifiant);
                    throw e;
                }
            }

            @Override
            public boolean supports(Class<?> type) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
            }
        };
    }

    static String role(Authentication auth) {
        if (possede(auth, ADMIN)) return ADMIN;
        return possede(auth, INVITE) ? INVITE : USER;
    }

    private static boolean possede(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_" + role));
    }

    static String json(String identifiant, String role) {
        return "{\"identifiant\":\"" + echapper(identifiant) + "\",\"role\":\"" + role + "\"}";
    }

    /** Variante avec l'adresse vue par le serveur : sert à diagnostiquer la détection du réseau local. */
    static String json(String identifiant, String role, String ip) {
        return "{\"identifiant\":\"" + echapper(identifiant) + "\",\"role\":\"" + role
                + "\",\"ip\":\"" + echapper(ip) + "\"}";
    }

    private static void ecrireUtilisateur(HttpServletResponse res, Authentication auth) throws IOException {
        res.setStatus(HttpStatus.OK.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json(auth.getName(), role(auth)));
    }

    private static void ecrireEchec(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        boolean bloque = ex instanceof LockedException;
        res.setStatus(bloque ? HttpStatus.TOO_MANY_REQUESTS.value() : HttpStatus.UNAUTHORIZED.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        String message = bloque ? ex.getMessage() : "Identifiant ou mot de passe incorrect";
        res.getWriter().write("{\"message\":\"" + echapper(message) + "\"}");
    }

    private static String echapper(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Force l'écriture du cookie XSRF-TOKEN dès la première requête, pour que le POST de login puisse l'envoyer. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
