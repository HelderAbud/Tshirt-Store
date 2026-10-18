"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ApiError } from "@/api/client";
import { adjustStock, listStock, StockItem } from "@/api/inventory";

export default function InventoryPage() {
  const [items, setItems] = useState<StockItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [variantId, setVariantId] = useState("");
  const [delta, setDelta] = useState("1");
  const [reason, setReason] = useState("acerto de estoque");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      setItems(await listStock());
    } catch (err) {
      setError(err instanceof ApiError ? err.detail ?? err.message : "Falha ao carregar estoque");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function onAdjust(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await adjustStock(Number(variantId), Number(delta), reason);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.detail ?? err.message : "Falha no ajuste");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminShell>
      <section className="rounded-xl border border-[var(--border)] bg-[var(--panel)] p-6">
        <h2 className="text-lg font-semibold">Estoque</h2>
        <p className="text-sm text-[var(--muted)]">Ajuste por variante com motivo</p>

        <form onSubmit={onAdjust} className="mt-4 grid gap-3 sm:grid-cols-4">
          <select
            className="rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
            value={variantId}
            onChange={(e) => setVariantId(e.target.value)}
            required
          >
            <option value="">Variante</option>
            {items.map((item) => (
              <option key={item.variantId} value={item.variantId}>
                {item.sku} ({item.color}/{item.size}) — {item.stockQty}
              </option>
            ))}
          </select>
          <input
            className="rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
            type="number"
            value={delta}
            onChange={(e) => setDelta(e.target.value)}
            required
          />
          <input
            className="rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            required
          />
          <button
            type="submit"
            disabled={saving}
            className="rounded-md bg-[var(--accent)] px-4 py-2 text-white disabled:opacity-60"
          >
            {saving ? "Salvando…" : "Ajustar"}
          </button>
        </form>

        {error && <p className="mt-3 text-sm text-red-400">{error}</p>}

        <div className="mt-6 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-[var(--muted)]">
              <tr>
                <th className="py-2">SKU</th>
                <th>Cor</th>
                <th>Tam.</th>
                <th>Qtd</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-4 text-[var(--muted)]">
                    Sem variantes. Crie produto/variante via API/Swagger.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.variantId} className="border-t border-[var(--border)]">
                    <td className="py-2">{item.sku}</td>
                    <td>{item.color}</td>
                    <td>{item.size}</td>
                    <td>{item.stockQty}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </AdminShell>
  );
}
