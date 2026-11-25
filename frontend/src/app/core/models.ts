export type Lieu = 'MAISON' | 'AUTRE';

/** Lieu de recharge créé par l'utilisateur, avec son prix du kWh. */
export interface LieuPerso {
  id?: number;
  nom: string;
  prixKwh: number;
}

export interface RechargeRequest {
  lieu: Lieu;
  /** Obligatoire quand le lieu est AUTRE. */
  lieuPersoId: number | null;
  debut: string;
  fin: string;
  pctDebut: number | null;
  pctFin: number | null;
  kwh: number;
  commentaire: string | null;
}

export interface Recharge extends RechargeRequest {
  id: number;
  lieuNom: string | null;
  pctCharge: number | null;
  nbHeures: number;
  heuresHc: number;
  heuresHp: number;
  puissanceKw: number;
  coutHc: number;
  coutHp: number;
  coutTotal: number;
}

export interface Tarif {
  id?: number;
  dateEffet: string;
  prixHc: number;
  prixHp: number;
  prixEssence: number;
  heureDebutHc: string;
  heureFinHc: string;
}

export interface ReleveKm {
  id?: number;
  dateReleve: string;
  kilometrage: number;
}

export interface Parametres {
  contratDebut: string;
  contratDureeAns: number;
  contratKm: number;
  kmDebut: number;
  penaliteParKm: number;
  consoEssenceL100: number;
}

export interface Mois {
  mois: string;
  enCours: boolean;
  kmDebut: number | null;
  kmMensuel: number | null;
  resteAFaire: number | null;
  kmParJour: number | null;
  kwh: number;
  cout: number;
  nbRecharges: number;
  prixEssence: number | null;
  coutEssence: number | null;
  gain: number | null;
}

export interface Synthese {
  contratDebut: string;
  contratFin: string;
  contratDureeAns: number;
  contratKm: number;
  kmParAn: number;
  kmParMois: number;
  kmParJour: number;
  kmDebut: number;
  kmActuel: number | null;
  dateKmActuel: string | null;
  kmParcourus: number | null;
  nbMois: number;
  ecartContratMoisPrecedent: number;
  ecartContratMois: number;
  penaliteParKm: number;
  penalite: number;
  kmProjetesFinContrat: number | null;
  ecartProjeteFinContrat: number | null;
  kwhTotal: number;
  coutElectricite: number;
  coutAu100km: number | null;
  kwhAu100km: number | null;
  consoEssenceL100: number;
  prixLitreEssence: number | null;
  prixEssenceAu100km: number | null;
  coutEssence: number;
  gainTotal: number;
  gainParMois: number | null;
  mois: Mois[];
}

export interface ImportResult {
  recharges: number;
  tarifs: number;
  releves: number;
  parametres: boolean;
  avertissements: string[];
}
