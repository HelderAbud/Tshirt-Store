import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Tshirt Store — Backoffice",
  description: "Gestão de estoque e vendas — loja única de revenda de roupas",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
