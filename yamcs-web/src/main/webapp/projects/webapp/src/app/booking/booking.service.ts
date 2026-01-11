import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { YamcsService } from '@yamcs/webapp-sdk';

export interface GSProvider {
  id: number;
  name: string;
  type: 'leafspace' | 'dhruva' | 'isro';
  contactEmail?: string;
  contactPhone?: string;
  apiEndpoint?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GSBooking {
  id: number;
  provider: string;
  satelliteId: string;
  startTime: string;
  endTime: string; // computed field
  durationMinutes: number;
  passType: string; // enum from database
  purpose: string; // enum from database
  ruleType: string; // enum from database
  frequencyDays?: number;
  notes?: string;
  status: string; // enum from database
  gsStatus: string; // enum from database
  requestedBy: string;
  approvedBy?: string;
  approvedAt?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
  // Provider-specific fields
  providerBookingId?: string;
  providerSatelliteId?: string;
  maxElevation?: number;
}

export interface BookingRequest {
  provider: string;
  satelliteId: string;
  startTime: string;
  durationMinutes: number;
  passType: string; // enum from database
  purpose: string; // enum from database
  ruleType: string; // enum from database
  frequencyDays?: number;
  notes?: string;
}

export interface ApprovalRequest {
  comments?: string;
}

export interface EnumValues {
  providerTypes: string[];
  ruleTypes: string[];
  statusTypes: string[];
  passTypes: string[];
  purposeTypes: string[];
  gsStatusTypes: string[];
}

// Provider API interfaces
export interface ProviderSatellite {
  id: string;
  name: string;
  noradId?: string;
}

export interface ProviderGroundStation {
  id: string;
  name: string;
  city?: string;
  state?: string;
  country?: string;
  latitude: number;
  longitude: number;
}

export interface ActivityScope {
  gsabracId: string;
  spbasId: string;
  satelliteId: string;
  activityScope?: string;
  taskName?: string;
  communicationBand?: string;
}

export interface ProviderContact {
  gsVisibilityId: string;
  gsId: string;
  groundStationName?: string;
  satelliteId: string;
  passStart: string;
  passEnd: string;
  maxElevation: number;
  status: string;
  durationSeconds: number;
}

export interface ProviderBookingInfo {
  satellitePassBookingId: string;
  gsId: string;
  groundStationName?: string;
  gsVisibilityId: string;
  noradId?: number;
  startDateTime: string;
  endDateTime: string;
  status: string;
  maxElevation?: number;
}

export interface ReserveContactRequest {
  provider: string;
  gsId: string;
  satelliteId: string;
  gsVisibilityId: string;
  gsabracId: string;
  purpose?: string;
  notes?: string;
  satelliteName?: string;  // Human-readable satellite name
}

@Injectable({
  providedIn: 'root'
})
export class BookingService {

  constructor(private http: HttpClient, private yamcs: YamcsService) {}

  // Enum methods
  getEnumValues(): Observable<EnumValues> {
    return this.http.get<EnumValues>(`${this.yamcs.yamcsClient.baseHref}api/booking/enums`);
  }

