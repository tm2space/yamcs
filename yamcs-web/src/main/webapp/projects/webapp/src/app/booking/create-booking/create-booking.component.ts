import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { BookingService, GSProvider, BookingRequest } from '../booking.service';
import { WebappSdkModule, YamcsService, YaSelectOption, utils, MessageService } from '@yamcs/webapp-sdk';

@Component({
  standalone: true,
  imports: [WebappSdkModule],
  templateUrl: './create-booking.component.html',
  styleUrl: './create-booking.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateBookingComponent implements OnInit {
  form: UntypedFormGroup;
  providers: GSProvider[] = [];

  providerOptions: YaSelectOption[] = [];
  groundStationOptions: YaSelectOption[] = [];

  passTypeOptions: YaSelectOption[] = [];
  purposeOptions: YaSelectOption[] = [];
  ruleTypeOptions: YaSelectOption[] = [];
  priorityOptions: YaSelectOption[] = [];
  frequencyBandOptions: YaSelectOption[] = [];

  showAdvancedOptions = false;
  isSubmitting = false;
  submissionStatus: 'idle' | 'success' | 'error' = 'idle';
  submissionMessage = '';

  constructor(
    private formBuilder: UntypedFormBuilder,
    private bookingService: BookingService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    readonly yamcs: YamcsService,
    private messageService: MessageService
  ) {
    const currentTime = yamcs.getMissionTime();
    const endTime = new Date(currentTime.getTime() + (15 * 60 * 1000)); // Default 15 minutes
    const satelliteName = yamcs.context ? yamcs.context.split('__')[0] : 'default'; // Extract satellite name from context (e.g., 'moi-1__realtime' -> 'moi-1')

    this.form = formBuilder.group({
      providerId: ['', Validators.required],
      satelliteName: [{value: satelliteName, disabled: true}], // Auto-populated and read-only
      startDateTime: [utils.toISOString(currentTime), Validators.required],
      durationMinutes: [15, [Validators.required, Validators.min(5), Validators.max(480)]],
      passType: ['both', Validators.required],
      purpose: ['telemetry', Validators.required],
      ruleType: ['one_time', Validators.required],
      frequencyDays: [''],
      notes: [''],
      priority: ['normal'],
      frequencyBand: ['S'],
      dataVolume: [100]
    });
  }

  ngOnInit() {
    this.loadProviders();
    this.loadEnumValues();
    this.setupFormValidation();
    this.setupGroundStations();
  }

  private setupGroundStations() {
    // Initialize ground station options based on provider selection
    this.form.get('providerId')?.valueChanges.subscribe((providerId) => {
      if (providerId) {
        this.updateGroundStationOptions(providerId);
      }
    });
  }

  private updateGroundStationOptions(providerId: string) {
    // Update ground station options based on selected provider
    const provider = this.providers.find(p => p.id.toString() === providerId);
    if (provider) {
      // For now, use default ground stations. This can be extended to fetch from provider API
      switch(provider.type) {
        case 'leafspace':
          this.groundStationOptions = [
            { id: 'LEAF_AZORES', label: 'Azores Ground Station' },
            { id: 'LEAF_LISBON', label: 'Lisbon Ground Station' }
          ];
          break;
        case 'dhruva':
          this.groundStationOptions = [
            { id: 'DHRUVA_BLR', label: 'Bangalore Ground Station' },
            { id: 'DHRUVA_HYD', label: 'Hyderabad Ground Station' }
          ];
          break;
        case 'isro':
          this.groundStationOptions = [
            { id: 'ISRO_ISTRAC', label: 'ISTRAC Bangalore' },
            { id: 'ISRO_LUCKNOW', label: 'Lucknow Ground Station' }
          ];
          break;
        default:
          this.groundStationOptions = [
            { id: 'DEFAULT_GS', label: 'Default Ground Station' }
          ];
      }
      this.cdr.markForCheck();
    }
  }

  private loadProviders() {
    this.bookingService.getProviders().subscribe({
      next: (providers) => {
        this.providers = providers || [];
        this.providerOptions = this.providers.map(p => ({
          id: p.id.toString(),
          label: `${p.name} (${p.type})`
        }));
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading providers:', error);
        this.providers = [];
        this.providerOptions = [];
        this.cdr.markForCheck();
      }
    });
  }

  private loadEnumValues() {
    this.bookingService.getEnumValues().subscribe({
      next: (enums) => {
        // Convert enum values to select options
        this.passTypeOptions = enums.passTypes.map(type => ({
          id: type,
          label: this.formatEnumLabel(type)
        }));

        this.purposeOptions = enums.purposeTypes.map(type => ({
          id: type,
          label: this.formatEnumLabel(type)
        }));

        this.ruleTypeOptions = enums.ruleTypes.map(type => ({
          id: type,
          label: this.formatRuleTypeLabel(type)
        }));

        // Populate advanced options
        this.priorityOptions = [
          { id: 'low', label: 'Low' },
          { id: 'normal', label: 'Normal' },
          { id: 'high', label: 'High' },
          { id: 'urgent', label: 'Urgent' }
        ];

        this.frequencyBandOptions = [
          { id: 'S', label: 'S-Band' },
          { id: 'X', label: 'X-Band' },
          { id: 'Ku', label: 'Ku-Band' },
          { id: 'Ka', label: 'Ka-Band' }
        ];

        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading enum values:', error);
        // Fallback to default values if API fails
        this.setDefaultEnumValues();
        this.cdr.markForCheck();
      }
    });
  }

  private formatEnumLabel(enumValue: string): string {
    return enumValue.charAt(0).toUpperCase() + enumValue.slice(1).replace('_', ' ');
  }

  private formatRuleTypeLabel(ruleType: string): string {
    switch (ruleType) {
      case 'one_time': return 'One-time booking';
      case 'daily': return 'Daily recurring';
      case 'weekly': return 'Weekly recurring';
      case 'monthly': return 'Monthly recurring';
      default: return this.formatEnumLabel(ruleType);
    }
  }

  private setDefaultEnumValues() {
    // Fallback enum values if API fails
    this.passTypeOptions = [
      { id: 'downlink', label: 'Downlink' },
      { id: 'uplink', label: 'Uplink' },
      { id: 'both', label: 'Both' }
    ];

    this.purposeOptions = [
      { id: 'telemetry', label: 'Telemetry' },
      { id: 'command', label: 'Command' },
      { id: 'routine', label: 'Routine' },
      { id: 'emergency', label: 'Emergency' }
    ];

    this.ruleTypeOptions = [
      { id: 'one_time', label: 'One-time booking' },
      { id: 'daily', label: 'Daily recurring' },
      { id: 'weekly', label: 'Weekly recurring' },
      { id: 'monthly', label: 'Monthly recurring' }
    ];
  }

  private setupFormValidation() {
    // Show/hide frequency field based on rule type
    this.form.get('ruleType')?.valueChanges.subscribe((ruleType) => {
      const frequencyControl = this.form.get('frequencyDays');
      if (ruleType === 'one_time') {
        frequencyControl?.clearValidators();
        frequencyControl?.setValue('');
      } else {
        frequencyControl?.setValidators([Validators.required, Validators.min(1)]);
      }
      frequencyControl?.updateValueAndValidity();
    });
  }

  onSubmit() {
    if (this.form.valid && !this.isSubmitting) {
      this.isSubmitting = true;

      const formValue = this.form.value;
      const provider = this.providers.find(p => p.id === parseInt(formValue.providerId));

      const bookingRequest: BookingRequest = {
        provider: provider?.name || 'Unknown Provider',
        satelliteId: this.form.get('satelliteName')?.value, // Get from disabled field
        startTime: utils.toISOString(formValue.startDateTime),
        durationMinutes: formValue.durationMinutes,
        passType: formValue.passType,
        purpose: formValue.purpose,
        ruleType: formValue.ruleType,
        frequencyDays: formValue.ruleType !== 'one_time' ? (formValue.frequencyDays || 1) : undefined,
        notes: formValue.notes || undefined
      };

      this.bookingService.createBooking(bookingRequest).subscribe({
        next: (booking) => {
          console.log('Booking created successfully:', booking);
          this.isSubmitting = false;
          this.cdr.markForCheck();

          // Show success message with details
          const startTime = new Date(booking.startTime).toLocaleString();
          this.messageService.showInfo(
            `🎉 Booking created successfully! ${booking.satelliteId} pass on ${booking.provider} starting ${startTime}`
          );

          // Navigate after showing success message
          setTimeout(() => {
            this.router.navigate(['/booking'], {
              queryParams: { c: this.yamcs.context }
            });
          }, 1500);
        },
        error: (error) => {
          console.error('Error creating booking:', error);
          this.isSubmitting = false;
          this.cdr.markForCheck();

          // Show detailed error message
          const errorMessage = error?.error?.msg || error?.message || 'Unknown error occurred';
          this.messageService.showError(`❌ Failed to create booking: ${errorMessage}`);
        }
      });
    }
  }

  onCancel() {
    this.router.navigate(['/booking'], {
      queryParams: { c: this.yamcs.context }
    });
  }

  showFrequencyField(): boolean {
    return this.form.get('ruleType')?.value !== 'one_time';
  }

  toggleAdvancedOptions(): void {
    this.showAdvancedOptions = !this.showAdvancedOptions;
  }

  getFrequencyLabel(): string {
    const ruleType = this.form.get('ruleType')?.value;
    switch (ruleType) {
      case 'daily':
        return 'Frequency (days)';
      case 'weekly':
        return 'Frequency (weeks)';
      case 'monthly':
        return 'Frequency (months)';
      default:
        return 'Frequency (days)';
    }
  }

  getFrequencyNote(): string {
    const ruleType = this.form.get('ruleType')?.value;
    switch (ruleType) {
      case 'daily':
        return 'Number of days between recurring bookings';
      case 'weekly':
        return 'Number of weeks between recurring bookings';
      case 'monthly':
        return 'Number of months between recurring bookings';
      default:
        return 'Number of days between recurring bookings';
    }
  }
}