/**
 * FlyPro Analytics – Configuration Vite
 * Rôle : bundler / dev-server du frontend React + TypeScript.
 * Alias `@` → `src/` pour des imports Clean Architecture côté UI.
 */
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "node:path";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Proxy vers le backend Spring Boot (à activer lors de l'implémentation API)
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
