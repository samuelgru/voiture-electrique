import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, firstValueFrom, map, of, tap } from 'rxjs';

/** INVITE = appareil du réseau local sans session (consultation seule). */
export type Role = 'ADMIN' | 'USER' | 'INVITE';

export interface Utilisateur {
  identifiant: string;
  role: Role;
  /** Adresse(s) vue(s) par le serveur, utile pour diagnostiquer la détection du réseau local. */
  ip?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  /** undefined = pas encore vérifié, null = non connecté */
  readonly utilisateur = signal<Utilisateur | null | undefined>(undefined);
  readonly estAdmin = computed(() => this.utilisateur()?.role === 'ADMIN');
  readonly estInvite = computed(() => this.utilisateur()?.role === 'INVITE');
  /** Saisie autorisée (recharges, relevés) : tout le monde sauf l'invité du réseau local. */
  readonly peutSaisir = computed(() => !!this.utilisateur() && !this.estInvite());
  private chargement?: Promise<Utilisateur | null>;

  /** Interroge le serveur une seule fois pour savoir qui est connecté. */
  verifier(): Promise<Utilisateur | null> {
    const u = this.utilisateur();
    if (u !== undefined) return Promise.resolve(u);
    this.chargement ??= firstValueFrom(
      this.http.get<Utilisateur>('/api/auth/me').pipe(
        catchError(() => of(null)),
        tap((r) => this.utilisateur.set(r)),
      ),
    );
    return this.chargement;
  }

  connecter(identifiant: string, motDePasse: string): Observable<Utilisateur> {
    const body = new HttpParams().set('username', identifiant).set('password', motDePasse);
    return this.http.post<Utilisateur>('/api/auth/login', body).pipe(tap((u) => this.utilisateur.set(u)));
  }

  deconnecter(): void {
    this.http
      .post('/api/auth/logout', null)
      .pipe(
        catchError(() => of(null)),
        map(() => null),
      )
      .subscribe(() => this.sessionExpiree());
  }

  /**
   * Appelé après une déconnexion ou quand le serveur répond 401 (session expirée) : on redemande qui je suis,
   * car sur le réseau local le serveur me reconnaît alors en invité (consultation).
   */
  sessionExpiree(): void {
    this.utilisateur.set(undefined);
    this.chargement = undefined;
    this.verifier().then((u) => this.router.navigate([u ? '/' : '/login']));
  }
}
