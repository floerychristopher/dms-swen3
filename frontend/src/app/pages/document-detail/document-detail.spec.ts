import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { delay, of, throwError } from 'rxjs';
import { DocumentDetail } from './document-detail';
import { DocumentService } from '../../services/document.service';
import { DocumentResponse } from '../../models/document.model';

describe('DocumentDetail', () => {
  const doc: DocumentResponse = {
    id: 7,
    title: 'Invoice 2026',
    description: 'Electricity bill',
    createdAt: '2026-10-06T12:00:00Z',
    collectionId: 2,
    collectionName: 'Taxes'
  };

  let documentService: { getById: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };

  async function navigateToDocument(id: number) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'documents/:id', component: DocumentDetail }]),
        { provide: DocumentService, useValue: documentService }
      ]
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(`/documents/${id}`, DocumentDetail);
    await harness.fixture.whenStable();
    return harness;
  }

  beforeEach(() => {
    documentService = {
      getById: vi.fn(() => of(doc).pipe(delay(0))), // async like a real HTTP response
      delete: vi.fn(() => of(undefined))
    };
  });

  afterEach(() => vi.restoreAllMocks());

  // Regression test: the detail page used to stay blank after the data arrived.
  it('renders the document loaded for the id in the URL', async () => {
    const harness = await navigateToDocument(7);
    const el = harness.routeNativeElement!;

    expect(documentService.getById).toHaveBeenCalledWith(7);
    expect(el.textContent).toContain('Invoice 2026');
    expect(el.textContent).toContain('Electricity bill');
    expect(el.querySelector('a[href="/collections/2"]')?.textContent).toContain('Taxes');
    expect(el.querySelector('a[href="/documents/7/edit"]')).not.toBeNull();
  });

  it('shows an error message if the document cannot be loaded', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    documentService.getById.mockReturnValue(throwError(() => ({ status: 404 })));

    const harness = await navigateToDocument(99);

    expect(harness.routeNativeElement!.textContent).toContain('Failed to load document.');
  });

  it('deletes the document after confirmation and navigates to the dashboard', async () => {
    const harness = await navigateToDocument(7);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    const deleteButton = Array.from(harness.routeNativeElement!.querySelectorAll('button'))
      .find(b => b.textContent?.includes('Delete'))!;
    deleteButton.click();

    expect(documentService.delete).toHaveBeenCalledWith(7);
    expect(navigate).toHaveBeenCalledWith(['/']);
  });

  it('does not delete when the confirmation is cancelled', async () => {
    const harness = await navigateToDocument(7);
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    harness.routeDebugElement!.componentInstance.deleteDocument();

    expect(documentService.delete).not.toHaveBeenCalled();
  });
});
