import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ImportResult, Parametres, Recharge, RechargeRequest, ReleveKm, Synthese, Tarif } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = '/api';

  synthese(): Observable<Synthese> {
    return this.http.get<Synthese>(`${this.base}/synthese`);
  }

  recharges(): Observable<Recharge[]> {
    return this.http.get<Recharge[]>(`${this.base}/recharges`);
  }
  creerRecharge(r: RechargeRequest): Observable<Recharge> {
    return this.http.post<Recharge>(`${this.base}/recharges`, r);
  }
  modifierRecharge(id: number, r: RechargeRequest): Observable<Recharge> {
    return this.http.put<Recharge>(`${this.base}/recharges/${id}`, r);
  }
  supprimerRecharge(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/recharges/${id}`);
  }

  tarifs(): Observable<Tarif[]> {
    return this.http.get<Tarif[]>(`${this.base}/tarifs`);
  }
  enregistrerTarif(t: Tarif): Observable<Tarif> {
    return t.id
      ? this.http.put<Tarif>(`${this.base}/tarifs/${t.id}`, t)
      : this.http.post<Tarif>(`${this.base}/tarifs`, t);
  }
  supprimerTarif(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/tarifs/${id}`);
  }

  releves(): Observable<ReleveKm[]> {
    return this.http.get<ReleveKm[]>(`${this.base}/releves`);
  }
  enregistrerReleve(r: ReleveKm): Observable<ReleveKm> {
    return r.id
      ? this.http.put<ReleveKm>(`${this.base}/releves/${r.id}`, r)
      : this.http.post<ReleveKm>(`${this.base}/releves`, r);
  }
  supprimerReleve(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/releves/${id}`);
  }

  parametres(): Observable<Parametres> {
    return this.http.get<Parametres>(`${this.base}/parametres`);
  }
  enregistrerParametres(p: Parametres): Observable<Parametres> {
    return this.http.put<Parametres>(`${this.base}/parametres`, p);
  }

  exporter(): Observable<Blob> {
    return this.http.get(`${this.base}/export`, { responseType: 'blob' });
  }

  importer(fichier: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('fichier', fichier);
    return this.http.post<ImportResult>(`${this.base}/import`, form);
  }
}

/** Extrait un message lisible d'une erreur HTTP Spring. */
export function messageErreur(e: any): string {
  return e?.error?.detail || e?.error?.message || e?.message || 'Erreur inattendue';
}
