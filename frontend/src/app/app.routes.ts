import { Routes } from '@angular/router';
import { SyntheseComponent } from './pages/synthese/synthese.component';
import { RechargesComponent } from './pages/recharges/recharges.component';
import { RelevesComponent } from './pages/releves/releves.component';
import { TarifsComponent } from './pages/tarifs/tarifs.component';
import { ParametresComponent } from './pages/parametres/parametres.component';

export const routes: Routes = [
  { path: '', component: SyntheseComponent, title: 'Zoé · Synthèse' },
  { path: 'recharges', component: RechargesComponent, title: 'Zoé · Recharges' },
  { path: 'kilometrage', component: RelevesComponent, title: 'Zoé · Kilométrage' },
  { path: 'tarifs', component: TarifsComponent, title: 'Zoé · Tarifs' },
  { path: 'parametres', component: ParametresComponent, title: 'Zoé · Paramètres' },
  { path: '**', redirectTo: '' },
];
