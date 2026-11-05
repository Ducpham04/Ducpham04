export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type Role = "USER" | "EDITOR" | "ADMIN";

export interface User {
  id: number;
  zaloId?: string;
  email?: string;
  name: string;
  avatarUrl?: string;
  phone?: string;
  role: Role;
  lastLat?: number;
  lastLng?: number;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface ArticleSummary {
  id: number;
  title: string;
  category: string;
  summary?: string;
  thumbnailUrl?: string;
  publishedAt?: string;
}

export interface Article extends ArticleSummary {
  content: string;
}

export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  stock: number;
  featured: boolean;
}

export type FeedbackStatus = "NEW" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";

export interface FeedbackSummary {
  id: number;
  code: string;
  category: string;
  title: string;
  status: FeedbackStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Feedback extends FeedbackSummary {
  content: string;
  address?: string;
  lat?: number;
  lng?: number;
  adminReply?: string;
  images: string[];
}

export type OrderStatus = "PENDING" | "PAID" | "FAILED" | "CANCELLED";

export interface Order {
  id: number;
  code: string;
  amount: number;
  status: OrderStatus;
  description: string;
  paymentMethod?: string;
  paidAt?: string;
  createdAt: string;
  items?: { productId: number; productName: string; unitPrice: number; quantity: number }[];
}

export interface CreateOrderResponse {
  order: Order;
  provider: "mock" | "zalo";
  checkout: {
    amount: number;
    desc: string;
    item: Record<string, unknown>[];
    extradata: string;
    method: string;
    mac: string;
  };
}
