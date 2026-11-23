package fr.perso.zoe.securite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Comptes de connexion, définis par variables d'environnement (ADMIN_*, USER_*). */
@ConfigurationProperties("zoe.securite")
public record SecuriteProperties(Compte admin, Compte utilisateur) {

    public record Compte(String identifiant, String motDePasse) {
        boolean actif() {
            return identifiant != null && !identifiant.isBlank() && motDePasse != null && !motDePasse.isBlank();
        }
    }
}
