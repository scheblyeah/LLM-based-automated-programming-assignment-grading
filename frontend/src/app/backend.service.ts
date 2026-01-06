import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GradingResponse, GeneratedTestResults } from './grading-response-dialog/grading-response-dialog.component';

@Injectable({
  providedIn: 'root'
})
export class BackendService {
  private baseUrl = 'http://localhost:8080'; // Your backend base URL

  constructor(private http: HttpClient) { }

  generateGradingScheme(submissionData: any): Observable<string> {
    return this.http.post(`${this.baseUrl}/generateGradingScheme`, submissionData, { responseType: 'text' });
  }

  processMultiple(submissionData: any): Observable<string> {
    return this.http.post(`${this.baseUrl}/processMultiple`, submissionData, { responseType: 'text' });
  }

  getGradingResults(): Observable<GradingResponse[]> {
    return this.http.get<GradingResponse[]>(`${this.baseUrl}/getGradingResults`);
  }

  getTestResults(): Observable<GeneratedTestResults[]> {
    return this.http.get<GeneratedTestResults[]>(`${this.baseUrl}/getTestResults`);
  }

  downloadCSV(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/download-csv`, { responseType: 'blob' });
  }

  getGradingScheme(): Observable<string>{
    return this.http.get(`${this.baseUrl}/getGradingScheme`, { responseType: 'text' });
  }

  getGeneratedTests(): Observable<string>{
    return this.http.get(`${this.baseUrl}/getGeneratedUnitTests`, { responseType: 'text' });
  }

}
