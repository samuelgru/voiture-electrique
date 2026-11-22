package fr.perso.zoe.web;

import fr.perso.zoe.model.ReleveKm;
import fr.perso.zoe.repository.ReleveKmRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/releves")
public class ReleveKmController {

    private final ReleveKmRepository repository;

    public ReleveKmController(ReleveKmRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ReleveKm> lister() {
        return repository.findAllByOrderByDateReleveAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReleveKm creer(@RequestBody ReleveKm releve) {
        releve.setId(null);
        return enregistrer(releve);
    }

    @PutMapping("/{id}")
    public ReleveKm modifier(@PathVariable Long id, @RequestBody ReleveKm releve) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Relevé introuvable");
        }
        releve.setId(id);
        return enregistrer(releve);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        repository.deleteById(id);
    }

    private ReleveKm enregistrer(ReleveKm releve) {
        if (releve.getDateReleve() == null || releve.getKilometrage() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date et kilométrage obligatoires");
        }
        return repository.save(releve);
    }
}
