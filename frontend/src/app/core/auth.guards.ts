import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Page réservée aux utilisateurs connectés. */
export const connecteGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return (await auth.verifier()) ? true : router.createUrlTree(['/login']);
};

/** Page réservée à l'administrateur (tarifs, paramètres). */
export const adminGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const u = await auth.verifier();
  if (!u) return router.createUrlTree(['/login']);
  return u.role === 'ADMIN' ? true : router.createUrlTree(['/']);
};

/** Renvoie vers la page de connexion si la session a expiré. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  return next(req).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && !req.url.startsWith('/api/auth/')) {
        auth.sessionExpiree();
      }
      return throwError(() => e);
    }),
  );
};
