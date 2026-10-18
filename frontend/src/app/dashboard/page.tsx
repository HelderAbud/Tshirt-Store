"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AdminShell } from "@/components/AdminShell";
import { ApiError } from "@/api/client";
import { DailyPoint, salesDaily } from "@/api/sales";

function isoDate(d: Date) {
  return d.toISOString().slice(0, 10);
}

export default function DashboardPage() {
  const [points, setPoints] = useState<DailyPoint[]>([]);
  const [error, setError] = useState<string | null>(null);

  const range = useMemo(() => {
    const to = new Date();
    const from = new Date();
    from.setUTCDate(to.getUTCDate() - 13);
    return { from: isoDate(from), to: isoDate(to) };
  }, []);

  const load = useCallback(async () => {
    setError(null);
    try {
      setPoints(await salesDaily(range.from, range.to));
    } catch (err) {
      setError(err instanceof ApiError ? err.detail ?? err.message : "Falha no dashboard");
    }
  }, [range.from, range.to]);

  useEffect(() => {
    void load();
  }, [load]);

  const totalRevenue = points.reduce((sum, p) => sum + Number(p.revenue), 0);
  const totalCount = points.reduce((sum, p) => sum + Number(p.count), 0);

  return (
    <AdminShell>
      <section className="rounded-xl border border-[var(--border)] bg-[var(--panel)] p-6">
        <h2 className="text-lg font-semibold">Dashboard</h2>
        <p className="text-sm text-[var(--muted)]">
          Receita diária (UTC) — últimos 14 dias
        </p>

        <div className="mt-4 flex flex-wrap gap-6">
          <div>
            <p className="text-xs uppercase text-[var(--muted)]">Receita</p>
            <p className="text-2xl font-semibold">R$ {totalRevenue.toFixed(2)}</p>
          </div>
          <div>
            <p className="text-xs uppercase text-[var(--muted)]">Vendas</p>
            <p className="text-2xl font-semibold">{totalCount}</p>
          </div>
        </div>

        {error && <p className="mt-3 text-sm text-red-400">{error}</p>}

        <div className="mt-6 h-72 w-full">
          {points.length === 0 ? (
            <p className="text-sm text-[var(--muted)]">Sem vendas no período.</p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={points}>
                <CartesianGrid strokeDasharray="3 3" stroke="#2a3548" />
                <XAxis dataKey="date" stroke="#8b9bb4" tick={{ fontSize: 12 }} />
                <YAxis stroke="#8b9bb4" tick={{ fontSize: 12 }} />
                <Tooltip
                  contentStyle={{
                    background: "#1a2332",
                    border: "1px solid #2a3548",
                    borderRadius: 8,
                  }}
                />
                <Bar dataKey="revenue" fill="#3d8bfd" name="Receita" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>
      </section>
    </AdminShell>
  );
}
