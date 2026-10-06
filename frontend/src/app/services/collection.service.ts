import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CollectionResponse, CollectionRequest } from '../models/collection.model';
import { DocumentResponse } from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class CollectionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/collections';

  getAll(): Observable<CollectionResponse[]> {
    return this.http.get<CollectionResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<CollectionResponse> {
    return this.http.get<CollectionResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CollectionRequest): Observable<CollectionResponse> {
    return this.http.post<CollectionResponse>(this.apiUrl, request);
  }

  update(id: number, request: CollectionRequest): Observable<CollectionResponse> {
    return this.http.put<CollectionResponse>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getDocuments(collectionId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.apiUrl}/${collectionId}/documents`);
  }

  assignDocument(collectionId: number, documentId: number): Observable<DocumentResponse> {
    return this.http.put<DocumentResponse>(`${this.apiUrl}/${collectionId}/documents/${documentId}`, {});
  }

  removeDocument(collectionId: number, documentId: number): Observable<DocumentResponse> {
    return this.http.delete<DocumentResponse>(`${this.apiUrl}/${collectionId}/documents/${documentId}`);
  }
}

