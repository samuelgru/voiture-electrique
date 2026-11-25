package fr.perso.zoe.dto;

import fr.perso.zoe.model.Lieu;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record RechargeRequest(
        @NotNull Lieu lieu,
        /** Obligatoire quand le lieu est AUTRE, ignoré sinon. */
        Long lieuPersoId,
        @NotNull LocalDateTime debut,
        @NotNull LocalDateTime fin,
        @DecimalMin("0") @DecimalMax("1") Double pctDebut,
        @DecimalMin("0") @DecimalMax("1") Double pctFin,
        @NotNull @PositiveOrZero Double kwh,
        @Size(max = 500) String commentaire
) {}
