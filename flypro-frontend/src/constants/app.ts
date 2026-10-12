export const APP_NAME = "FlyPro";
export const APP_BRAND = "Royal Air Maroc";
export const APP_TAGLINE = "Plateforme intelligente de gestion des badges & accès";

export const STORAGE_KEYS = {
  accessToken: "flypro.accessToken",
  refreshToken: "flypro.refreshToken",
  theme: "flypro.theme",
} as const;

export const ROLES = {
  SUPER_ADMIN: "ROLE_SUPER_ADMIN",
  ADMIN: "ROLE_ADMIN",
  ANALYST: "ROLE_ANALYST",
  EMPLOYEE: "ROLE_EMPLOYEE",
} as const;

export type AppRole = (typeof ROLES)[keyof typeof ROLES];

export const ROLE_LABELS: Record<AppRole, string> = {
  ROLE_SUPER_ADMIN: "Super Admin",
  ROLE_ADMIN: "Administrateur",
  ROLE_ANALYST: "Analyste",
  ROLE_EMPLOYEE: "Employé",
};
