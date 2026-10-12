/**
 * Utilitaire `cn` (classnames) pour Shadcn UI / Tailwind.
 * Rôle futur : fusionner les classes CSS conditionnelles (clsx + tailwind-merge).
 * Placeholder — implémentation minimale pour préparer l'intégration Shadcn.
 */
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
