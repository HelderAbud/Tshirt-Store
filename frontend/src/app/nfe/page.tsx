"use client";

import { FormEvent, useState } from "react";
import { AdminShell } from "@/components/AdminShell";
import { ApiError } from "@/api/client";
import { importNfe } from "@/api/nfe";

export default function NfePage() {
  const [message, setMessage] = useState("XML já autorizado. A chave repetida não mexe no estoque.");
  const [error, setError] = useState(false);
  const [sending, setSending] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const input = event.currentTarget.elements.namedItem("file");
    if (!(input instanceof HTMLInputElement) || !input.files?.[0]) return;
    setSending(true);
    setError(false);
    setMessage("Importando…");
    try {
      const result = await importNfe(input.files[0]);
      setMessage(`${result.accessKey} · ${result.direction}`);
    } catch (err) {
      setError(true);
      setMessage(err instanceof ApiError ? err.detail ?? err.message : "Falha na importação");
    } finally {
      setSending(false);
    }
  }

  return (
    <AdminShell>
      <section className="rounded-xl border border-[var(--border)] bg-[var(--panel)] p-6">
        <h2 className="text-lg font-semibold">NF-e</h2>
        <form onSubmit={onSubmit} className="mt-4 flex flex-wrap gap-3">
          <input name="file" type="file" accept=".xml,text/xml,application/xml" required />
          <button
            type="submit"
            disabled={sending}
            className="rounded-md bg-[var(--accent)] px-4 py-2 text-white disabled:opacity-60"
          >
            Importar
          </button>
        </form>
        <p className={`mt-3 text-sm ${error ? "text-red-400" : "text-[var(--muted)]"}`}>{message}</p>
      </section>
    </AdminShell>
  );
}
