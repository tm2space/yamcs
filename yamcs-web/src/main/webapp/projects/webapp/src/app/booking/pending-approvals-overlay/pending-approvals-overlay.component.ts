import { ChangeDetectionStrategy, Component, Input, Output, EventEmitter } from '@angular/core';
import { WebappSdkModule } from '@yamcs/webapp-sdk';
import { BehaviorSubject } from 'rxjs';
import { GSBooking } from '../booking.service';
import { PendingApprovalsTableComponent } from '../pending-approvals-table/pending-approvals-table.component';

@Component({
  selector: 'app-pending-approvals-overlay',
  templateUrl: './pending-approvals-overlay.component.html',
  styleUrl: './pending-approvals-overlay.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [WebappSdkModule, PendingApprovalsTableComponent],
})
export class PendingApprovalsOverlayComponent {
  @Input() pendingBookings: GSBooking[] = [];
  @Input() showApprovals: boolean = true;

  @Output() approve = new EventEmitter<GSBooking>();
  @Output() reject = new EventEmitter<GSBooking>();

  isCollapsed$ = new BehaviorSubject<boolean>(true);

  toggleCollapse() {
    this.isCollapsed$.next(!this.isCollapsed$.value);
  }


  onApproveBooking(booking: GSBooking) {
    this.approve.emit(booking);
  }

  onRejectBooking(booking: GSBooking) {
    this.reject.emit(booking);
  }
}