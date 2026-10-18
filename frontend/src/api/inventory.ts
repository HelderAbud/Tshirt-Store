import { apiFetch } from "./client";

export type StockItem = {
  variantId: number;
  productId: number;
  sku: string;
  color: string;
  size: string;
  stockQty: number;
  price: number;
};

export async function listStock(): Promise<StockItem[]> {
  return apiFetch("/api/admin/inventory/stock");
}

export async function adjustStock(
  variantId: number,
  delta: number,
  reason: string,
): Promise<{ variantId: number; productId: number; sku: string; stockQty: number }> {
  return apiFetch("/api/admin/inventory/adjust", {
    method: "POST",
    body: JSON.stringify({ variantId, delta, reason }),
  });
}
