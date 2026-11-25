package fr.perso.zoe.repository;

import fr.perso.zoe.model.Recharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechargeRepository extends JpaRepository<Recharge, Long> {
    List<Recharge> findAllByOrderByDebutDesc();
    boolean existsByLieuPersoId(Long lieuPersoId);
}
