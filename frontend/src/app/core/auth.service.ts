import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, firstValueFrom, map, of, tap } from 'rxjs';

export type Role = 'ADMIN' | 'USER';

export interface Utilisateur {
  identifiant: string;
  role: Role;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  /** undefined = pas encore vérifié, null = non connecté */
  readonly utilisateur = signal<Utilisateur | null | undefined>(undefined);
  readonly estAdmin = computed(() => this.utilisateur()?.role === 'ADMIN');
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

  /** Appelé aussi quand le serveur répond 401 (session expirée). */
  sessionExpiree(): void {
    this.utilisateur.set(null);
    this.chargement = undefined;
    this.router.navigate(['/login']);
  }
}
