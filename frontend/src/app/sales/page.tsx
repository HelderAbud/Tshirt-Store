"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ApiError } from "@/api/client";
import { listStock, StockItem } from "@/api/inventory";
import { createSale } from "@/api/sales";

export default function SalesPage() {
  const [items, setItems] = useState<StockItem[]>([]);
  const [variantId, setVariantId] = useState("");
  const [qty, setQty] = useState("1");
  const [unitPrice, setUnitPrice] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const stock = await listStock();
      setItems(stock);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail ?? err.message : "Falha ao carregar");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    const selected = items.find((i) => String(i.variantId) === variantId);
    if (selected && !unitPrice) {
      // price not on stock DTO — leave blank for admin input
    }
  }, [variantId, items, unitPrice]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const sale = await createSale([
        {
          variantId: Number(variantId),
          qty: Number(qty),
          unitPrice: Number(unitPrice),
        },
      ]);
      setMessage(`Venda #${sale.id} — total R$ ${Number(sale.totalAmount).toFixed(2)}`);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.detail ?? err.message : "Falha na venda");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminShell>
      <section className="rounded-xl border border-[var(--border)] bg-[var(--panel)] p-6">
        <h2 className="text-lg font-semibold">Venda de balcão</h2>
        <p className="text-sm text-[var(--muted)]">Registra venda e baixa estoque</p>

        <form onSubmit={onSubmit} className="mt-4 grid gap-3 sm:grid-cols-4">
          <select
            className="rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
            value={variantId}
            onChange={(e) => {
              setVariantId(e.target.value);
              const selected = items.find((i) => String(i.variantId) === e.target.value);
              if (selected) {
                setUnitPrice(String(selected.price));
              }
            }}
            required
          >
            <option value="">Variante</option>
            {items.map((item) => (
              <option key={item.variantId} value={item.variantId}>
                {item.sku} — estoque {item.stockQty}
              </option>
            ))}
          </select>
          <input
            className="rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
            type="number"
            min={1}
            value={qty}
            onChange={(e) => setQty(e.target.value)}
            required
            placeholder="Qtd"
          />
          <input
            className="rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
            type="number"
            min={0}
            step="0.01"
            value={unitPrice}
            onChange={(e) => setUnitPrice(e.target.value)}
            required
            placeholder="Preço unit."
          />
          <button
            type="submit"
            disabled={saving}
            className="rounded-md bg-[var(--accent)] px-4 py-2 text-white disabled:opacity-60"
          >
            {saving ? "Registrando…" : "Registrar venda"}
          </button>
        </form>

        {message && <p className="mt-3 text-sm text-emerald-400">{message}</p>}
        {error && <p className="mt-3 text-sm text-red-400">{error}</p>}
      </section>
    </AdminShell>
  );
}
