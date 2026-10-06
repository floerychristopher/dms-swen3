import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { CollectionFormComponent } from './collection-form';
import { CollectionService } from '../../services/collection.service';

describe('CollectionFormComponent', () => {
  let collectionService: Record<string, ReturnType<typeof vi.fn>>;
  let navigate: ReturnType<typeof vi.spyOn>;

  async function open(url = '/collections/new') {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'collections/new', component: CollectionFormComponent },
          { path: 'collections/:id/edit', component: CollectionFormComponent }
        ]),
        { provide: CollectionService, useValue: collectionService }
      ]
    });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(url, CollectionFormComponent);
    await harness.fixture.whenStable();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    return { harness, component };
  }

  beforeEach(() => {
    collectionService = {
      getById: vi.fn(() => of({ id: 3, name: 'Taxes', description: 'All tax docs', createdAt: '', documentCount: 0 })),
      create: vi.fn(() => of({ id: 3, name: 'Taxes', createdAt: '', documentCount: 0 })),
      update: vi.fn(() => of({ id: 3, name: 'Taxes 2026', createdAt: '', documentCount: 0 }))
    };
  });

  afterEach(() => vi.restoreAllMocks());

  it('requires a name with at most 100 characters', async () => {
    const { component } = await open();
    const name = component.collectionForm.get('name')!;

    expect(name.hasError('required')).toBe(true);
    name.setValue('x'.repeat(101));
    expect(name.hasError('maxlength')).toBe(true);
    name.setValue('Taxes');
    expect(name.valid).toBe(true);
  });

  it('does not send an invalid form', async () => {
    const { component } = await open();

    component.onSubmit();

    expect(collectionService['create']).not.toHaveBeenCalled();
  });

  it('rejects a whitespace-only name', async () => {
    const { component } = await open();

    component.collectionForm.setValue({ name: '   ', description: '' });
    component.onSubmit();

    expect(component.collectionForm.get('name')!.hasError('required')).toBe(true);
    expect(collectionService['create']).not.toHaveBeenCalled();
  });

  it('submits the typed name when the user presses Enter without leaving the field', async () => {
    const { harness } = await open();
    const form = harness.routeNativeElement!.querySelector('form')!;
    const nameInput = form.querySelector<HTMLInputElement>('input[formControlName="name"]')!;

    nameInput.value = 'Taxes';
    nameInput.dispatchEvent(new Event('input'));
    form.dispatchEvent(new Event('submit'));

    expect(collectionService['create']).toHaveBeenCalledWith(expect.objectContaining({ name: 'Taxes' }));
  });

  it('shows a general error message for other backend errors', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    collectionService['create'].mockReturnValue(throwError(() => ({ status: 500 })));
    const { harness, component } = await open();

    component.collectionForm.setValue({ name: 'Taxes', description: '' });
    component.onSubmit();
    harness.detectChanges();

    expect(navigate).not.toHaveBeenCalled();
    expect(harness.routeNativeElement!.textContent).toContain('Failed to save collection.');
  });

  it('creates a collection and navigates to its detail page', async () => {
    const { component } = await open();

    component.collectionForm.setValue({ name: 'Taxes', description: '' });
    component.onSubmit();

    expect(collectionService['create']).toHaveBeenCalledWith({ name: 'Taxes', description: '' });
    expect(navigate).toHaveBeenCalledWith(['/collections', 3]);
  });

  it('shows the backend error at the name field if the name already exists (HTTP 409)', async () => {
    collectionService['create'].mockReturnValue(throwError(() => ({
      status: 409,
      error: { errors: { name: "A collection named 'Taxes' already exists" } }
    })));
    const { harness, component } = await open();
    component.collectionForm.setValue({ name: 'Taxes', description: '' });

    harness.routeNativeElement!.querySelector('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
    await harness.fixture.whenStable();

    expect(component.collectionForm.get('name')!.hasError('backend')).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
    expect(harness.routeNativeElement!.textContent).toContain("A collection named 'Taxes' already exists");
  });

  it('pre-fills the form in edit mode and updates the collection', async () => {
    const { component } = await open('/collections/3/edit');

    expect(component.collectionForm.value).toEqual({ name: 'Taxes', description: 'All tax docs' });

    component.collectionForm.get('name')!.setValue('Taxes 2026');
    component.onSubmit();

    expect(collectionService['update']).toHaveBeenCalledWith(3, { name: 'Taxes 2026', description: 'All tax docs' });
  });
});
