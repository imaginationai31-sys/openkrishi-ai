import { getToken } from "firebase/app-check";
import { appCheck } from "../firebase";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "https://openkrishi-ai-api.onrender.com").replace(/\/$/, "");

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  if (appCheck) {
    try {
      const token = await getToken(appCheck, false);
      if (token.token) headers.set("X-Firebase-AppCheck", token.token);
    } catch (error) {
      console.warn("Firebase App Check token unavailable:", error);
    }
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });
}
