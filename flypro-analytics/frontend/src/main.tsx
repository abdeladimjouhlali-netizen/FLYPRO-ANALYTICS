/**
 * FlyPro Analytics – bootstrap React
 * Rôle futur : monter le routeur, QueryClient, providers (auth, thème).
 * Squelette uniquement — pas de logique métier.
 */
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./styles/index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
