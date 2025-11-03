import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnChanges,
} from '@angular/core';
import {
  ContextAlarmInfo,
  Parameter,
  ParameterMember,
  ParameterType,
  ParameterValue,
  Sample,
  WebappSdkModule,
  YamcsService,
  utils,
} from '@yamcs/webapp-sdk';
import { BehaviorSubject } from 'rxjs';
import { AlarmLevelComponent } from '../../../shared/alarm-level/alarm-level.component';
import { ExpressionComponent } from '../../../shared/expression/expression.component';
import { MarkdownComponent } from '../../../shared/markdown/markdown.component';
import { SeverityMeterComponent } from '../severity-meter/severity-meter.component';

@Component({
  selector: 'app-parameter-detail',
  templateUrl: './parameter-detail.component.html',
  styleUrl: './parameter-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AlarmLevelComponent,
    ExpressionComponent,
    MarkdownComponent,
    SeverityMeterComponent,
    WebappSdkModule,
  ],
})
export class ParameterDetailComponent implements OnChanges {
  @Input()
  parameter: Parameter;

  @Input()
  offset: string;

  @Input()
  pval?: ParameterValue;

  // A Parameter or a Member depending on whether the user is visiting
  // nested entries of an aggregate or array.
  entry$ = new BehaviorSubject<Parameter | ParameterMember | null>(null);
  ptype$ = new BehaviorSubject<ParameterType | null>(null);
  lastKnownSample$ = new BehaviorSubject<Sample | null>(null);
  loadingLastKnown$ = new BehaviorSubject<boolean>(false);

  constructor(readonly yamcs: YamcsService) {}

  ngOnChanges() {
    if (this.parameter) {
      if (this.offset) {
        const entry = utils.getEntryForOffset(this.parameter, this.offset);
        this.entry$.next(entry);
      } else {
        this.entry$.next(this.parameter);
      }
      this.ptype$.next(utils.getParameterTypeForPath(this.parameter) || null);

      // Fetch last known value from parameter archive
      this.fetchLastKnownValue();
    } else {
      this.entry$.next(null);
      this.ptype$.next(null);
      this.lastKnownSample$.next(null);
    }
  }

  private fetchLastKnownValue() {
    this.loadingLastKnown$.next(true);
    const qualifiedName = this.offset
      ? this.parameter.qualifiedName + this.offset
      : this.parameter.qualifiedName;

    this.yamcs.yamcsClient
      .getParameterSamples(this.yamcs.instance!, qualifiedName, {
        count: 1,
        order: 'desc',
        fields: ['time', 'avg'],
      })
      .then((samples) => {
        this.loadingLastKnown$.next(false);
        if (samples && samples.length > 0) {
          this.lastKnownSample$.next(samples[0]);
        } else {
          this.lastKnownSample$.next(null);
        }
      })
      .catch(() => {
        this.loadingLastKnown$.next(false);
        this.lastKnownSample$.next(null);
      });
  }

  getDefaultAlarmLevel(ptype: ParameterType, label: string) {
    if (ptype && ptype.defaultAlarm) {
      const alarm = ptype.defaultAlarm;
      if (alarm.enumerationAlarms) {
        for (const enumAlarm of alarm.enumerationAlarms) {
          if (enumAlarm.label === label) {
            return enumAlarm.level;
          }
        }
      }
      return alarm.defaultLevel;
    }
  }

  getEnumerationAlarmLevel(contextAlarm: ContextAlarmInfo, label: string) {
    const alarm = contextAlarm.alarm;
    for (const enumAlarm of alarm.enumerationAlarms) {
      if (enumAlarm.label === label) {
        return enumAlarm.level;
      }
    }
    return alarm.defaultLevel;
  }

  /**
   * Converts a numeric value to hexadecimal representation
   * Returns null if value is not a valid integer
   */
  toHex(value: any): string | null {
    if (value === null || value === undefined) {
      return null;
    }

    const num = typeof value === 'number' ? value : Number(value);

    if (isNaN(num) || !Number.isFinite(num)) {
      return null;
    }

    // Convert to integer (remove decimal part)
    const intValue = Math.floor(num);

    // Convert to hex
    const hexValue = intValue.toString(16).toUpperCase();
    return '0x' + hexValue;
  }

  /**
   * Checks if a value should display hex representation
   * Returns true for integer types
   */
  shouldShowHex(value: any): boolean {
    if (!value || typeof value !== 'object') {
      return false;
    }

    // Check if it's an integer type
    const integerTypes = ['UINT32', 'SINT32', 'UINT64', 'SINT64'];
    return integerTypes.includes(value.type);
  }

  /**
   * Gets the numeric value from a Value object
   */
  getNumericValue(value: any): number | null {
    if (!value || typeof value !== 'object') {
      return null;
    }

    switch (value.type) {
      case 'UINT32':
        return value.uint32Value;
      case 'SINT32':
        return value.sint32Value;
      case 'UINT64':
        return value.uint64Value;
      case 'SINT64':
        return value.sint64Value;
      default:
        return null;
    }
  }

  /**
   * Checks if the current parameter is an integer type
   * Used for displaying hex values in last known value section
   */
  isIntegerParameter(): boolean {
    const ptype = this.ptype$.value;
    if (!ptype || !ptype.engType) {
      return false;
    }

    const integerEngTypes = ['integer', 'uint32', 'sint32', 'uint64', 'sint64'];
    return integerEngTypes.includes(ptype.engType.toLowerCase());
  }
}
