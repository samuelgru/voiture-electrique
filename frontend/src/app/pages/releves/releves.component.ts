import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, messageErreur } from '../../core/api.service';
import { ReleveKm } from '../../core/models';

@Component({
  selector: 'app-releves',
  imports: [FormsModule, DatePipe, DecimalPipe],
  template: `
    <div class="toolbar">
      <h1>Kilométrage</h1>
      @if (!form()) {
        <button class="primary" (click)="nouveau()">+ Nouveau relevé</button>
      }
    </div>
    <p class="muted">
      Saisis le compteur au 1<sup>er</sup> de chaque mois : la synthèse en déduit les kilomètres mensuels.
      Le relevé le plus récent sert de « kilométrage actuel ».
    </p>

    @if (erreur()) {
      <div class="alert error">{{ erreur() }}</div>
    }

    @if (form(); as f) {
      <form class="card" (ngSubmit)="enregistrer()">
        <h2>{{ f.id ? 'Modifier le relevé' : 'Nouveau relevé' }}</h2>
        <div class="form-grid">
          <label>Date <input type="date" name="date" [(ngModel)]="f.dateReleve" required /></label>
          <label>Compteur (km) <input type="number" name="km" [(ngModel)]="f.kilometrage" min="0" required /></label>
          <div class="actions">
            <button type="submit" class="primary">Enregistrer</button>
            <button type="button" (click)="form.set(null)">Annuler</button>
          </div>
        </div>
      </form>
    }

    <div class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Date</th><th class="num">Compteur</th><th class="num">Depuis le relevé précédent</th><th></th></tr>
          </thead>
          <tbody>
            @for (r of lignes(); track r.releve.id) {
              <tr>
                <td>{{ r.releve.dateReleve | date: 'EEEE dd MMMM yyyy' }}</td>
                <td class="num">{{ r.releve.kilometrage | number: '1.0-0' }} km</td>
                <td class="num">{{ r.delta != null ? (r.delta | number: '1.0-0') + ' km' : '' }}</td>
                <td>
                  <div class="actions">
                    <button class="icon" (click)="editer(r.releve)" title="Modifier">✎</button>
                    <button class="icon danger" (click)="supprimer(r.releve)" title="Supprimer">✕</button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="muted">Aucun relevé.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class RelevesComponent implements OnInit {
  private api = inject(ApiService);

  releves = signal<ReleveKm[]>([]);
  erreur = signal<string | null>(null);
  form = signal<ReleveKm | null>(null);

  lignes = computed(() => {
    const r = this.releves();
    return r
      .map((releve, i) => ({ releve, delta: i > 0 ? releve.kilometrage - r[i - 1].kilometrage : null }))
      .reverse();
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.api.releves().subscribe({
      next: (r) => this.releves.set(r),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  nouveau(): void {
    const dernier = this.releves().at(-1);
    this.form.set({ dateReleve: new Date().toISOString().slice(0, 10), kilometrage: dernier?.kilometrage ?? 0 });
  }

  editer(r: ReleveKm): void {
    this.form.set({ ...r });
  }

  enregistrer(): void {
    const f = this.form();
    if (!f) return;
    this.api.enregistrerReleve(f).subscribe({
      next: () => {
        this.form.set(null);
        this.erreur.set(null);
        this.charger();
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  supprimer(r: ReleveKm): void {
    if (!r.id || !confirm('Supprimer ce relevé ?')) return;
    this.api.supprimerReleve(r.id).subscribe({
      next: () => this.charger(),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }
}
