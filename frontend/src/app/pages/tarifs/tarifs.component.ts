import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, messageErreur } from '../../core/api.service';
import { LieuPerso, Tarif } from '../../core/models';

@Component({
  selector: 'app-tarifs',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  template: `
    <div class="toolbar">
      <h1>Tarifs de référence</h1>
      @if (!form()) {
        <button class="primary" (click)="nouveau()">+ Nouveau tarif</button>
      }
    </div>
    <p class="muted">
      Chaque tarif s'applique à partir de sa date d'effet, jusqu'au tarif suivant.
      Une recharge prend le tarif en vigueur à sa date de début.
    </p>

    @if (erreur()) {
      <div class="alert error">{{ erreur() }}</div>
    }

    @if (form(); as f) {
      <form class="card" (ngSubmit)="enregistrer()">
        <h2>{{ f.id ? 'Modifier le tarif' : 'Nouveau tarif' }}</h2>
        <div class="form-grid">
          <label>Date d'effet <input type="date" name="date" [(ngModel)]="f.dateEffet" required /></label>
          <label>Prix HC (€/kWh) <input type="number" name="hc" [(ngModel)]="f.prixHc" step="0.0001" min="0" required /></label>
          <label>Prix HP (€/kWh) <input type="number" name="hp" [(ngModel)]="f.prixHp" step="0.0001" min="0" required /></label>
          <label>Essence (€/L) <input type="number" name="ess" [(ngModel)]="f.prixEssence" step="0.01" min="0" required /></label>
          <label>Début HC <input type="time" name="dhc" [(ngModel)]="f.heureDebutHc" required /></label>
          <label>Fin HC <input type="time" name="fhc" [(ngModel)]="f.heureFinHc" required /></label>
        </div>
        <div class="actions" style="margin-top: 14px">
          <button type="submit" class="primary">Enregistrer</button>
          <button type="button" (click)="form.set(null)">Annuler</button>
        </div>
      </form>
    }

    <div class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>À partir du</th>
              <th class="num">HC €/kWh</th>
              <th class="num">HP €/kWh</th>
              <th class="num">Essence €/L</th>
              <th>Heures creuses</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (t of tarifs(); track t.id) {
              <tr>
                <td>{{ t.dateEffet | date: 'dd/MM/yyyy' }}</td>
                <td class="num">{{ t.prixHc | currency: 'EUR' : 'symbol' : '1.4-4' }}</td>
                <td class="num">{{ t.prixHp | currency: 'EUR' : 'symbol' : '1.4-4' }}</td>
                <td class="num">{{ t.prixEssence | currency: 'EUR' }}</td>
                <td>{{ t.heureDebutHc.slice(0, 5) }} → {{ t.heureFinHc.slice(0, 5) }}</td>
                <td>
                  <div class="actions">
                    <button class="icon" (click)="editer(t)" title="Modifier">✎</button>
                    <button class="icon danger" (click)="supprimer(t)" title="Supprimer">✕</button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="muted">Aucun tarif.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    <div class="toolbar" style="margin-top: 28px">
      <h2>Autres lieux de recharge</h2>
      @if (!formLieu()) {
        <button class="primary" (click)="nouveauLieu()">+ Nouveau lieu</button>
      }
    </div>
    <p class="muted">
      « Maison » utilise les tarifs HC/HP ci-dessus. Tout autre lieu (parc relais, travail…) a un prix du kWh unique,
      sans heures creuses, qui s'applique à toutes ses recharges (modifier le prix recalcule aussi les anciennes).
    </p>

    @if (formLieu(); as l) {
      <form class="card" (ngSubmit)="enregistrerLieu()">
        <h3>{{ l.id ? 'Modifier le lieu' : 'Nouveau lieu' }}</h3>
        <div class="form-grid">
          <label>Nom <input type="text" name="nomLieu" [(ngModel)]="l.nom" maxlength="100" required /></label>
          <label>Prix (€/kWh) <input type="number" name="prixLieu" [(ngModel)]="l.prixKwh" step="0.0001" min="0" required /></label>
        </div>
        <div class="actions" style="margin-top: 14px">
          <button type="submit" class="primary" [disabled]="!l.nom.trim() || l.prixKwh == null">Enregistrer</button>
          <button type="button" (click)="formLieu.set(null)">Annuler</button>
        </div>
      </form>
    }

    <div class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Lieu</th>
              <th class="num">€/kWh</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (l of lieux(); track l.id) {
              <tr>
                <td>{{ l.nom }}</td>
                <td class="num">{{ l.prixKwh | currency: 'EUR' : 'symbol' : '1.4-4' }}</td>
                <td>
                  <div class="actions">
                    <button class="icon" (click)="editerLieu(l)" title="Modifier">✎</button>
                    <button class="icon danger" (click)="supprimerLieu(l)" title="Supprimer">✕</button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="3" class="muted">Aucun autre lieu.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class TarifsComponent implements OnInit {
  private api = inject(ApiService);

  tarifs = signal<Tarif[]>([]);
  erreur = signal<string | null>(null);
  form = signal<Tarif | null>(null);
  lieux = signal<LieuPerso[]>([]);
  formLieu = signal<LieuPerso | null>(null);

  ngOnInit(): void {
    this.charger();
    this.chargerLieux();
  }

  chargerLieux(): void {
    this.api.lieux().subscribe({
      next: (l) => this.lieux.set(l),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  nouveauLieu(): void {
    this.formLieu.set({ nom: '', prixKwh: 0 });
  }

  editerLieu(l: LieuPerso): void {
    this.formLieu.set({ ...l });
  }

  enregistrerLieu(): void {
    const l = this.formLieu();
    if (!l) return;
    this.api.enregistrerLieu({ ...l, nom: l.nom.trim() }).subscribe({
      next: () => {
        this.formLieu.set(null);
        this.erreur.set(null);
        this.chargerLieux();
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  supprimerLieu(l: LieuPerso): void {
    if (!l.id || !confirm(`Supprimer le lieu « ${l.nom} » ?`)) return;
    this.api.supprimerLieu(l.id).subscribe({
      next: () => {
        this.erreur.set(null);
        this.chargerLieux();
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  charger(): void {
    this.api.tarifs().subscribe({
      next: (t) => this.tarifs.set(t),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  nouveau(): void {
    const dernier = this.tarifs().at(-1);
    this.form.set({
      dateEffet: new Date().toISOString().slice(0, 10),
      prixHc: dernier?.prixHc ?? 0,
      prixHp: dernier?.prixHp ?? 0,
      prixEssence: dernier?.prixEssence ?? 0,
      heureDebutHc: dernier?.heureDebutHc.slice(0, 5) ?? '22:00',
      heureFinHc: dernier?.heureFinHc.slice(0, 5) ?? '06:00',
    });
  }

  editer(t: Tarif): void {
    this.form.set({ ...t, heureDebutHc: t.heureDebutHc.slice(0, 5), heureFinHc: t.heureFinHc.slice(0, 5) });
  }

  enregistrer(): void {
    const f = this.form();
    if (!f) return;
    this.api.enregistrerTarif(f).subscribe({
      next: () => {
        this.form.set(null);
        this.erreur.set(null);
        this.charger();
      },
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  supprimer(t: Tarif): void {
    if (!t.id || !confirm('Supprimer ce tarif ?')) return;
    this.api.supprimerTarif(t.id).subscribe({
      next: () => this.charger(),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }
}
