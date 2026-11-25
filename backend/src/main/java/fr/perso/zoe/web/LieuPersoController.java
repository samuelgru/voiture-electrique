package fr.perso.zoe.web;

import fr.perso.zoe.model.LieuPerso;
import fr.perso.zoe.repository.LieuPersoRepository;
import fr.perso.zoe.repository.RechargeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Lieux de recharge supplémentaires. Lecture pour tous (formulaire de recharge), écriture réservée à l'admin. */
@RestController
@RequestMapping("/api/lieux")
public class LieuPersoController {

    private final LieuPersoRepository repository;
    private final RechargeRepository rechargeRepository;

    public LieuPersoController(LieuPersoRepository repository, RechargeRepository rechargeRepository) {
        this.repository = repository;
        this.rechargeRepository = rechargeRepository;
    }

    @GetMapping
    public List<LieuPerso> lister() {
        return repository.findAllByOrderByNomAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LieuPerso creer(@RequestBody LieuPerso lieu) {
        lieu.setId(null);
        return enregistrer(lieu);
    }

    @PutMapping("/{id}")
    public LieuPerso modifier(@PathVariable Long id, @RequestBody LieuPerso lieu) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lieu introuvable");
        }
        lieu.setId(id);
        return enregistrer(lieu);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        if (rechargeRepository.existsByLieuPersoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce lieu est utilisé par des recharges");
        }
        repository.deleteById(id);
    }

    private LieuPerso enregistrer(LieuPerso lieu) {
        String nom = lieu.getNom() == null ? "" : lieu.getNom().trim();
        if (nom.isEmpty() || nom.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom obligatoire (100 caractères maximum)");
        }
        if (lieu.getPrixKwh() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix du kWh doit être positif");
        }
        String cle = nom.toLowerCase(java.util.Locale.ROOT);
        if (cle.equals("maison")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom réservé : « Maison » existe déjà");
        }
        repository.findByNomIgnoreCase(nom)
                .filter(autre -> !autre.getId().equals(lieu.getId()))
                .ifPresent(autre -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lieu porte déjà ce nom");
                });
        lieu.setNom(nom);
        return repository.save(lieu);
    }
}
