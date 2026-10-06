import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CollectionService } from './collection.service';

describe('CollectionService', () => {
  let service: CollectionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CollectionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getAll() sends GET /api/collections', () => {
    service.getAll().subscribe();
    expect(http.expectOne('/api/collections').request.method).toBe('GET');
  });

  it('create() sends POST with the request body', () => {
    service.create({ name: 'Taxes' }).subscribe();
    const req = http.expectOne('/api/collections');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Taxes' });
  });

  it('update() sends PUT /api/collections/:id', () => {
    service.update(3, { name: 'Taxes 2026' }).subscribe();
    expect(http.expectOne('/api/collections/3').request.method).toBe('PUT');
  });

  it('delete() sends DELETE /api/collections/:id', () => {
    service.delete(3).subscribe();
    expect(http.expectOne('/api/collections/3').request.method).toBe('DELETE');
  });

  it('getDocuments() sends GET /api/collections/:id/documents', () => {
    service.getDocuments(3).subscribe();
    expect(http.expectOne('/api/collections/3/documents').request.method).toBe('GET');
  });

  it('assignDocument() sends PUT /api/collections/:id/documents/:docId', () => {
    service.assignDocument(3, 7).subscribe();
    expect(http.expectOne('/api/collections/3/documents/7').request.method).toBe('PUT');
  });

  it('removeDocument() sends DELETE /api/collections/:id/documents/:docId', () => {
    service.removeDocument(3, 7).subscribe();
    expect(http.expectOne('/api/collections/3/documents/7').request.method).toBe('DELETE');
  });
});

