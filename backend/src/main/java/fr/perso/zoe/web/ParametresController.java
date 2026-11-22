package fr.perso.zoe.web;

import fr.perso.zoe.model.Parametres;
import fr.perso.zoe.service.ParametresService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parametres")
public class ParametresController {

    private final ParametresService service;

    public ParametresController(ParametresService service) {
        this.service = service;
    }

    @GetMapping
    public Parametres get() {
        return service.get();
    }

    @PutMapping
    public Parametres enregistrer(@RequestBody Parametres p) {
        return service.enregistrer(p);
    }
}
