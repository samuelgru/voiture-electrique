import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, messageErreur } from '../../core/api.service';
import { ImportResult, Parametres } from '../../core/models';

@Component({
  selector: 'app-parametres',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Paramètres</h1>

    @if (erreur()) {
      <div class="alert error">{{ erreur() }}</div>
    }
    @if (message()) {
      <div class="alert success">{{ message() }}</div>
    }

    @if (params(); as p) {
      <form class="card" (ngSubmit)="enregistrer()">
        <h2>Contrat de location &amp; comparaison essence</h2>
        <div class="form-grid">
          <label>Début du contrat <input type="date" name="debut" [(ngModel)]="p.contratDebut" required /></label>
          <label>Durée (ans) <input type="number" name="duree" [(ngModel)]="p.contratDureeAns" min="1" required /></label>
          <label>Kilométrage du contrat <input type="number" name="km" [(ngModel)]="p.contratKm" min="0" required /></label>
          <label>Compteur au début <input type="number" name="kmDebut" [(ngModel)]="p.kmDebut" min="0" required /></label>
          <label>Pénalité (€/km en trop) <input type="number" name="pen" [(ngModel)]="p.penaliteParKm" step="0.0001" min="0" /></label>
          <label>Conso essence (L/100 km) <input type="number" name="conso" [(ngModel)]="p.consoEssenceL100" step="0.1" min="0" /></label>
        </div>
        <div class="actions" style="margin-top: 14px">
          <button type="submit" class="primary">Enregistrer</button>
        </div>
      </form>
    }

    <div class="card">
      <h2>Import du fichier Excel</h2>
      <p class="muted">
        Importe <code>electricite.xlsx</code> (onglets « Zoe », « Prix Electricité » et « Synthèse »).
        <strong>Toutes les recharges, tarifs et relevés existants sont remplacés.</strong>
      </p>
      <div class="actions">
        <input type="file" accept=".xlsx" (change)="choisir($event)" />
        <button class="primary" [disabled]="!fichier() || enCours()" (click)="importer()">
          {{ enCours() ? 'Import…' : 'Importer' }}
        </button>
      </div>
      @if (resultat(); as r) {
        <div class="alert success" style="margin-top: 14px">
          {{ r.recharges }} recharges, {{ r.tarifs }} tarifs et {{ r.releves }} relevés importés.
        </div>
        @for (a of r.avertissements; track a) {
          <div class="muted">⚠ {{ a }}</div>
        }
      }
    </div>
  `,
})
export class ParametresComponent implements OnInit {
  private api = inject(ApiService);

  params = signal<Parametres | null>(null);
  erreur = signal<string | null>(null);
  message = signal<string | null>(null);
  fichier = signal<File | null>(null);
  enCours = signal(false);
  resultat = signal<ImportResult | null>(null);

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.api.parametres().subscribe({
      next: (p) => this.params.set(p),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  enregistrer(): void {
    const p = this.params();
    if (!p) return;
    this.api.enregistrerParametres(p).subscribe({
      next: (r) => {
        this.params.set(r);
        this.erreur.set(null);
        this.message.set('Paramètres enregistrés.');
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  choisir(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    this.fichier.set(input.files?.[0] ?? null);
  }

  importer(): void {
    const f = this.fichier();
    if (!f || !confirm('Remplacer toutes les données par le contenu du fichier ?')) return;
    this.enCours.set(true);
    this.resultat.set(null);
    this.api.importer(f).subscribe({
      next: (r) => {
        this.enCours.set(false);
        this.erreur.set(null);
        this.resultat.set(r);
        this.charger();
      },
      error: (e) => {
        this.enCours.set(false);
        this.erreur.set(messageErreur(e));
      },
    });
  }
}
