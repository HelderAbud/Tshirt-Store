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
