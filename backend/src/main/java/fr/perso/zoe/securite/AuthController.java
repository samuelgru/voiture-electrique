package fr.perso.zoe.securite;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Utilisateur connecté ; 401 si personne (géré par Spring Security). */
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public String moi(Authentication auth) {
        return SecuriteConfig.json(auth.getName(), SecuriteConfig.role(auth));
    }
}