  // Provider methods
  getProviders(): Observable<GSProvider[]> {
    return this.http.get<{providers: GSProvider[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/providers`)
      .pipe(map(response => response.providers));
  }

  // Booking methods
  getBookings(): Observable<GSBooking[]> {
    return this.http.get<{bookings: GSBooking[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/bookings`)
      .pipe(map(response => response.bookings));
  }

  getPendingBookings(): Observable<GSBooking[]> {
    return this.http.get<{bookings: GSBooking[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/bookings/pending`)
      .pipe(map(response => response.bookings));
  }

  createBooking(booking: BookingRequest): Observable<GSBooking> {
    return this.http.post<GSBooking>(`${this.yamcs.yamcsClient.baseHref}api/booking/bookings`, booking);
  }

  approveBooking(bookingId: number, comments?: string): Observable<{status: string}> {
    const request: ApprovalRequest = { comments };
    return this.http.post<{status: string}>(`${this.yamcs.yamcsClient.baseHref}api/booking/bookings/${bookingId}/approve`, request);
  }

  rejectBooking(bookingId: number, reason: string): Observable<{status: string}> {
    const request: ApprovalRequest = { comments: reason };
    return this.http.post<{status: string}>(`${this.yamcs.yamcsClient.baseHref}api/booking/bookings/${bookingId}/reject`, request);
  }

  // Update booking gs_status
  updateBookingGsStatus(bookingId: number, gsStatus: string): Observable<{status: string}> {
    return this.http.put<{status: string}>(`${this.yamcs.yamcsClient.baseHref}api/booking/bookings/${bookingId}/gsstatus`, { gsStatus });
  }

  // ============== Provider API Methods ==============
  // These methods call the external provider APIs through the backend abstraction layer

  /**
   * Get satellites available from the specified provider
   * @param providerType Provider type (e.g., 'dhruva', 'leafspace', 'isro')
   */
  getProviderSatellites(providerType: string): Observable<ProviderSatellite[]> {
    return this.http.get<{satellites: ProviderSatellite[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/provider/${providerType}/satellites`)
      .pipe(map(response => response.satellites || []));
  }

  /**
   * Get ground stations available from the specified provider
   * @param providerType Provider type (e.g., 'dhruva', 'leafspace', 'isro')
   */
  getProviderGroundStations(providerType: string): Observable<ProviderGroundStation[]> {
    return this.http.get<{groundStations: ProviderGroundStation[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/provider/${providerType}/groundstations`)
      .pipe(map(response => response.groundStations || []));
  }

  /**
   * Get activity scopes for a satellite from the provider
   * @param providerType Provider type
   * @param satelliteId Satellite ID from the provider
   */
  getActivityScopes(providerType: string, satelliteId: string): Observable<ActivityScope[]> {
    return this.http.get<{activityScopes: ActivityScope[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/provider/${providerType}/satellites/${satelliteId}/activityscopes`)
      .pipe(map(response => response.activityScopes || []));
  }

  /**
   * Get available contacts (passes/visibility windows) from the provider
   * @param providerType Provider type
   * @param gsId Ground station ID
   * @param satelliteId Satellite ID
   * @param spbasId Activity scope ID
   * @param startDate Start date (ISO format)
   * @param endDate End date (ISO format)
   */
  getProviderContacts(
    providerType: string,
    gsId: string,
    satelliteId: string,
    spbasId: string,
    startDate: string,
    endDate: string
  ): Observable<ProviderContact[]> {
    const params = new URLSearchParams({
      gsId,
      satelliteId,
      spbasId,
      startDate,
      endDate
    });
    return this.http.get<{contacts: ProviderContact[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/provider/${providerType}/contacts?${params.toString()}`)
      .pipe(map(response => response.contacts || []));
  }

  /**
   * Reserve a contact (book a pass) with the provider
   * @param request Reserve contact request
   */
  reserveContact(request: ReserveContactRequest): Observable<ProviderBookingInfo> {
    return this.http.post<ProviderBookingInfo>(`${this.yamcs.yamcsClient.baseHref}api/booking/provider/${request.provider}/reserve`, request);
  }

  /**
   * Cancel a booking with the provider
   * @param providerType Provider type
   * @param bookingId Provider's booking ID
   */
  cancelProviderBooking(providerType: string, bookingId: string): Observable<{success: boolean, message: string}> {
    return this.http.post<{success: boolean, message: string}>(`${this.yamcs.yamcsClient.baseHref}api/booking/provider/${providerType}/cancel`, {
      provider: providerType,
      satellitePassBookingId: bookingId
    });
  }

  /**
   * Get all bookings from the provider
   * @param providerType Provider type
   */
  getProviderBookings(providerType: string): Observable<ProviderBookingInfo[]> {
    return this.http.get<{bookings: ProviderBookingInfo[]}>(`${this.yamcs.yamcsClient.baseHref}api/booking/provider/${providerType}/bookings`)
      .pipe(map(response => response.bookings || []));
  }
}