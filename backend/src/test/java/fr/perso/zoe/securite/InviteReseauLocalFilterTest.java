package fr.perso.zoe.securite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class InviteReseauLocalFilterTest {

    private final InviteReseauLocalFilter filtre = new InviteReseauLocalFilter("192.168.1.0/24");

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appareilLocalDirect() {
        assertThat(filtre.estLocal(",192.168.1.42")).isTrue();
    }

    @Test
    void appareilLocalViaReverseProxyDuNas() {
        // client local, puis le NAS (192.168.1.10) qui se connecte à nginx
        assertThat(filtre.estLocal("192.168.1.42,192.168.1.10")).isTrue();
    }

    @Test
    void visiteurInternetViaReverseProxyNEstPasLocal() {
        assertThat(filtre.estLocal("203.0.113.9,192.168.1.10")).isFalse();
    }

    @Test
    void enTeteFalsifieParUnVisiteurInternetNEstPasLocal() {
        // le client envoie X-Forwarded-For: 192.168.1.5 ; le reverse proxy ajoute sa vraie adresse
        assertThat(filtre.estLocal("192.168.1.5,203.0.113.9,192.168.1.10")).isFalse();
    }

    @Test
    void autreReseauOuAdresseIllisibleOuVide() {
        assertThat(filtre.estLocal(",192.168.2.42")).isFalse();
        assertThat(filtre.estLocal(",10.0.0.1")).isFalse();
        assertThat(filtre.estLocal(",pas-une-ip")).isFalse();
        assertThat(filtre.estLocal(",")).isFalse();
        assertThat(filtre.estLocal(null)).isFalse();
    }

    @Test
    void plusieursReseaux() {
        InviteReseauLocalFilter f = new InviteReseauLocalFilter("192.168.1.0/24, 10.0.0.0/8");
        assertThat(f.estLocal("10.1.2.3,192.168.1.10")).isTrue();
    }

    @Test
    void desactiveSansReseau() {
        assertThat(new InviteReseauLocalFilter("").actif()).isFalse();
        assertThat(new InviteReseauLocalFilter(null).actif()).isFalse();
    }

    private Object roleApresFiltre(InviteReseauLocalFilter f, String chaine) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(InviteReseauLocalFilter.ENTETE, chaine);
        f.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void attribueLeRoleInviteAuxAppareilsLocaux() throws Exception {
        var auth = (UsernamePasswordAuthenticationToken) roleApresFiltre(filtre, ",192.168.1.42");
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_INVITE");
    }

    @Test
    void neFaitRienPourUnAppareilDistantOuSiLeFiltreEstInactif() throws Exception {
        assertThat(roleApresFiltre(filtre, "203.0.113.9,192.168.1.10")).isNull();
        SecurityContextHolder.clearContext();
        assertThat(roleApresFiltre(new InviteReseauLocalFilter(""), ",192.168.1.42")).isNull();
    }

    @Test
    void neRemplacePasUneSessionExistante() throws Exception {
        var admin = UsernamePasswordAuthenticationToken.authenticated("admin", null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(admin);
        assertThat(roleApresFiltre(filtre, ",192.168.1.42")).isSameAs(admin);
    }

    // ------------------------------------------------------------ nom de domaine (hairpin NAT de la box)

    private final java.util.concurrent.atomic.AtomicReference<java.time.Instant> maintenant =
            new java.util.concurrent.atomic.AtomicReference<>(java.time.Instant.parse("2026-11-24T10:00:00Z"));
    private final java.time.Clock horloge = new java.time.Clock() {
        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public java.time.Instant instant() { return maintenant.get(); }
    };
    private final java.util.concurrent.atomic.AtomicInteger appels = new java.util.concurrent.atomic.AtomicInteger();
    private volatile String adressePubliqueActuelle = "198.51.100.7";
    private volatile boolean dnsEnPanne = false;

    private java.util.Set<java.net.InetAddress> resoudre(String nom) {
        appels.incrementAndGet();
        if (dnsEnPanne) throw new IllegalStateException("DNS indisponible");
        try {
            return java.util.Set.of(java.net.InetAddress.getByName(adressePubliqueActuelle));
        } catch (java.net.UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private InviteReseauLocalFilter avecDomaine() {
        return new InviteReseauLocalFilter("172.30.0.1/32, zoe.exemple.me", this::resoudre, horloge,
                java.time.Duration.ofMinutes(5));
    }

    @Test
    void accepteLAdressePubliqueDuDomaineVueApresUnHairpin() {
        // téléphone chez soi, via l'URL publique : la box présente son adresse publique
        assertThat(avecDomaine().estLocal("198.51.100.7,172.30.0.1")).isTrue();
    }

    @Test
    void refuseUnVisiteurInternetMemeAvecLAdresseDuDomaineFalsifiee() {
        // le visiteur falsifie X-Forwarded-For avec l'adresse publique du NAS ; le proxy ajoute sa vraie adresse
        assertThat(avecDomaine().estLocal("198.51.100.7,203.0.113.9,172.30.0.1")).isFalse();
        assertThat(avecDomaine().estLocal("203.0.113.9,172.30.0.1")).isFalse();
    }

    @Test
    void suitLesChangementsDAdresseDuDomaine() {
        InviteReseauLocalFilter f = avecDomaine();
        assertThat(f.estLocal("198.51.100.7,172.30.0.1")).isTrue();
        adressePubliqueActuelle = "198.51.100.99";
        // dans la période de validité : ancienne adresse encore connue
        assertThat(f.estLocal("198.51.100.7,172.30.0.1")).isTrue();
        maintenant.set(maintenant.get().plusSeconds(301));
        assertThat(f.estLocal("198.51.100.7,172.30.0.1")).isFalse();
        assertThat(f.estLocal("198.51.100.99,172.30.0.1")).isTrue();
    }

    @Test
    void gardeLesDernieresAdressesSiLeDnsEstEnPanne() {
        InviteReseauLocalFilter f = avecDomaine();
        assertThat(f.estLocal("198.51.100.7,172.30.0.1")).isTrue();
        dnsEnPanne = true;
        maintenant.set(maintenant.get().plusSeconds(301));
        assertThat(f.estLocal("198.51.100.7,172.30.0.1")).isTrue();
    }

    @Test
    void neResoutJamaisUnNomVenantDeLEnTeteDuClient() {
        InviteReseauLocalFilter f = avecDomaine();
        assertThat(f.estLocal("evil.example.com,172.30.0.1")).isFalse();
        // le seul appel de résolution est celui du domaine configuré, pas celui du nom reçu
        assertThat(appels.get()).isLessThanOrEqualTo(1);
    }

    @Test
    void sansDomaineAucuneResolution() {
        assertThat(filtre.estLocal("198.51.100.7")).isFalse();
        assertThat(appels.get()).isZero();
    }
}
