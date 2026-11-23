import { Routes } from '@angular/router';
import { adminGuard, connecteGuard } from './core/auth.guards';
import { LoginComponent } from './pages/login/login.component';
import { SyntheseComponent } from './pages/synthese/synthese.component';
import { RechargesComponent } from './pages/recharges/recharges.component';
import { RelevesComponent } from './pages/releves/releves.component';
import { TarifsComponent } from './pages/tarifs/tarifs.component';
import { ParametresComponent } from './pages/parametres/parametres.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, title: 'Zoé · Connexion' },
  { path: '', component: SyntheseComponent, title: 'Zoé · Synthèse', canActivate: [connecteGuard] },
  { path: 'recharges', component: RechargesComponent, title: 'Zoé · Recharges', canActivate: [connecteGuard] },
  { path: 'kilometrage', component: RelevesComponent, title: 'Zoé · Kilométrage', canActivate: [connecteGuard] },
  { path: 'tarifs', component: TarifsComponent, title: 'Zoé · Tarifs', canActivate: [adminGuard] },
  { path: 'parametres', component: ParametresComponent, title: 'Zoé · Paramètres', canActivate: [adminGuard] },
  { path: '**', redirectTo: '' },
];
