import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Document } from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/documents';

  getAll(): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl);
  }
}
