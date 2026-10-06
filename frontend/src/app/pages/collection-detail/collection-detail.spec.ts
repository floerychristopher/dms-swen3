import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { delay, of } from 'rxjs';
import { CollectionDetail } from './collection-detail';
import { CollectionService } from '../../services/collection.service';
import { DocumentService } from '../../services/document.service';
import { CollectionResponse } from '../../models/collection.model';
import { DocumentResponse } from '../../models/document.model';

describe('CollectionDetail', () => {
  const collection: CollectionResponse = {
    id: 2, name: 'Taxes', createdAt: '2026-10-06T12:00:00Z', documentCount: 1
  };
  const inCollection: DocumentResponse = {
    id: 7, title: 'Invoice 2026', createdAt: '2026-10-06T12:00:00Z', collectionId: 2, collectionName: 'Taxes'
  };
  const unassigned: DocumentResponse = { id: 8, title: 'Contract', createdAt: '2026-10-06T12:00:00Z' };

  let collectionService: Record<string, ReturnType<typeof vi.fn>>;
  let documentService: Record<string, ReturnType<typeof vi.fn>>;

  async function navigateToCollection(id: number) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'collections/:id', component: CollectionDetail }]),
        { provide: CollectionService, useValue: collectionService },
        { provide: DocumentService, useValue: documentService }
      ]
    });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(`/collections/${id}`, CollectionDetail);
    await harness.fixture.whenStable();
    return { harness, component };
  }

  beforeEach(() => {
    collectionService = {
      getById: vi.fn(() => of(collection).pipe(delay(0))), // async like a real HTTP response
      getDocuments: vi.fn(() => of([inCollection]).pipe(delay(0))),
      assignDocument: vi.fn(() => of(unassigned)),
      removeDocument: vi.fn(() => of(inCollection)),
      delete: vi.fn(() => of(undefined))
    };
    documentService = { getAll: vi.fn(() => of([inCollection, unassigned])) };
  });

  afterEach(() => vi.restoreAllMocks());

  // Regression test: the list used to show "No documents in this collection." although the count was 1.
  it('renders the documents that belong to the collection', async () => {
    const { harness } = await navigateToCollection(2);
    const el = harness.routeNativeElement!;

    expect(collectionService['getDocuments']).toHaveBeenCalledWith(2);
    expect(el.textContent).toContain('Taxes');
    expect(el.textContent).toContain('1 document(s)');
    expect(el.querySelector('a[href="/documents/7"]')?.textContent).toContain('Invoice 2026');
    expect(el.textContent).not.toContain('No documents in this collection.');
  });

  it('shows an empty state if the collection has no documents', async () => {
    collectionService['getDocuments'].mockReturnValue(of([]));
    const { harness } = await navigateToCollection(2);

    expect(harness.routeNativeElement!.textContent).toContain('No documents in this collection.');
  });

  it('offers only documents that are not yet in this collection for assignment', async () => {
    const { component } = await navigateToCollection(2);

    expect(component.availableDocuments.map(d => d.id)).toEqual([8]);
  });

  it('assigns a document and reloads the collection', async () => {
    const { component } = await navigateToCollection(2);
    collectionService['getById'].mockClear();

    component.selectedDocumentIdToAssign = 8;
    component.assignDocument();

    expect(collectionService['assignDocument']).toHaveBeenCalledWith(2, 8);
    expect(collectionService['getById']).toHaveBeenCalledWith(2); // refreshes the document count
    expect(component.selectedDocumentIdToAssign).toBeNull();
  });

  it('removes a document after confirmation', async () => {
    const { component } = await navigateToCollection(2);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.removeDocument(inCollection);

    expect(collectionService['removeDocument']).toHaveBeenCalledWith(2, 7);
  });
});
