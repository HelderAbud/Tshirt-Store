import { apiFetch } from "./client";

export type NfeImportResult = {
  accessKey: string;
  direction: string;
};

export async function importNfe(file: File): Promise<NfeImportResult> {
  const body = new FormData();
  body.append("file", file);
  return apiFetch<NfeImportResult>("/api/admin/nfe/import", { method: "POST", body });
}

export async function createSupplierCode(supplierCnpj: string, supplierCode: string, sku: string) {
  return apiFetch("/api/admin/nfe/supplier-codes", {
    method: "POST",
    body: JSON.stringify({ supplierCnpj, supplierCode, sku }),
  });
}
