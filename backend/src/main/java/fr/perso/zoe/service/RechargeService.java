package fr.perso.zoe.service;

import fr.perso.zoe.dto.RechargeDto;
import fr.perso.zoe.dto.RechargeRequest;
import fr.perso.zoe.model.Lieu;
import fr.perso.zoe.model.LieuPerso;
import fr.perso.zoe.model.Recharge;
import fr.perso.zoe.model.Tarif;
import fr.perso.zoe.repository.LieuPersoRepository;
import fr.perso.zoe.repository.RechargeRepository;
import fr.perso.zoe.repository.TarifRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RechargeService {

    private final RechargeRepository repository;
    private final TarifRepository tarifRepository;
    private final LieuPersoRepository lieuRepository;
    private final CalculRechargeService calcul;

    public RechargeService(RechargeRepository repository, TarifRepository tarifRepository,
                           LieuPersoRepository lieuRepository, CalculRechargeService calcul) {
        this.repository = repository;
        this.lieuRepository = lieuRepository;
        this.tarifRepository = tarifRepository;
        this.calcul = calcul;
    }

    @Transactional(readOnly = true)
    public List<RechargeDto> lister() {
        List<Tarif> tarifs = tarifRepository.findAllByOrderByDateEffetAsc();
        return repository.findAllByOrderByDebutDesc().stream().map(r -> calcul.calculer(r, tarifs)).toList();
    }

    @Transactional
    public RechargeDto creer(RechargeRequest req) {
        Recharge r = new Recharge();
        appliquer(r, req);
        return calcul.calculer(repository.save(r), tarifRepository.findAllByOrderByDateEffetAsc());
    }

    @Transactional
    public RechargeDto modifier(Long id, RechargeRequest req) {
        Recharge r = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recharge introuvable"));
        appliquer(r, req);
        return calcul.calculer(repository.save(r), tarifRepository.findAllByOrderByDateEffetAsc());
    }

    @Transactional
    public void supprimer(Long id) {
        repository.deleteById(id);
    }

    private void appliquer(Recharge r, RechargeRequest req) {
        if (req.fin().isBefore(req.debut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fin doit être après le début");
        }
        r.setLieu(req.lieu());
        if (req.lieu() == Lieu.AUTRE) {
            if (req.lieuPersoId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisis un lieu");
            }
            LieuPerso lieu = lieuRepository.findById(req.lieuPersoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lieu introuvable"));
            r.setLieuPerso(lieu);
        } else {
            r.setLieuPerso(null);
        }
        r.setDebut(req.debut().withNano(0));
        r.setFin(req.fin().withNano(0));
        r.setPctDebut(req.pctDebut());
        r.setPctFin(req.pctFin());
        r.setKwh(req.kwh());
        r.setCommentaire(req.commentaire());
    }
}
