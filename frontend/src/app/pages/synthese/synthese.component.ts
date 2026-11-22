import { AfterViewChecked, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { ApiService, messageErreur } from '../../core/api.service';
import { Synthese } from '../../core/models';

Chart.register(...registerables);

@Component({
  selector: 'app-synthese',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, NgClass, RouterLink],
  templateUrl: './synthese.component.html',
})
export class SyntheseComponent implements OnInit, AfterViewChecked, OnDestroy {
  private api = inject(ApiService);

  synthese = signal<Synthese | null>(null);
  erreur = signal<string | null>(null);

  @ViewChild('chartKm') chartKmRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('chartCout') chartCoutRef?: ElementRef<HTMLCanvasElement>;
  private charts: Chart[] = [];
  private chartsDessines = false;

  ngOnInit(): void {
    this.api.synthese().subscribe({
      next: (s) => this.synthese.set(s),
      error: (e) => this.erreur.set(messageErreur(e)),
    });
  }

  ngAfterViewChecked(): void {
    const s = this.synthese();
    if (!this.chartsDessines && s && this.chartKmRef && this.chartCoutRef) {
      this.chartsDessines = true;
      this.dessiner(s);
    }
  }

  ngOnDestroy(): void {
    this.charts.forEach((c) => c.destroy());
  }

  private dessiner(s: Synthese): void {
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
      new Chart(this.chartKmRef!.nativeElement, {
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
        options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
      }),
    );

    this.charts.push(
      new Chart(this.chartCoutRef!.nativeElement, {
        type: 'bar',
        data: {
          labels,
          datasets: [
            { label: 'Électricité (€)', data: s.mois.map((m) => round(m.cout)), backgroundColor: primary, borderRadius: 4 },
            { label: 'Essence simulée (€)', data: s.mois.map((m) => round(m.coutEssence)), backgroundColor: accent, borderRadius: 4 },
          ],
        },
        options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
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
