export interface CollectionResponse {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  documentCount: number;
}

export interface CollectionRequest {
  name: string;
  description?: string;
}

