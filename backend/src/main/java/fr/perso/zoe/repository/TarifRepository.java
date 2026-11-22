package fr.perso.zoe.repository;

import fr.perso.zoe.model.Tarif;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifRepository extends JpaRepository<Tarif, Long> {
    List<Tarif> findAllByOrderByDateEffetAsc();
}
