package fr.perso.zoe.securite;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Comptes de connexion, définis par variables d'environnement (ADMIN_*, USER_*).
 *
 * @param reseauLocal            réseaux (CIDR séparés par des virgules, ex. 192.168.1.0/24) dont les appareils sont
 *                               connectés automatiquement en consultation ; vide = fonction désactivée
 * @param dureeSessionAdminMinutes inactivité avant la fin d'une session administrateur (0 = durée normale, 30 jours)
 */
@ConfigurationProperties("zoe.securite")
public record SecuriteProperties(Compte admin, Compte utilisateur, String reseauLocal, Integer dureeSessionAdminMinutes) {

    public int dureeAdminMinutes() {
        return dureeSessionAdminMinutes == null ? 30 : Math.max(0, dureeSessionAdminMinutes);
    }

    public record Compte(String identifiant, String motDePasse) {
        boolean actif() {
            return identifiant != null && !identifiant.isBlank() && motDePasse != null && !motDePasse.isBlank();
        }
    }
}
