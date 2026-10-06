import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { DocumentService } from '../../services/document.service';
import { CollectionService } from '../../services/collection.service';
import { DocumentResponse } from '../../models/document.model';
import { CollectionResponse } from '../../models/collection.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatListModule, MatIconModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  documents: DocumentResponse[] = [];
  collections: CollectionResponse[] = [];

  private documentService = inject(DocumentService);
  private collectionService = inject(CollectionService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.documentService.getAll().subscribe(docs => {
      this.documents = docs;
      this.cdr.detectChanges();
    });
    this.collectionService.getAll().subscribe(cols => {
      this.collections = cols;
      this.cdr.detectChanges();
    });
  }
}
