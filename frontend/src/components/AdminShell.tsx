"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { clearToken, getToken } from "@/api/client";
import { useEffect, useState } from "react";

const nav = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/inventory", label: "Estoque" },
  { href: "/sales", label: "Venda" },
  { href: "/nfe", label: "NF-e" },
];

export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/login");
      return;
    }
    setReady(true);
  }, [router]);

  if (!ready) {
    return (
      <main className="flex min-h-screen items-center justify-center text-[var(--muted)]">
        Carregando…
      </main>
    );
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-5xl flex-col gap-6 px-4 py-6">
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-[var(--border)] pb-4">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">
            Tshirt Store
          </p>
          <h1 className="text-xl font-semibold">Backoffice</h1>
        </div>
        <nav className="flex flex-wrap gap-2">
          {nav.map((item) => {
            const active = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`rounded-md px-3 py-2 text-sm ${
                  active
                    ? "bg-[var(--accent)] text-white"
                    : "bg-[var(--panel)] text-[var(--muted)] hover:text-white"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
          <button
            type="button"
            className="rounded-md px-3 py-2 text-sm text-[var(--muted)] hover:text-white"
            onClick={() => {
              clearToken();
              router.replace("/login");
            }}
          >
            Sair
          </button>
        </nav>
      </header>
      {children}
    </div>
  );
}
