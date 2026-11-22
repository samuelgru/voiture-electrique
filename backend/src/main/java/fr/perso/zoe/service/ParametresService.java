package fr.perso.zoe.service;

import fr.perso.zoe.model.Parametres;
import fr.perso.zoe.repository.ParametresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametresService {

    private final ParametresRepository repository;

    public ParametresService(ParametresRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Parametres get() {
        return repository.findById(Parametres.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Paramètres absents de la base"));
    }

    @Transactional
    public Parametres enregistrer(Parametres p) {
        p.setId(Parametres.SINGLETON_ID);
        return repository.save(p);
    }
}
