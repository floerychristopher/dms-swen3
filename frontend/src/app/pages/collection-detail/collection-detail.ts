import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule } from '@angular/forms';
import { CollectionService } from '../../services/collection.service';
import { DocumentService } from '../../services/document.service';
import { CollectionResponse } from '../../models/collection.model';
import { DocumentResponse } from '../../models/document.model';

@Component({
  selector: 'app-collection-detail',
  imports: [
    CommonModule, 
    RouterModule, 
    MatCardModule, 
    MatButtonModule, 
    MatIconModule,
    MatListModule,
    MatSelectModule,
    MatFormFieldModule,
    FormsModule
  ],
  styleUrl: './collection-detail.scss',
  templateUrl: './collection-detail.html',
})
export class CollectionDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly collectionService = inject(CollectionService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly documentService = inject(DocumentService);

  collection: CollectionResponse | null = null;
  documents: DocumentResponse[] = [];
  allDocuments: DocumentResponse[] = [];
  availableDocuments: DocumentResponse[] = [];
  
  selectedDocumentIdToAssign: number | null = null;
  error: string | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadData(+id);
    }
  }

  loadData(id: number): void {
    this.collectionService.getById(id).subscribe({
      next: (col) => {
        this.collection = col; 
        this.cdr.detectChanges();
        this.loadDocuments(id);
      },
      error: (err) => this.handleError('Failed to load collection.', err)
    });
  }

  loadDocuments(id: number): void {
    this.collectionService.getDocuments(id).subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loadAllDocuments();
        this.cdr.detectChanges();
      },
      error: (err) => this.handleError('Failed to load collection documents.', err)
    });
  }

  loadAllDocuments(): void {
    this.documentService.getAll().subscribe({
      next: (docs) => {
        this.allDocuments = docs;
        // Filter out documents already in this collection
        this.availableDocuments = this.allDocuments.filter(d => d.collectionId !== this.collection?.id);
        this.cdr.detectChanges();
      },
      error: (err) => this.handleError('Failed to load available documents.', err)
    });
  }

  assignDocument(): void {
    if (this.collection && this.selectedDocumentIdToAssign) {
      this.collectionService.assignDocument(this.collection.id, this.selectedDocumentIdToAssign).subscribe({
        next: () => {
          this.selectedDocumentIdToAssign = null;
          this.loadData(this.collection!.id); // Reload data instead of just documents to update count
        },
        error: (err) => this.handleError('Failed to assign document.', err)
      });
    }
  }

  removeDocument(doc: DocumentResponse): void {
    if (this.collection && confirm(`Remove "${doc.title}" from this collection?`)) {
      this.collectionService.removeDocument(this.collection.id, doc.id).subscribe({
        next: () => {
          this.loadData(this.collection!.id); // Reload data instead of just documents to update count
        },
        error: (err) => this.handleError('Failed to remove document.', err)
      });
    }
  }

  deleteCollection(): void {
    if (this.collection && confirm('Are you sure you want to delete this collection?')) {
      this.collectionService.delete(this.collection.id).subscribe({
        next: () => {
          this.router.navigate(['/']);
        },
        error: (err) => this.handleError('Failed to delete collection.', err)
      });
    }
  }

  private handleError(msg: string, err: any): void {
    console.error(msg, err);
    this.error = msg;
    this.cdr.detectChanges();
  }
}
