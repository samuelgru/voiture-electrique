package fr.perso.zoe.web;

import fr.perso.zoe.dto.SyntheseDto;
import fr.perso.zoe.service.SyntheseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/synthese")
public class SyntheseController {

    private final SyntheseService service;

    public SyntheseController(SyntheseService service) {
        this.service = service;
    }

    @GetMapping
    public SyntheseDto synthese() {
        return service.calculer();
    }
}
