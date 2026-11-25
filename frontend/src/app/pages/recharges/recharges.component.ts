import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, messageErreur } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { LieuPerso, Recharge, RechargeRequest } from '../../core/models';

interface FormRecharge {
  id: number | null;
  /** 'MAISON' ou 'AUTRE:<id du lieu>'. */
  lieu: string;
  debut: string;
  fin: string;
  pctDebut: number | null;
  pctFin: number | null;
  kwh: number | null;
  commentaire: string;
}

@Component({
  selector: 'app-recharges',
  imports: [FormsModule, CurrencyPipe, DatePipe, DecimalPipe, PercentPipe],
  templateUrl: './recharges.component.html',
})
export class RechargesComponent implements OnInit {
  private api = inject(ApiService);
  auth = inject(AuthService);

  lieux = signal<LieuPerso[]>([]);
  recharges = signal<Recharge[]>([]);
  erreur = signal<string | null>(null);
  form = signal<FormRecharge | null>(null);
  filtreMois = signal<string>('');

  mois = computed(() => [...new Set(this.recharges().map((r) => r.debut.slice(0, 7)))]);
  visibles = computed(() => {
    const f = this.filtreMois();
    return f ? this.recharges().filter((r) => r.debut.startsWith(f)) : this.recharges();
  });
  totaux = computed(() =>
    this.visibles().reduce(
      (t, r) => ({ kwh: t.kwh + r.kwh, cout: t.cout + r.coutTotal, heures: t.heures + r.nbHeures }),
      { kwh: 0, cout: 0, heures: 0 },
    ),
  );

  ngOnInit(): void {
    this.charger();
    this.api.lieux().subscribe({
      next: (l) => this.lieux.set(l),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  charger(): void {
    this.api.recharges().subscribe({
      next: (r) => this.recharges.set(r),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  nouvelle(): void {
    const aujourdhui = new Date();
    const demain = new Date(aujourdhui.getTime() + 86400000);
    this.form.set({
      id: null,
      lieu: 'MAISON',
      debut: `${isoDate(aujourdhui)}T22:30`,
      fin: `${isoDate(demain)}T06:15`,
      pctDebut: null,
      pctFin: null,
      kwh: null,
      commentaire: '',
    });
  }

  editer(r: Recharge): void {
    this.form.set({
      id: r.id,
      lieu: r.lieu === 'AUTRE' ? `AUTRE:${r.lieuPersoId}` : r.lieu,
      debut: r.debut.slice(0, 16),
      fin: r.fin.slice(0, 16),
      pctDebut: r.pctDebut != null ? Math.round(r.pctDebut * 100) : null,
      pctFin: r.pctFin != null ? Math.round(r.pctFin * 100) : null,
      kwh: r.kwh,
      commentaire: r.commentaire ?? '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  annuler(): void {
    this.form.set(null);
  }

  enregistrer(): void {
    const f = this.form();
    if (!f || f.kwh == null) return;
    const [lieu, lieuId] = f.lieu.split(':');
    const req: RechargeRequest = {
      lieu: lieu as RechargeRequest['lieu'],
      lieuPersoId: lieuId ? Number(lieuId) : null,
      debut: f.debut,
      fin: f.fin,
      pctDebut: f.pctDebut != null ? f.pctDebut / 100 : null,
      pctFin: f.pctFin != null ? f.pctFin / 100 : null,
      kwh: f.kwh,
      commentaire: f.commentaire?.trim() || null,
    };
    const appel = f.id ? this.api.modifierRecharge(f.id, req) : this.api.creerRecharge(req);
    appel.subscribe({
      next: () => {
        this.form.set(null);
        this.erreur.set(null);
        this.charger();
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  supprimer(r: Recharge): void {
    if (!confirm('Supprimer cette recharge ?')) return;
    this.api.supprimerRecharge(r.id).subscribe({
      next: () => this.charger(),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  libelleLieu(r: Recharge): string {
    return r.lieu === 'AUTRE' ? (r.lieuNom ?? 'Autre') : 'Maison';
  }
}

function isoDate(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
