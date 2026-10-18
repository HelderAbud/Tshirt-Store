import { apiFetch } from "./client";

export type SaleItemInput = {
  variantId: number;
  qty: number;
  unitPrice: number;
};

export type SaleResponse = {
  id: number;
  totalAmount: number;
  createdAt: string;
  items: {
    variantId: number;
    qty: number;
    unitPrice: number;
    lineTotal: number;
  }[];
};

export type DailyPoint = {
  date: string;
  revenue: number;
  count: number;
};

export async function createSale(items: SaleItemInput[]): Promise<SaleResponse> {
  return apiFetch("/api/admin/sales", {
    method: "POST",
    body: JSON.stringify({ items }),
  });
}

export async function salesDaily(from: string, to: string): Promise<DailyPoint[]> {
  return apiFetch(`/api/admin/sales/daily?from=${from}&to=${to}`);
}
