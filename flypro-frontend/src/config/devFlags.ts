/** Feature flags for local development. Flip when restoring JWT. */
export const AUTH_DISABLED = import.meta.env.VITE_AUTH_DISABLED === "true";

export const DEV_USER = {
  id: 1,
  username: "admin",
  email: "admin@flypro.local",
  enabled: true,
  accountLocked: false,
  role: "ROLE_SUPER_ADMIN" as const,
};
