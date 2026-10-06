import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { DocumentService } from '../../services/document.service';
import { CollectionService } from '../../services/collection.service';
import { CollectionResponse } from '../../models/collection.model';
import { DocumentRequest } from '../../models/document.model';
import { notBlank } from '../../validators/not-blank.validator';

@Component({
  selector: 'app-document-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, 
    MatButtonModule, MatSelectModule, MatIconModule
  ],
  templateUrl: './document-form.html',
  styleUrl: './document-form.scss'
})
export class DocumentFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private documentService = inject(DocumentService);
  private collectionService = inject(CollectionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  documentForm!: FormGroup;
  collections: CollectionResponse[] = [];
  isEditMode = false;
  documentId?: number;
  error: string | null = null;

  ngOnInit(): void {
    // mat-error only shows errors once a field was touched (blurred) or the form was submitted
    this.documentForm = this.fb.group({
      title: ['', [notBlank, Validators.maxLength(255)]],
      description: ['', [Validators.maxLength(2000)]],
      collectionId: [null]
    });

    this.collectionService.getAll().subscribe(cols => {
      this.collections = cols;
      this.cdr.detectChanges();
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.documentId = +idParam;
      this.documentService.getById(this.documentId).subscribe(doc => {
        this.documentForm.patchValue({
          title: doc.title,
          description: doc.description,
          collectionId: doc.collectionId
        });
        this.cdr.detectChanges();
      });
    }
  }

  onSubmit(): void {
    if (this.documentForm.invalid) {
      this.documentForm.markAllAsTouched();
      return;
    }

    const request: DocumentRequest = this.documentForm.value;
    const req$ = this.isEditMode && this.documentId
      ? this.documentService.update(this.documentId, request)
      : this.documentService.create(request);

    this.error = null;
    req$.subscribe({
      next: (doc) => this.router.navigate(['/documents', doc.id]),
      error: (err) => {
        console.error('Failed to save document', err);
        this.error = 'Failed to save document.';
        this.cdr.detectChanges();
      }
    });
  }
}
