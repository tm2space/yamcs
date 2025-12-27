import { Injectable } from '@angular/core';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { YamcsService } from './yamcs.service';

const STORAGE_KEY = 'yamcs.tZero';

@Injectable({
  providedIn: 'root',
})
export class MissionTimerService {
  readonly tZero$ = new BehaviorSubject<Date | null>(null);

  readonly missionElapsed$: Observable<number | null>;

  constructor(private yamcs: YamcsService) {
    // Load T-zero from localStorage on init
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const date = new Date(stored);
        if (!isNaN(date.getTime())) {
          this.tZero$.next(date);
        }
      } catch {
        // Invalid stored value, ignore
      }
    }

    // Compute mission elapsed time based on current mission time and T-zero
    this.missionElapsed$ = combineLatest([this.yamcs.time$, this.tZero$]).pipe(
      map(([timeStr, tZero]) => {
        if (!timeStr || !tZero) {
          return null;
        }
        const missionTime = new Date(timeStr);
        return missionTime.getTime() - tZero.getTime();
      }),
    );
  }

  setTZero(date: Date): void {
    this.tZero$.next(date);
    localStorage.setItem(STORAGE_KEY, date.toISOString());
  }

  clearTZero(): void {
    this.tZero$.next(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  getTZero(): Date | null {
    return this.tZero$.getValue();
  }
}
