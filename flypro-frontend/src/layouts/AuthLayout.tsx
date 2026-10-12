import { Outlet } from "react-router-dom";
import { Activity, IdCard, Plane, ShieldCheck } from "lucide-react";
import { APP_BRAND, APP_TAGLINE } from "@/constants/app";

const HIGHLIGHTS = [
  { icon: IdCard, text: "Gestion centralisée des badges et des accès" },
  { icon: ShieldCheck, text: "Sécurité JWT, RBAC et détection d'anomalies IA" },
  { icon: Activity, text: "Supervision temps réel des flux et des alertes" },
];

export function AuthLayout() {
  return (
    <div className="grid min-h-screen lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
      {/* Panneau de marque — visible à partir de lg */}
      <div className="relative hidden overflow-hidden lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16">
        <div
          className="absolute inset-0 bg-cover bg-center"
          style={{ backgroundImage: "url('/brand/ram-dreamliner-bg.png')" }}
          aria-hidden
        />
        <div
          className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/35 to-black/10"
          aria-hidden
        />
        <div
          className="pointer-events-none absolute inset-x-0 bottom-0 h-1.5"
          style={{ background: "linear-gradient(90deg, transparent, var(--color-gold), transparent)" }}
          aria-hidden
        />

        <div className="relative z-10">
          <img
            src="/brand/royal-air-maroc-logo.png"
            alt="Royal Air Maroc"
            className="h-11 w-auto rounded-lg bg-white/95 px-2.5 py-1.5 object-contain"
            decoding="async"
          />
        </div>

        <div className="relative z-10 max-w-md">
          <p className="mb-4 inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-[11px] font-semibold tracking-[0.2em] text-white uppercase backdrop-blur-sm">
            <Plane className="size-3.5" />
            Plateforme opérationnelle
          </p>
          <h1 className="ram-wordmark text-4xl leading-tight text-white drop-shadow-sm xl:text-5xl">
            {APP_BRAND}
          </h1>
          <p className="mt-4 text-base text-white/85">{APP_TAGLINE}</p>

          <ul className="mt-9 space-y-4">
            {HIGHLIGHTS.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-center gap-3 text-sm text-white/90">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-white/15 backdrop-blur-sm ring-1 ring-white/20">
                  <Icon className="size-4" />
                </span>
                {text}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative z-10 text-xs text-white/60">
          © {new Date().getFullYear()} {APP_BRAND} — Plateforme interne
        </p>
      </div>

      {/* Panneau formulaire */}
      <div className="relative flex items-center justify-center px-4 py-10 lg:bg-[var(--color-surface)]">
        <div
          className="absolute inset-0 bg-cover bg-center lg:hidden"
          style={{ backgroundImage: "url('/brand/ram-dreamliner-bg.png')" }}
          aria-hidden
        />
        <div className="absolute inset-0 bg-black/55 lg:hidden" aria-hidden />
        <div className="relative z-10 w-full max-w-md">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
