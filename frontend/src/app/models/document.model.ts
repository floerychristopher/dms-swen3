export interface DocumentResponse {
  id: number;
  title: string;
  description?: string;
  createdAt: string;
  updatedAt?: string;
  collectionId?: number;
  collectionName?: string;
}

export interface DocumentRequest {
  title: string;
  description?: string;
  collectionId?: number;
}
