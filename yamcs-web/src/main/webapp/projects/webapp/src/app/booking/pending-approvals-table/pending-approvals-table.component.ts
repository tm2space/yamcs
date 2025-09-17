import { ChangeDetectionStrategy, Component, Input, Output, EventEmitter } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { WebappSdkModule } from '@yamcs/webapp-sdk';
import { GSBooking } from '../booking.service';

@Component({
  selector: 'app-pending-approvals-table',
  templateUrl: './pending-approvals-table.component.html',
  styleUrl: './pending-approvals-table.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [WebappSdkModule],
})
export class PendingApprovalsTableComponent {
  @Input() set pendingBookings(bookings: GSBooking[]) {
    this.dataSource.data = bookings;
  }

  @Output() approve = new EventEmitter<GSBooking>();
  @Output() reject = new EventEmitter<GSBooking>();

  dataSource = new MatTableDataSource<GSBooking>([]);

  displayedColumns = [
    'startTime',
    'endTime',
    'yamcsGsName',
    'providerName',
    'missionName',
    'status',
    'actions'
  ];

  approveBooking(booking: GSBooking) {
    this.approve.emit(booking);
  }

  rejectBooking(booking: GSBooking) {
    this.reject.emit(booking);
  }
}