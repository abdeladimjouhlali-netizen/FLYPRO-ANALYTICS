import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import toast from "react-hot-toast";
import { tokenStore } from "@/api/tokenStore";
import type { ApiResponse, ErrorResponse, RefreshResponse } from "@/types/api";

declare module "axios" {
  export interface AxiosRequestConfig {
    /** When true, the response-error interceptor skips its automatic toast (caller handles it). */
    skipErrorToast?: boolean;
  }
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const AI_BASE = import.meta.env.VITE_AI_BASE_URL ?? "http://localhost:8000";

export const api = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
});

export const aiApi = axios.create({
  baseURL: AI_BASE,
  headers: { "Content-Type": "application/json" },
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return null;
  try {
    const { data } = await axios.post<ApiResponse<RefreshResponse>>(
      `${API_BASE}/api/v1/auth/refresh`,
      { refreshToken },
    );
    const payload = data.data;
    if (!payload) return null;
    tokenStore.set(payload.accessToken, payload.refreshToken);
    return payload.accessToken;
  } catch {
    tokenStore.clear();
    return null;
  }
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorResponse>) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true;
      refreshing ??= refreshAccessToken().finally(() => {
        refreshing = null;
      });
      const token = await refreshing;
      if (token) {
        original.headers.Authorization = `Bearer ${token}`;
        return api(original);
      }
      if (!window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }

    const message =
      error.response?.data?.message ??
      error.message ??
      "Une erreur inattendue est survenue";
    if (error.response?.status !== 401) {
      toast.error(message);
    }
    return Promise.reject(error);
  },
);

aiApi.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string }>) => {
    if (!error.config?.skipErrorToast) {
      toast.error(error.response?.data?.message ?? "Erreur service AI");
    }
    return Promise.reject(error);
  },
);

export function unwrap<T>(response: ApiResponse<T>): T {
  if (response.data === undefined || response.data === null) {
    throw new Error(response.message ?? "Réponse API vide");
  }
  return response.data;
}
