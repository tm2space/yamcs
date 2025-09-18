import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { BookingService, GSProvider, GSBooking } from '../booking.service';
import { WebappSdkModule, AuthService, User } from '@yamcs/webapp-sdk';
import { PendingApprovalsOverlayComponent } from '../pending-approvals-overlay/pending-approvals-overlay.component';

@Component({
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    WebappSdkModule,
    PendingApprovalsOverlayComponent,
  ],
  templateUrl: './booking-page.component.html',
  styleUrl: './booking-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingPageComponent implements OnInit {
  private user: User;
  providers: GSProvider[] = [];
  recentBookings: GSBooking[] = [];
  pendingBookings: GSBooking[] = [];

  pendingCount = 0;
  activeCount = 0;
  providerCount = 0;

  displayedColumns = ['startTime', 'satelliteId', 'provider', 'purpose', 'status'];

  constructor(
    private bookingService: BookingService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.user = authService.getUser()!;
  }

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    // Load providers
    this.bookingService.getProviders().subscribe({
      next: (providers) => {
        this.providers = providers || [];
        this.providerCount = (providers || []).length;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading providers:', error);
        this.providers = [];
        this.providerCount = 0;
        this.cdr.markForCheck();
      }
    });

    // Load pending bookings
    this.bookingService.getPendingBookings().subscribe({
      next: (bookings) => {
        this.pendingBookings = bookings || [];
        this.pendingCount = (bookings || []).length;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading pending bookings:', error);
        this.pendingBookings = [];
        this.pendingCount = 0;
        this.cdr.markForCheck();
      }
    });

    // Load recent bookings
    this.bookingService.getBookings().subscribe({
      next: (bookings) => {
        const safeBookings = bookings || [];
        // Get recent bookings (last 10)
        this.recentBookings = safeBookings.slice(0, 10);

        // Count active bookings (approved or pending)
        this.activeCount = safeBookings.filter(b =>
          b.status === 'approved' || b.status === 'pending'
        ).length;

        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading bookings:', error);
        this.recentBookings = [];
        this.activeCount = 0;
        this.cdr.markForCheck();
      }
    });
  }

  showApprovals() {
    // You can add permission check here if needed
    return true;
  }

  onApproveBooking(booking: GSBooking) {
    this.bookingService.approveBooking(booking.id).subscribe({
      next: () => {
        console.log('Booking approved successfully');
        this.loadData(); // Reload data to update the UI
      },
      error: (error) => {
        console.error('Error approving booking:', error);
      }
    });
  }

  onRejectBooking(booking: GSBooking) {
    const reason = prompt('Please enter rejection reason:');
    if (reason) {
      this.bookingService.rejectBooking(booking.id, reason).subscribe({
        next: () => {
          console.log('Booking rejected successfully');
          this.loadData(); // Reload data to update the UI
        },
        error: (error) => {
          console.error('Error rejecting booking:', error);
        }
      });
    }
  }
}