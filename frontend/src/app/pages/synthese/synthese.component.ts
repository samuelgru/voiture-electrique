import { Component, ElementRef, OnDestroy, OnInit, afterRenderEffect, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { ApiService, messageErreur } from '../../core/api.service';
import { Synthese } from '../../core/models';

Chart.register(...registerables);

@Component({
  selector: 'app-synthese',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, NgClass, RouterLink],
  templateUrl: './synthese.component.html',
})
export class SyntheseComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);

  synthese = signal<Synthese | null>(null);
  erreur = signal<string | null>(null);

  private chartKm = viewChild<ElementRef<HTMLCanvasElement>>('chartKm');
  private chartCout = viewChild<ElementRef<HTMLCanvasElement>>('chartCout');
  private charts: Chart[] = [];

  constructor() {
    // Dessine les graphiques une fois les données reçues et les canvas présents dans le DOM
    afterRenderEffect(() => {
      const s = this.synthese();
      const km = this.chartKm()?.nativeElement;
      const cout = this.chartCout()?.nativeElement;
      if (s && km && cout && this.charts.length === 0) {
        this.dessiner(s, km, cout);
      }
    });
  }

  ngOnInit(): void {
    this.api.synthese().subscribe({
      next: (s) => this.synthese.set(s),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  ngOnDestroy(): void {
    this.charts.forEach((c) => c.destroy());
  }

  private dessiner(s: Synthese, canvasKm: HTMLCanvasElement, canvasCout: HTMLCanvasElement): void {
    const style = getComputedStyle(document.documentElement);
    const primary = style.getPropertyValue('--primary').trim();
    const accent = style.getPropertyValue('--accent').trim();
    const muted = style.getPropertyValue('--muted').trim();
    const border = style.getPropertyValue('--border').trim();
    Chart.defaults.color = muted;
    Chart.defaults.borderColor = border;

    const labels = s.mois.map((m) =>
      new Date(m.mois).toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' }),
    );

    this.charts.push(
      new Chart(canvasKm, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            { label: 'Km parcourus', data: s.mois.map((m) => m.kmMensuel), backgroundColor: primary, borderRadius: 4 },
            {
              label: 'Forfait mensuel',
              type: 'line',
              data: s.mois.map(() => s.kmParMois),
              borderColor: accent,
              borderDash: [6, 4],
              pointRadius: 0,
              borderWidth: 2,
            },
          ],
        },
        options: { locale: 'fr-FR', maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      }),
    );

    this.charts.push(
      new Chart(canvasCout, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            { label: 'Électricité (€)', data: s.mois.map((m) => round(m.cout)), backgroundColor: primary, borderRadius: 4 },
            { label: 'Essence simulée (€)', data: s.mois.map((m) => round(m.coutEssence)), backgroundColor: accent, borderRadius: 4 },
          ],
        },
        options: { locale: 'fr-FR', maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      }),
    );
  }

  totalKm(s: Synthese): number {
    return s.mois.reduce((a, m) => a + (m.kmMensuel ?? 0), 0);
  }
  totalReste(s: Synthese): number {
    return s.mois.reduce((a, m) => a + (m.resteAFaire ?? 0), 0);
  }
  totalKwh(s: Synthese): number {
    return s.mois.reduce((a, m) => a + m.kwh, 0);
  }
  totalGain(s: Synthese): number {
    return s.mois.reduce((a, m) => a + (m.gain ?? 0), 0);
  }
}

function round(v: number | null): number | null {
  return v == null ? null : Math.round(v * 100) / 100;
}
