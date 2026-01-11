import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  BookingService,
  GSProvider,
  ProviderSatellite,
  ProviderGroundStation,
  ActivityScope,
  ProviderContact,
  ReserveContactRequest
} from '../booking.service';
import { WebappSdkModule, YamcsService, YaSelectOption, MessageService } from '@yamcs/webapp-sdk';

@Component({
  standalone: true,
  imports: [WebappSdkModule],
  templateUrl: './create-booking.component.html',
  styleUrl: './create-booking.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateBookingComponent implements OnInit {
  form: UntypedFormGroup;

  // Provider data
  providers: GSProvider[] = [];
  providerOptions: YaSelectOption[] = [];

  // Data from provider APIs
  satellites: ProviderSatellite[] = [];
  satelliteOptions: YaSelectOption[] = [];

  groundStations: ProviderGroundStation[] = [];
  groundStationOptions: YaSelectOption[] = [];

  activityScopes: ActivityScope[] = [];
  activityScopeOptions: YaSelectOption[] = [];
  selectedActivityScope: ActivityScope | null = null;

  contacts: ProviderContact[] = [];
  selectedContact: ProviderContact | null = null;

  // UI state
  isLoadingSatellites = false;
  isLoadingGroundStations = false;
  isLoadingContacts = false;
  isSubmitting = false;

  // Selected provider type for API calls
  selectedProviderType: string = '';

  // Purpose options for booking
  purposeOptions: YaSelectOption[] = [
    { id: 'telemetry', label: 'Telemetry' },
    { id: 'command', label: 'Command' },
    { id: 'routine', label: 'Routine' },
    { id: 'emergency', label: 'Emergency' }
  ];

  constructor(
    private formBuilder: UntypedFormBuilder,
    private bookingService: BookingService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    readonly yamcs: YamcsService,
    private messageService: MessageService
  ) {
    // Get next 7 days for contact search
    const today = new Date();
    const nextWeek = new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000);

    this.form = formBuilder.group({
      providerId: ['', Validators.required],
      satelliteId: ['', Validators.required],
      groundStationId: ['', Validators.required],
      activityScopeId: ['', Validators.required],
      contactId: [''],
      purpose: ['telemetry', Validators.required],
      notes: [''],
      startDate: [today.toISOString().split('T')[0], Validators.required],
      endDate: [nextWeek.toISOString().split('T')[0], Validators.required]
    });
  }

  ngOnInit() {
    this.loadProviders();
    this.setupFormListeners();
  }

  private setupFormListeners() {
    // When provider changes, load satellites
    this.form.get('providerId')?.valueChanges.subscribe((providerId) => {
      if (providerId) {
        const provider = this.providers.find(p => p.id.toString() === providerId);
        if (provider) {
          this.selectedProviderType = provider.type;
          this.loadSatellites(provider.type);
        }
      } else {
        this.clearSatellites();
      }
    });

    // When satellite changes, load ground stations and activity scopes
    this.form.get('satelliteId')?.valueChanges.subscribe((satelliteId) => {
      if (satelliteId && this.selectedProviderType) {
        this.loadGroundStations(this.selectedProviderType);
        this.loadActivityScopes(this.selectedProviderType, satelliteId);
      } else {
        this.clearGroundStations();
      }
    });

    // When activity scope changes, update selected scope
    this.form.get('activityScopeId')?.valueChanges.subscribe((scopeId) => {
      if (scopeId) {
        this.selectedActivityScope = this.activityScopes.find(s => s.spbasId === scopeId) || null;
      } else {
        this.selectedActivityScope = null;
      }
      this.clearContacts();
    });

    // When ground station changes, clear contacts (user needs to search again)
    this.form.get('groundStationId')?.valueChanges.subscribe((gsId) => {
      this.clearContacts();
    });
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
        // Fallback to hardcoded providers if DB not available
        // Use provider type as both ID and type for consistency
        this.providers = [
          { id: 1, name: 'Dhruva Space', type: 'dhruva', isActive: true, createdAt: '', updatedAt: '' },
          { id: 2, name: 'Leafspace', type: 'leafspace', isActive: true, createdAt: '', updatedAt: '' },
          { id: 3, name: 'ISRO', type: 'isro', isActive: true, createdAt: '', updatedAt: '' }
        ];
        this.providerOptions = this.providers.map(p => ({
          id: p.id.toString(),
          label: `${p.name} (${p.type})`
        }));
        this.cdr.markForCheck();
      }
    });
  }

  private loadSatellites(providerType: string) {
    this.isLoadingSatellites = true;
    this.clearSatellites();
    this.cdr.markForCheck();

    this.bookingService.getProviderSatellites(providerType).subscribe({
      next: (satellites) => {
        this.satellites = satellites;
        this.satelliteOptions = satellites.map(s => ({
          id: s.id,
          label: `${s.name}${s.noradId ? ` (NORAD: ${s.noradId})` : ''}`
        }));
        this.isLoadingSatellites = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading satellites:', error);
        this.messageService.showError('Failed to load satellites from provider');
        this.isLoadingSatellites = false;
        this.cdr.markForCheck();
      }
    });
  }

  private loadGroundStations(providerType: string) {
    this.isLoadingGroundStations = true;
    this.clearGroundStations();
    this.cdr.markForCheck();

    this.bookingService.getProviderGroundStations(providerType).subscribe({
      next: (groundStations) => {
        this.groundStations = groundStations;
        this.groundStationOptions = groundStations.map(gs => ({
          id: gs.id,
          label: `${gs.name}${gs.city ? ` - ${gs.city}` : ''}`
        }));
        this.isLoadingGroundStations = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading ground stations:', error);
        this.messageService.showError('Failed to load ground stations from provider');
        this.isLoadingGroundStations = false;
        this.cdr.markForCheck();
      }
    });
  }

  private loadActivityScopes(providerType: string, satelliteId: string) {
    this.bookingService.getActivityScopes(providerType, satelliteId).subscribe({
      next: (scopes) => {
        this.activityScopes = scopes;
        this.activityScopeOptions = scopes.map(s => ({
          id: s.spbasId,
          label: `${s.activityScope || 'Unknown'} - ${s.taskName || 'Task'}${s.communicationBand ? ` (${s.communicationBand})` : ''}`
        }));
        // Auto-select if only one scope
        if (scopes.length === 1) {
          this.form.get('activityScopeId')?.setValue(scopes[0].spbasId);
        }
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading activity scopes:', error);
        this.activityScopes = [];
        this.activityScopeOptions = [];
        this.selectedActivityScope = null;
        this.cdr.markForCheck();
      }
    });
  }

  loadContacts() {
    if (!this.selectedProviderType || !this.selectedActivityScope) {
      return;
    }

    const gsId = this.form.get('groundStationId')?.value;
    const satelliteId = this.form.get('satelliteId')?.value;
    const startDate = this.form.get('startDate')?.value;
    const endDate = this.form.get('endDate')?.value;

    if (!gsId || !satelliteId || !startDate || !endDate) {
      return;
    }

    this.isLoadingContacts = true;
    this.contacts = [];
    this.selectedContact = null;
    this.cdr.markForCheck();

    this.bookingService.getProviderContacts(
      this.selectedProviderType,
      gsId,
      satelliteId,
      this.selectedActivityScope.spbasId,
      startDate,
      endDate
    ).subscribe({
      next: (contacts) => {
        this.contacts = contacts;
        this.isLoadingContacts = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading contacts:', error);
        this.messageService.showError('Failed to load available passes from provider');
        this.isLoadingContacts = false;
        this.cdr.markForCheck();
      }
    });
  }

  private clearSatellites() {
    this.satellites = [];
    this.satelliteOptions = [];
    this.form.get('satelliteId')?.setValue('');
    this.clearGroundStations();
  }

  private clearGroundStations() {
    this.groundStations = [];
    this.groundStationOptions = [];
    this.activityScopes = [];
    this.activityScopeOptions = [];
    this.selectedActivityScope = null;
    this.form.get('groundStationId')?.setValue('');
    this.form.get('activityScopeId')?.setValue('');
    this.clearContacts();
  }

  private clearContacts() {
    this.contacts = [];
    this.selectedContact = null;
    this.form.get('contactId')?.setValue('');
  }

  selectContact(contact: ProviderContact) {
    this.selectedContact = contact;
    this.form.get('contactId')?.setValue(contact.gsVisibilityId);
    this.cdr.markForCheck();
  }

  formatDateTime(isoString: string): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString();
  }

  formatDuration(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}m ${secs}s`;
  }

  onSubmit() {
    if (!this.selectedContact || this.isSubmitting) {
      if (!this.selectedContact) {
        this.messageService.showError('Please select an available pass to book');
      }
      return;
    }

    this.isSubmitting = true;
    this.cdr.markForCheck();

    // Get the satellite name from the satellites array
    const selectedSatelliteId = this.form.get('satelliteId')?.value;
    const selectedSatellite = this.satellites.find(s => s.id === selectedSatelliteId);

    const request: ReserveContactRequest = {
      provider: this.selectedProviderType,
      gsId: this.form.get('groundStationId')?.value,
      satelliteId: selectedSatelliteId,
      gsVisibilityId: this.selectedContact.gsVisibilityId,
      gsabracId: this.selectedActivityScope?.gsabracId || '',
      purpose: this.form.get('purpose')?.value,
      notes: this.form.get('notes')?.value,
      satelliteName: selectedSatellite?.name || ''
    };

    this.bookingService.reserveContact(request).subscribe({
      next: (booking) => {
        console.log('Contact reserved successfully:', booking);
        this.isSubmitting = false;
        this.cdr.markForCheck();

        const startTime = new Date(booking.startDateTime).toLocaleString();
        this.messageService.showInfo(
          `Booking created successfully! Pass starting ${startTime}`
        );

        setTimeout(() => {
          this.router.navigate(['/booking'], {
            queryParams: { c: this.yamcs.context }
          });
        }, 1500);
      },
      error: (error) => {
        console.error('Error reserving contact:', error);
        this.isSubmitting = false;
        this.cdr.markForCheck();

        const errorMessage = error?.error?.msg || error?.message || 'Unknown error occurred';
        this.messageService.showError(`Failed to reserve contact: ${errorMessage}`);
      }
    });
  }

  onCancel() {
    this.router.navigate(['/booking'], {
      queryParams: { c: this.yamcs.context }
    });
  }

  // Check if contacts can be loaded
  canLoadContacts(): boolean {
    return !!(
      this.selectedProviderType &&
      this.form.get('satelliteId')?.value &&
      this.form.get('groundStationId')?.value &&
      this.selectedActivityScope
    );
  }

  // Check if a contact is available for booking
  isContactAvailable(contact: ProviderContact): boolean {
    if (!contact.status) return false;
    const status = contact.status.toLowerCase();
    return status === 'available' || status === 'slot_available' || status === 'active';
  }
}
