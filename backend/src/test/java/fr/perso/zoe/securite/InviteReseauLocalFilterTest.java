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
}
