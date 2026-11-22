package fr.perso.zoe.repository;

import fr.perso.zoe.model.ReleveKm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleveKmRepository extends JpaRepository<ReleveKm, Long> {
    List<ReleveKm> findAllByOrderByDateReleveAsc();
}
