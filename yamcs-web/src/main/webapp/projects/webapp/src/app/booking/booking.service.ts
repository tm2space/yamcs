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

// Remove this interface - gs_status is now part of GSBooking

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
}