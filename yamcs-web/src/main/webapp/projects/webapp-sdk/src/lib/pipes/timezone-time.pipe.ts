import { Pipe, PipeTransform } from '@angular/core';
import { formatInTimeZone } from 'date-fns-tz';

@Pipe({
  name: 'timezoneTime',
  pure: true,
})
export class TimezoneTimePipe implements PipeTransform {
  transform(dateStr: string | null, timezone: string): string {
    if (!dateStr) {
      return '--:--:--';
    }

    try {
      const date = new Date(dateStr);
      return formatInTimeZone(date, timezone, 'HH:mm:ss');
    } catch {
      return '--:--:--';
    }
  }
}
