import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UntypedFormBuilder, UntypedFormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { WebappSdkModule } from '@yamcs/webapp-sdk';
import { GSBooking } from '../booking.service';

interface DialogData {
  booking: GSBooking;
  action: 'approve' | 'reject';
}

@Component({
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    WebappSdkModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.action === 'approve' ? 'Approve' : 'Reject' }} Booking</h2>

    <mat-dialog-content>
      <form [formGroup]="approvalForm" class="ya-form">
        <ya-field label="Pass Details">
          <div class="booking-details">
            <div class="detail-row">
              <span class="label">Satellite:</span>
              <span class="value">{{ data.booking.satelliteId }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Provider:</span>
              <span class="value">{{ data.booking.provider }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Start Time:</span>
              <span class="value">{{ formatDateTime(data.booking.startTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Duration:</span>
              <span class="value">{{ data.booking.durationMinutes }} minutes</span>
            </div>
            <div class="detail-row">
              <span class="label">Pass Type:</span>
              <span class="value">{{ data.booking.passType | titlecase }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Purpose:</span>
              <span class="value">{{ data.booking.purpose | titlecase }}</span>
            </div>
            <div class="detail-row">
              <span class="label">Requested By:</span>
              <span class="value">{{ data.booking.requestedBy }}</span>
            </div>
            <div class="detail-row" *ngIf="data.booking.notes">
              <span class="label">Notes:</span>
              <span class="value">{{ data.booking.notes }}</span>
            </div>
          </div>
        </ya-field>

        <ya-field [label]="data.action === 'approve' ? 'Comments (optional)' : 'Rejection Reason (required)'">
          <textarea
            formControlName="comments"
            rows="3"
            [placeholder]="data.action === 'approve' ? 'Add any comments about this approval...' : 'Please provide a reason for rejection...'"
          ></textarea>
        </ya-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <ya-button mat-dialog-close>CANCEL</ya-button>
      <ya-button
        [appearance]="data.action === 'approve' ? 'primary' : 'danger'"
        (click)="onConfirm()"
        [disabled]="approvalForm.invalid">
        {{ data.action === 'approve' ? 'APPROVE' : 'REJECT' }}
      </ya-button>
    </mat-dialog-actions>
  `,
  styles: [`
    .booking-details {
      background: var(--y-background-color-secondary);
      border: 1px solid var(--y-border-color);
      border-radius: 4px;
      padding: 16px;
      margin-bottom: 16px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
      padding: 6px 0;
      border-bottom: 1px solid var(--y-border-color-light);
    }

    .detail-row:last-child {
      border-bottom: none;
      margin-bottom: 0;
    }

    .label {
      font-weight: 500;
      color: var(--y-text-color-secondary);
      min-width: 100px;
      flex-shrink: 0;
    }

    .value {
      color: var(--y-text-color-primary);
      text-align: right;
      word-break: break-word;
    }

    mat-dialog-content {
      padding: 24px !important;
    }

    mat-dialog-actions {
      padding: 16px 24px !important;
      gap: 8px;
    }
  `]
})
export class ApprovalDialogComponent {
  approvalForm: UntypedFormGroup;

  constructor(
    private dialogRef: MatDialogRef<ApprovalDialogComponent>,
    private fb: UntypedFormBuilder,
    @Inject(MAT_DIALOG_DATA) public data: DialogData
  ) {
    this.approvalForm = this.fb.group({
      comments: ['', data.action === 'reject' ? Validators.required : []]
    });
  }

  formatDateTime(dateTime: string): string {
    return new Date(dateTime).toLocaleString();
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.approvalForm.valid) {
      this.dialogRef.close({
        comments: this.approvalForm.get('comments')?.value
      });
    }
  }
}