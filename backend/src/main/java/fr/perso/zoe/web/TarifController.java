package fr.perso.zoe.web;

import fr.perso.zoe.model.Tarif;
import fr.perso.zoe.repository.TarifRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tarifs")
public class TarifController {

    private final TarifRepository repository;

    public TarifController(TarifRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Tarif> lister() {
        return repository.findAllByOrderByDateEffetAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tarif creer(@RequestBody Tarif tarif) {
        tarif.setId(null);
        return enregistrer(tarif);
    }

    @PutMapping("/{id}")
    public Tarif modifier(@PathVariable Long id, @RequestBody Tarif tarif) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarif introuvable");
        }
        tarif.setId(id);
        return enregistrer(tarif);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private Tarif enregistrer(Tarif tarif) {
        if (tarif.getDateEffet() == null || tarif.getHeureDebutHc() == null || tarif.getHeureFinHc() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date d'effet et plage HC obligatoires");
        }
        return repository.save(tarif);
    }
}
