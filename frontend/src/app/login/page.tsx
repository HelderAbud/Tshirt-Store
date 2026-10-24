"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { login } from "@/api/auth";
import { ApiError } from "@/api/client";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await login(email, password);
      if (res.role !== "ADMIN") {
        setError("Acesso do backoffice exige role ADMIN");
        return;
      }
      router.replace("/dashboard");
    } catch (err) {
      setError(err instanceof ApiError ? err.detail ?? err.message : "Falha no login");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-md rounded-xl border border-[var(--border)] bg-[var(--panel)] p-8 shadow-xl"
      >
        <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">Tshirt Store</p>
        <h1 className="mt-2 text-2xl font-semibold">Login admin</h1>
        <p className="mt-1 text-sm text-[var(--muted)]">Gestão de estoque e vendas</p>

        <label className="mt-6 block text-sm text-[var(--muted)]">
          Email
          <input
            className="mt-1 w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)]"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            type="email"
            required
          />
        </label>
        <label className="mt-4 block text-sm text-[var(--muted)]">
          Senha
          <input
            className="mt-1 w-full rounded-md border border-[var(--border)] bg-[var(--bg)] px-3 py-2 text-[var(--text)]"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            required
          />
        </label>

        {error && <p className="mt-4 text-sm text-red-400">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="mt-6 w-full rounded-md bg-[var(--accent)] px-4 py-2 font-medium text-white disabled:opacity-60"
        >
          {loading ? "Entrando…" : "Entrar"}
        </button>
      </form>
    </main>
  );
}
