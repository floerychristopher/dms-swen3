import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DocumentService } from './document.service';
import { DocumentResponse } from '../models/document.model';

describe('DocumentService', () => {
  let service: DocumentService;
  let http: HttpTestingController;

  const doc: DocumentResponse = { id: 1, title: 'Invoice', createdAt: '2026-10-06T12:00:00Z' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DocumentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getAll() sends GET /api/documents', () => {
    let result: DocumentResponse[] | undefined;
    service.getAll().subscribe(r => (result = r));

    const req = http.expectOne('/api/documents');
    expect(req.request.method).toBe('GET');
    req.flush([doc]);

    expect(result).toEqual([doc]);
  });

  it('getById() sends GET /api/documents/:id', () => {
    service.getById(1).subscribe();
    const req = http.expectOne('/api/documents/1');
    expect(req.request.method).toBe('GET');
    req.flush(doc);
  });

  it('create() sends POST with the request body', () => {
    const body = { title: 'Invoice', description: 'Q3' };
    service.create(body).subscribe();

    const req = http.expectOne('/api/documents');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(doc);
  });

  it('update() sends PUT /api/documents/:id', () => {
    service.update(1, { title: 'New' }).subscribe();
    const req = http.expectOne('/api/documents/1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ title: 'New' });
    req.flush(doc);
  });

  it('delete() sends DELETE /api/documents/:id', () => {
    service.delete(1).subscribe();
    const req = http.expectOne('/api/documents/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});

