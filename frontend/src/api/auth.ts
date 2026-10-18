import { apiFetch, setToken } from "./client";

export type LoginResponse = {
  token: string;
  email: string;
  role: string;
};

export async function login(email: string, password: string): Promise<LoginResponse> {
  const data = await apiFetch<LoginResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  setToken(data.token);
  return data;
}
