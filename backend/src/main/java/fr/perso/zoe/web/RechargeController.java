package fr.perso.zoe.web;

import fr.perso.zoe.dto.RechargeDto;
import fr.perso.zoe.dto.RechargeRequest;
import fr.perso.zoe.service.RechargeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recharges")
public class RechargeController {

    private final RechargeService service;

    public RechargeController(RechargeService service) {
        this.service = service;
    }

    @GetMapping
    public List<RechargeDto> lister() {
        return service.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RechargeDto creer(@Valid @RequestBody RechargeRequest req) {
        return service.creer(req);
    }

    @PutMapping("/{id}")
    public RechargeDto modifier(@PathVariable Long id, @Valid @RequestBody RechargeRequest req) {
        return service.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
