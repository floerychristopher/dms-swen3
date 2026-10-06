import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CollectionService } from '../../services/collection.service';
import { CollectionRequest } from '../../models/collection.model';
import { notBlank } from '../../validators/not-blank.validator';

@Component({
  selector: 'app-collection-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule
  ],
  templateUrl: './collection-form.html',
  styleUrl: './collection-form.scss'
})
export class CollectionFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private collectionService = inject(CollectionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  collectionForm!: FormGroup;
  isEditMode = false;
  collectionId?: number;
  error: string | null = null;

  ngOnInit(): void {
    this.collectionForm = this.fb.group({
      name: ['', [notBlank, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(1000)]]
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.collectionId = +idParam;
      this.collectionService.getById(this.collectionId).subscribe(col => {
        this.collectionForm.patchValue({
          name: col.name,
          description: col.description
        });
      });
    }
  }

  onSubmit(): void {
    if (this.collectionForm.invalid) {
      this.collectionForm.markAllAsTouched();
      return;
    }

    const request: CollectionRequest = this.collectionForm.value;
    
    const req$ = this.isEditMode && this.collectionId
      ? this.collectionService.update(this.collectionId, request)
      : this.collectionService.create(request);

    this.error = null;
    req$.subscribe({
      next: (col) => this.router.navigate(['/collections', col.id]),
      error: (err) => {
        // Handle 409 Conflict from backend (duplicate name)
        if (err.status === 409 && err.error?.errors?.name) {
          this.collectionForm.get('name')?.setErrors({ backend: err.error.errors.name });
        } else {
          console.error('Failed to save collection', err);
          this.error = 'Failed to save collection.';
        }
        this.cdr.detectChanges();
      }
    });
  }
}
