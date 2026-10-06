import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Dashboard } from './dashboard';
import { DocumentService } from '../../services/document.service';
import { CollectionService } from '../../services/collection.service';
import { DocumentResponse } from '../../models/document.model';
import { CollectionResponse } from '../../models/collection.model';

describe('Dashboard', () => {
  async function render(documents: DocumentResponse[], collections: CollectionResponse[]) {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: DocumentService, useValue: { getAll: vi.fn(() => of(documents)) } },
        { provide: CollectionService, useValue: { getAll: vi.fn(() => of(collections)) } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders documents and collections loaded from the REST API', async () => {
    const el = await render(
      [{ id: 1, title: 'Invoice', createdAt: '2026-10-06T12:00:00Z', collectionName: 'Taxes' }],
      [{ id: 2, name: 'Taxes', createdAt: '2026-10-06T12:00:00Z', documentCount: 1 }]
    );

    expect(el.textContent).toContain('Invoice');
    expect(el.textContent).toContain('Taxes');
    expect(el.textContent).toContain('1 document(s)');
    expect(el.querySelector('a[href="/documents/1"]')).not.toBeNull();
    expect(el.querySelector('a[href="/collections/2"]')).not.toBeNull();
    expect(el.textContent).not.toContain('No documents found.');
  });

  it('shows empty states when there is no data', async () => {
    const el = await render([], []);

    expect(el.textContent).toContain('No documents found.');
    expect(el.textContent).toContain('No collections found.');
  });
});
