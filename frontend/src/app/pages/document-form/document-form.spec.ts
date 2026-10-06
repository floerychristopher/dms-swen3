import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { DocumentFormComponent } from './document-form';
import { DocumentService } from '../../services/document.service';
import { CollectionService } from '../../services/collection.service';
import { DocumentResponse } from '../../models/document.model';

describe('DocumentFormComponent', () => {
  const existing: DocumentResponse = {
    id: 7, title: 'Invoice', description: 'Q3', createdAt: '2026-10-06T12:00:00Z', collectionId: 2
  };

  let documentService: Record<string, ReturnType<typeof vi.fn>>;
  let navigate: ReturnType<typeof vi.spyOn>;

  async function open(url: string) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'documents/new', component: DocumentFormComponent },
          { path: 'documents/:id/edit', component: DocumentFormComponent }
        ]),
        { provide: DocumentService, useValue: documentService },
        {
          provide: CollectionService,
          useValue: { getAll: vi.fn(() => of([{ id: 2, name: 'Taxes', createdAt: '', documentCount: 0 }])) }
        }
      ]
    });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(url, DocumentFormComponent);
    await harness.fixture.whenStable();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    return { harness, component };
  }

  beforeEach(() => {
    documentService = {
      getById: vi.fn(() => of(existing)),
      create: vi.fn(() => of({ ...existing, id: 42 })),
      update: vi.fn(() => of(existing))
    };
  });

  afterEach(() => vi.restoreAllMocks());

  describe('validation', () => {
    it('requires a title', async () => {
      const { component } = await open('/documents/new');
      const title = component.documentForm.get('title')!;

      expect(title.hasError('required')).toBe(true);
      title.setValue('Invoice');
      expect(title.valid).toBe(true);
    });

    it('limits the title to 255 and the description to 2000 characters', async () => {
      const { component } = await open('/documents/new');

      component.documentForm.get('title')!.setValue('x'.repeat(256));
      component.documentForm.get('description')!.setValue('x'.repeat(2001));

      expect(component.documentForm.get('title')!.hasError('maxlength')).toBe(true);
      expect(component.documentForm.get('description')!.hasError('maxlength')).toBe(true);
    });

    it('does not send an invalid form and shows the error message', async () => {
      const { harness, component } = await open('/documents/new');

      harness.routeNativeElement!.querySelector('form')!.dispatchEvent(new Event('submit'));
      harness.detectChanges();
      await harness.fixture.whenStable();

      expect(documentService['create']).not.toHaveBeenCalled();
      expect(component.documentForm.get('title')!.touched).toBe(true);
      expect(harness.routeNativeElement!.textContent).toContain('Title is required.');
    });
  });

  it('submits the typed title when the user presses Enter without leaving the field', async () => {
    const { harness } = await open('/documents/new');
    const form = harness.routeNativeElement!.querySelector('form')!;
    const titleInput = form.querySelector<HTMLInputElement>('input[formControlName="title"]')!;

    titleInput.value = 'Invoice';
    titleInput.dispatchEvent(new Event('input'));
    form.dispatchEvent(new Event('submit')); // Enter submits the form, no blur happens

    expect(documentService['create']).toHaveBeenCalledWith(expect.objectContaining({ title: 'Invoice' }));
  });

  it('does not send a whitespace-only title', async () => {
    const { component } = await open('/documents/new');

    component.documentForm.setValue({ title: '   ', description: '', collectionId: null });
    component.onSubmit();

    expect(component.documentForm.get('title')!.invalid).toBe(true);
    expect(documentService['create']).not.toHaveBeenCalled();
  });

  it('shows an error message if the backend rejects the document', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    documentService['create'].mockReturnValue(throwError(() => ({ status: 500 })));
    const { harness, component } = await open('/documents/new');

    component.documentForm.setValue({ title: 'Invoice', description: '', collectionId: null });
    component.onSubmit();
    harness.detectChanges();

    expect(navigate).not.toHaveBeenCalled();
    expect(harness.routeNativeElement!.textContent).toContain('Failed to save document.');
  });

  it('creates a document and navigates to its detail page', async () => {
    const { component } = await open('/documents/new');

    component.documentForm.setValue({ title: 'Invoice', description: 'Q3', collectionId: 2 });
    component.onSubmit();

    expect(documentService['create']).toHaveBeenCalledWith({ title: 'Invoice', description: 'Q3', collectionId: 2 });
    expect(navigate).toHaveBeenCalledWith(['/documents', 42]);
  });

  it('pre-fills the form in edit mode and updates the document', async () => {
    const { component } = await open('/documents/7/edit');

    expect(component.isEditMode).toBe(true);
    expect(component.documentForm.value).toEqual({ title: 'Invoice', description: 'Q3', collectionId: 2 });

    component.documentForm.get('title')!.setValue('Invoice (paid)');
    component.onSubmit();

    expect(documentService['update']).toHaveBeenCalledWith(7, expect.objectContaining({ title: 'Invoice (paid)' }));
    expect(navigate).toHaveBeenCalledWith(['/documents', 7]);
  });
});
