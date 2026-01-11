import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup } from '@angular/forms';
import {
  CommandOption,
  ConfigService,
  Value,
  WebappSdkModule,
  YaSelectOption,
} from '@yamcs/webapp-sdk';
import { BehaviorSubject } from 'rxjs';
import { renderValue } from '../arguments/argument/argument.component';
import { TemplateProvider } from './TemplateProvider';
import { BookingService, GSBooking } from '../../../booking/booking.service';

@Component({
  standalone: true,
  selector: 'app-command-options-form',
  templateUrl: './command-options-form.component.html',
  styleUrl: './command-options-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [WebappSdkModule, CommonModule],
})
export class CommandOptionsForm implements OnInit, OnChanges {
  @Input()
  formGroup: FormGroup;

  @Input()
  templateProvider: TemplateProvider;

  commandOptions: CommandOption[];

  streamOptions$ = new BehaviorSubject<YaSelectOption[]>([]);
  bookingOptions$ = new BehaviorSubject<YaSelectOption[]>([]);
  bookings: GSBooking[] = [];
  selectedBooking: GSBooking | null = null;
  isLoadingBookings = false;

  constructor(
    configService: ConfigService,
    private bookingService: BookingService,
    private cdr: ChangeDetectorRef
  ) {
    this.commandOptions = configService.getCommandOptions();

    const streamOptions: YaSelectOption[] = configService
      .getTcStreams()
      .map((streamName) => ({ id: streamName, label: streamName }));
    this.streamOptions$.next(streamOptions);
  }

  ngOnInit(): void {
    this.formGroup.addControl('stream', new FormControl(''));
    this.formGroup.addControl('booking', new FormControl(''));
    for (const option of this.commandOptions) {
      this.formGroup.addControl('extra__' + option.id, new FormControl(null));
    }

    // Load available bookings
    this.loadBookings();

    // Subscribe to booking selection changes
    this.formGroup.controls['booking'].valueChanges.subscribe(bookingId => {
      this.selectedBooking = this.bookings.find(b => b.id.toString() === bookingId) || null;
      this.cdr.markForCheck();
    });
  }

  private loadBookings(): void {
    this.isLoadingBookings = true;
    this.bookingService.getBookings().subscribe({
      next: (bookings: GSBooking[]) => {
        // Filter to show only approved bookings
        this.bookings = bookings.filter((b: GSBooking) => b.status === 'approved');
        const options: YaSelectOption[] = this.bookings.map((b: GSBooking) => ({
          id: b.id.toString(),
          label: `${b.satelliteId} - ${this.formatDateTime(b.startTime)} (${b.provider})`
        }));
        this.bookingOptions$.next(options);
        this.isLoadingBookings = false;
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        console.error('Failed to load bookings:', err);
        this.isLoadingBookings = false;
        this.cdr.markForCheck();
      }
    });
  }

  private formatDateTime(isoString: string): string {
    return new Date(isoString).toLocaleString();
  }

  getSelectedBooking(): GSBooking | null {
    return this.selectedBooking;
  }

  ngOnChanges(): void {
    if (this.templateProvider) {
      for (const option of this.commandOptions || []) {
        const previousValue = this.templateProvider.getOption(
          option.id,
          option.type,
        );
        if (previousValue !== undefined) {
          this.formGroup.controls['extra__' + option.id].setValue(
            renderValue(previousValue),
          );
        }
      }
    }
  }

  getStream() {
    const control = this.formGroup.controls['stream'];
    return control.value || undefined;
  }

  getResult(struct = false) {
    const extra: { [key: string]: Value } = {};
    for (const id in this.formGroup.controls) {
      if (id.startsWith('extra__')) {
        const control = this.formGroup.controls[id];
        if (control.value !== null) {
          const optionId = id.replace('extra__', '');

          if (struct) {
            extra[optionId] = this.toStructValue(optionId, control.value);
          } else {
            extra[optionId] = this.toYamcsValue(optionId, control.value);
          }
        }
      }
    }
    // Add booking ID if selected
    const bookingControl = this.formGroup.controls['booking'];
    if (bookingControl && bookingControl.value) {
      const selected = this.bookings.find(b => b.id.toString() === bookingControl.value);
      if (selected?.providerBookingId) {
        extra['bookingId'] = { type: 'STRING', stringValue: selected.providerBookingId };
      }
    }
    return extra;
  }

  private toStructValue(optionId: string, controlValue: any): any {
    let option: CommandOption;
    for (const candidate of this.commandOptions) {
      if (candidate.id === optionId) {
        option = candidate;
      }
    }
    switch (option!.type) {
      case 'BOOLEAN':
        return controlValue === 'true';
      case 'NUMBER':
        return Number(controlValue);
      default:
        return String(controlValue);
    }
  }

  private toYamcsValue(optionId: string, controlValue: any): Value {
    let option: CommandOption;
    for (const candidate of this.commandOptions) {
      if (candidate.id === optionId) {
        option = candidate;
      }
    }
    switch (option!.type) {
      case 'BOOLEAN':
        if (controlValue === 'true') {
          return { type: 'BOOLEAN', booleanValue: true };
        }
        return { type: 'BOOLEAN', booleanValue: false };
      case 'NUMBER':
        return { type: 'SINT32', sint32Value: Number(controlValue) };
      default:
        return { type: 'STRING', stringValue: String(controlValue) };
    }
  }
}
