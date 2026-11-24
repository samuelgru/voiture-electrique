import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (auth.utilisateur(); as u) {
      <header class="topbar">
        <div class="brand">
          <span class="logo">⚡</span>
          <span>Ma Zoé</span>
        </div>
        <nav>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Synthèse</a>
          <a routerLink="/recharges" routerLinkActive="active">Recharges</a>
          <a routerLink="/kilometrage" routerLinkActive="active">Kilométrage</a>
          @if (auth.estAdmin()) {
            <a routerLink="/tarifs" routerLinkActive="active">Tarifs</a>
            <a routerLink="/parametres" routerLinkActive="active">Paramètres</a>
          }
        </nav>
        <div class="session">
          @if (auth.estInvite()) {
            <span class="muted" [title]="'Adresse vue par le serveur : ' + (u.ip ?? '?')">Consultation · réseau local</span>
            <button class="icon" routerLink="/login">Mode admin</button>
          } @else {
            <span class="muted">{{ u.identifiant }}@if (auth.estAdmin()) { · admin }</span>
            <button class="icon" (click)="auth.deconnecter()">
              {{ auth.estAdmin() ? 'Quitter le mode admin' : 'Déconnexion' }}
            </button>
          }
        </div>
      </header>
    }
    <main class="container">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {
  auth = inject(AuthService);
}
