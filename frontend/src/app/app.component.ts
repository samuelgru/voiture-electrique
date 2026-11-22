import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <div class="brand">
        <span class="logo">⚡</span>
        <span>Ma Zoé</span>
      </div>
      <nav>
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Synthèse</a>
        <a routerLink="/recharges" routerLinkActive="active">Recharges</a>
        <a routerLink="/kilometrage" routerLinkActive="active">Kilométrage</a>
        <a routerLink="/tarifs" routerLinkActive="active">Tarifs</a>
        <a routerLink="/parametres" routerLinkActive="active">Paramètres</a>
      </nav>
    </header>
    <main class="container">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {}
