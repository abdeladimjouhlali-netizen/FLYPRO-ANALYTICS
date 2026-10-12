import { STORAGE_KEYS } from "@/constants/app";

export const tokenStore = {
  getAccess(): string | null {
    return localStorage.getItem(STORAGE_KEYS.accessToken);
  },
  getRefresh(): string | null {
    return localStorage.getItem(STORAGE_KEYS.refreshToken);
  },
  set(accessToken: string, refreshToken: string) {
    localStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
    localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
  },
  clear() {
    localStorage.removeItem(STORAGE_KEYS.accessToken);
    localStorage.removeItem(STORAGE_KEYS.refreshToken);
  },
};
