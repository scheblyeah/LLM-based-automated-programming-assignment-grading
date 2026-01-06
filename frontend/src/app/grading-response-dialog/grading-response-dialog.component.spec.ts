import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GradingResponseDialogComponent } from './grading-response-dialog.component';

describe('GradingResponseDialogComponent', () => {
  let component: GradingResponseDialogComponent;
  let fixture: ComponentFixture<GradingResponseDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GradingResponseDialogComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(GradingResponseDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
