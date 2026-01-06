import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';

import { AppComponent } from './app.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { GradingResponseDialogComponent } from './grading-response-dialog/grading-response-dialog.component';
import { CommonModule } from '@angular/common';
import {MatButton} from "@angular/material/button";
import { MatButtonModule } from '@angular/material/button';
import {MatToolbar} from "@angular/material/toolbar";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import {MatTooltipModule} from "@angular/material/tooltip";
import {MatIconModule} from "@angular/material/icon";


@NgModule({
  declarations: [
    AppComponent,
    GradingResponseDialogComponent
  ],
    imports: [
        BrowserModule,
        CommonModule,
        FormsModule,
        MatDialogModule,
        MatSnackBarModule,
        HttpClientModule,
        MatTabsModule,
        MatButton,
        MatButtonModule,
        MatToolbar,
        MatTooltipModule,
        MatIconModule,
        MatProgressSpinner
    ],
  exports: [
    GradingResponseDialogComponent
  ],
  providers: [
    provideAnimationsAsync()
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
export class GradingResponseDialogModule {}
