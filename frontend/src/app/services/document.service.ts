import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocumentResponse, DocumentRequest } from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/documents';

  getAll(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<DocumentResponse> {
    return this.http.get<DocumentResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: DocumentRequest): Observable<DocumentResponse> {
    return this.http.post<DocumentResponse>(this.apiUrl, request);
  }

  upload(request: DocumentRequest, pdf: File): Observable<DocumentResponse> {
    const body = new FormData();
    body.append('file', pdf);
    body.append('title', request.title);
    if (request.description != null) {
      body.append('description', request.description);
    }
    if (request.collectionId != null) {
      body.append('collectionId', String(request.collectionId));
    }
    return this.http.post<DocumentResponse>(this.apiUrl, body);
  }

  update(id: number, request: DocumentRequest): Observable<DocumentResponse> {
    return this.http.put<DocumentResponse>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

