import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { DocumentDetail } from './pages/document-detail/document-detail';
import { CollectionDetail } from './pages/collection-detail/collection-detail';
import { DocumentFormComponent } from './pages/document-form/document-form';
import { CollectionFormComponent } from './pages/collection-form/collection-form';

export const routes: Routes = [
  { path: '', component: Dashboard },
  { path: 'documents/new', component: DocumentFormComponent },
  { path: 'documents/:id/edit', component: DocumentFormComponent },
  { path: 'documents/:id', component: DocumentDetail },
  { path: 'collections/new', component: CollectionFormComponent },
  { path: 'collections/:id/edit', component: CollectionFormComponent },
  { path: 'collections/:id', component: CollectionDetail },
];
