import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DocumentService } from '../../services/document.service';
import { DocumentResponse } from '../../models/document.model';

@Component({
  selector: 'app-document-detail',
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule],
  styleUrl: './document-detail.scss',
  templateUrl: './document-detail.html',
})
export class DocumentDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly documentService = inject(DocumentService);
  private readonly cdr = inject(ChangeDetectorRef);

  document: DocumentResponse | null = null;
  error: string | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadDocument(+id);
    }
  }

  loadDocument(id: number): void {
    this.documentService.getById(id).subscribe({
      next: (doc) => { this.document = doc; this.cdr.detectChanges(); },
      error: (err) => {
        console.error('Failed to load document', err);
        this.error = 'Failed to load document.';
      }
    });
  }

  deleteDocument(): void {
    if (this.document && confirm('Are you sure you want to delete this document?')) {
      this.documentService.delete(this.document.id).subscribe({
        next: () => {
          this.router.navigate(['/']);
        },
        error: (err) => {
          console.error('Failed to delete document', err);
          this.error = 'Failed to delete document.';
          this.cdr.detectChanges();
        }
      });
    }
  }
}
