import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'missionTimer',
  pure: true,
})
export class MissionTimerPipe implements PipeTransform {
  transform(diffMs: number | null): string {
    if (diffMs === null) {
      return 'T±000d:00h:00m:00s';
    }

    const prefix = diffMs < 0 ? 'T-' : 'T+';
    const absDiff = Math.abs(diffMs);

    const days = Math.floor(absDiff / 86400000);
    const hours = Math.floor((absDiff % 86400000) / 3600000);
    const minutes = Math.floor((absDiff % 3600000) / 60000);
    const seconds = Math.floor((absDiff % 60000) / 1000);

    const ddd = days.toString().padStart(3, '0');
    const hh = hours.toString().padStart(2, '0');
    const mm = minutes.toString().padStart(2, '0');
    const ss = seconds.toString().padStart(2, '0');

    return `${prefix}${ddd}d:${hh}h:${mm}m:${ss}s`;
  }
}
