package fr.perso.zoe.repository;

import fr.perso.zoe.model.LieuPerso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LieuPersoRepository extends JpaRepository<LieuPerso, Long> {
    List<LieuPerso> findAllByOrderByNomAsc();
    Optional<LieuPerso> findByNomIgnoreCase(String nom);
}
