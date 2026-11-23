import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { messageErreur } from '../../core/api.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="login-page">
      <form class="card login-card" (ngSubmit)="connecter()">
        <div class="brand"><span class="logo">⚡</span> Ma Zoé</div>
        @if (erreur()) {
          <div class="alert error">{{ erreur() }}</div>
        }
        <label>Identifiant
          <input name="identifiant" [(ngModel)]="identifiant" autocomplete="username" required autofocus />
        </label>
        <label>Mot de passe
          <input name="motDePasse" type="password" [(ngModel)]="motDePasse" autocomplete="current-password" required />
        </label>
        <button type="submit" class="primary" [disabled]="enCours() || !identifiant || !motDePasse">
          {{ enCours() ? 'Connexion…' : 'Se connecter' }}
        </button>
      </form>
    </div>
  `,
  styles: `
    .login-page { min-height: 70vh; display: grid; place-items: center; }
    .login-card { width: min(360px, 100%); display: flex; flex-direction: column; gap: 14px; padding: 24px; }
    .brand { font-weight: 700; font-size: 20px; text-align: center; margin-bottom: 4px; }
    .logo { color: var(--primary); }
    .alert { margin: 0; }
  `,
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  identifiant = '';
  motDePasse = '';
  enCours = signal(false);
  erreur = signal<string | null>(null);

  connecter(): void {
    this.enCours.set(true);
    this.erreur.set(null);
    this.auth.connecter(this.identifiant.trim(), this.motDePasse).subscribe({
      next: () => {
        this.enCours.set(false);
        this.router.navigate(['/']);
      },
      error: (e) => {
        this.enCours.set(false);
        this.motDePasse = '';
        this.erreur.set(e.status === 401 || e.status === 429 ? messageErreur(e) : 'Serveur injoignable');
      },
    });
  }
}
