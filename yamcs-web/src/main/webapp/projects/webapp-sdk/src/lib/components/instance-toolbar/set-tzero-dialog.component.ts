import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MissionTimerService } from '../../services/mission-timer.service';
import * as utils from '../../utils';
import { YaButton } from '../button/button.component';
import { YaDateTimeInput } from '../date-time-input/date-time-input.component';
import { YaField } from '../field/field.component';

@Component({
  selector: 'ya-set-tzero-dialog',
  templateUrl: './set-tzero-dialog.component.html',
  imports: [
    MatDialogActions,
    MatDialogClose,
    MatDialogContent,
    MatDialogTitle,
    ReactiveFormsModule,
    YaDateTimeInput,
    YaButton,
    YaField,
  ],
})
export class SetTZeroDialogComponent {
  private dialogRef = inject(MatDialogRef<SetTZeroDialogComponent>);
  private missionTimerService = inject(MissionTimerService);

  form = new FormGroup({
    tZero: new FormControl<string | null>(null),
  });

  constructor() {
    const currentTZero = this.missionTimerService.getTZero();
    if (currentTZero) {
      this.form.setValue({
        tZero: utils.toISOString(currentTZero),
      });
    }
  }

  setTZero() {
    if (this.form.value.tZero) {
      const date = new Date(this.form.value.tZero);
      this.missionTimerService.setTZero(date);
      this.dialogRef.close(true);
    }
  }

  clearTZero() {
    this.missionTimerService.clearTZero();
    this.dialogRef.close(true);
  }
}
