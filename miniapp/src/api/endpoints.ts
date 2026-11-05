import { api } from "./client";
import type {
  Article, ArticleSummary, CreateOrderResponse, Feedback, FeedbackSummary, Order, Page, Product, TokenResponse, User,
} from "./types";

const qs = (p: Record<string, string | number | boolean | undefined | null>) => {
  const s = new URLSearchParams();
  Object.entries(p).forEach(([k, v]) => v !== undefined && v !== null && v !== "" && s.set(k, String(v)));
  const str = s.toString();
  return str ? `?${str}` : "";
};

export const authApi = {
  loginZalo: (accessToken: string) =>
    api<TokenResponse>("/auth/zalo", { method: "POST", body: { accessToken }, auth: false }),
  logout: (refreshToken: string) => api<void>("/auth/logout", { method: "POST", body: { refreshToken }, auth: false }),
};

export const meApi = {
  get: () => api<User>("/me"),
  update: (name: string) => api<User>("/me", { method: "PATCH", body: { name } }),
  linkPhone: (token: string, zaloAccessToken: string) =>
    api<User>("/me/phone", { method: "POST", body: { token, zaloAccessToken } }),
  location: (token: string, zaloAccessToken: string) =>
    api<User>("/me/location", { method: "POST", body: { token, zaloAccessToken } }),
};

export const contentApi = {
  articles: (p: { q?: string; category?: string; page?: number; size?: number }) =>
    api<Page<ArticleSummary>>(`/articles${qs(p)}`),
  article: (id: number) => api<Article>(`/articles/${id}`),
  categories: () => api<string[]>("/articles/categories"),
  products: (p: { q?: string; featured?: boolean; page?: number; size?: number }) =>
    api<Page<Product>>(`/products${qs(p)}`),
  product: (id: number) => api<Product>(`/products/${id}`),
};

export interface CreateFeedbackInput {
  category: string;
  title: string;
  content: string;
  address?: string;
  lat?: number;
  lng?: number;
}

export const feedbackApi = {
  categories: () => api<string[]>("/feedbacks/categories"),
  create: (data: CreateFeedbackInput, images: File[]) => {
    const fd = new FormData();
    fd.append("data", new Blob([JSON.stringify(data)], { type: "application/json" }));
    images.forEach((f) => fd.append("images", f));
    return api<Feedback>("/feedbacks", { method: "POST", body: fd });
  },
  mine: (page = 0, size = 10) => api<Page<FeedbackSummary>>(`/feedbacks/me${qs({ page, size })}`),
  get: (id: number) => api<Feedback>(`/feedbacks/${id}`),
};

export const orderApi = {
  create: (items: { productId: number; quantity: number }[]) =>
    api<CreateOrderResponse>("/orders", { method: "POST", body: { items } }),
  mine: (page = 0, size = 10) => api<Page<Order>>(`/orders/me${qs({ page, size })}`),
  get: (id: number) => api<Order>(`/orders/${id}`),
  mockPay: (id: number, success = true) => api<Order>(`/orders/${id}/mock-pay?success=${success}`, { method: "POST" }),
};
