import { useMemo, useRef, useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  Bell,
  ChevronDown,
  Globe,
  LogOut,
  Menu,
  Moon,
  Search,
  Sun,
  UserRound,
} from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";
import { Button } from "@/components/ui/Button";
import { ROLE_LABELS } from "@/constants/app";
import { AUTH_DISABLED } from "@/config/devFlags";
import { useAuth } from "@/contexts/AuthContext";
import { useTheme } from "@/contexts/ThemeContext";
import { cn } from "@/utils/cn";

type NavbarProps = {
  onMenuClick: () => void;
};

const LANGS = [
  { code: "fr", label: "FR" },
  { code: "en", label: "EN" },
  { code: "ar", label: "AR" },
] as const;

function useBreadcrumbs() {
  const { pathname } = useLocation();
  return useMemo(() => {
    const parts = pathname.split("/").filter(Boolean);
    const crumbs: { label: string; to?: string }[] = [{ label: "Accueil", to: "/dashboard" }];
    let acc = "";
    parts.forEach((part, i) => {
      acc += `/${part}`;
      const label = part
        .replace(/-/g, " ")
        .replace(/\b\w/g, (c) => c.toUpperCase());
      crumbs.push({
        label: /^\d+$/.test(part) ? `#${part}` : label,
        to: i === parts.length - 1 ? undefined : acc,
      });
    });
    return crumbs;
  }, [pathname]);
}

export function Navbar({ onMenuClick }: NavbarProps) {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const breadcrumbs = useBreadcrumbs();
  const [lang, setLang] = useState<(typeof LANGS)[number]["code"]>("fr");
  const [profileOpen, setProfileOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [query, setQuery] = useState("");
  const searchRef = useRef<HTMLInputElement>(null);

  async function handleLogout() {
    await logout();
    if (!AUTH_DISABLED) navigate("/login");
  }

  function onSearchSubmit(e: FormEvent) {
    e.preventDefault();
    const q = query.trim();
    if (!q) return;
    navigate(`/employees?q=${encodeURIComponent(q)}`);
  }

  return (
    <header className="sticky top-0 z-30 border-b border-[var(--color-border)] bg-white/90 shadow-sm backdrop-blur-md dark:bg-[var(--color-surface)]/90">
      <div className="flex h-16 items-center gap-3 px-4 sm:px-6">
        <Button
          variant="ghost"
          size="sm"
          className="lg:hidden"
          onClick={onMenuClick}
          aria-label="Ouvrir le menu"
        >
          <Menu className="size-4" />
        </Button>

        <form onSubmit={onSearchSubmit} className="relative hidden min-w-0 flex-1 md:block md:max-w-md">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-ink-400" aria-hidden />
          <input
            ref={searchRef}
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Rechercher employés, badges, zones…"
            className="h-10 w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-surface-muted)] pr-3 pl-10 text-sm outline-none transition focus:border-brand-600 focus:ring-2 focus:ring-sky-400/30"
            aria-label="Recherche globale"
          />
        </form>

        <div className="ml-auto flex items-center gap-1 sm:gap-2">
          <div className="relative">
            <Button
              variant="ghost"
              size="sm"
              aria-label="Notifications"
              aria-expanded={notifOpen}
              onClick={() => {
                setNotifOpen((v) => !v);
                setProfileOpen(false);
              }}
            >
              <Bell className="size-4" />
              <span className="absolute top-1.5 right-1.5 size-1.5 rounded-full bg-danger" />
            </Button>
            <AnimatePresence>
              {notifOpen ? (
                <motion.div
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 6 }}
                  className="absolute right-0 mt-2 w-80 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-3 shadow-xl"
                  role="menu"
                >
                  <p className="px-1 text-xs font-semibold tracking-wide text-ink-500 uppercase">
                    Notifications
                  </p>
                  <ul className="mt-2 space-y-1">
                    <li className="rounded-lg px-2 py-2 text-sm hover:bg-[var(--color-surface-muted)]">
                      <p className="font-medium">Alerte CRITICAL</p>
                      <p className="text-xs text-ink-500">Accès refusé zone sécurisée</p>
                    </li>
                    <li className="rounded-lg px-2 py-2 text-sm hover:bg-[var(--color-surface-muted)]">
                      <p className="font-medium">Import terminé</p>
                      <p className="text-xs text-ink-500">Batch employés validé</p>
                    </li>
                  </ul>
                  <Link
                    to="/alerts"
                    className="mt-2 block rounded-lg px-2 py-1.5 text-center text-xs font-semibold text-brand-600 hover:bg-brand-50"
                    onClick={() => setNotifOpen(false)}
                  >
                    Voir toutes les alertes
                  </Link>
                </motion.div>
              ) : null}
            </AnimatePresence>
          </div>

          <div className="hidden items-center gap-1 rounded-xl border border-[var(--color-border)] p-0.5 sm:flex">
            {LANGS.map((l) => (
              <button
                key={l.code}
                type="button"
                onClick={() => setLang(l.code)}
                className={cn(
                  "rounded-lg px-2 py-1 text-[11px] font-semibold transition",
                  lang === l.code
                    ? "bg-navy-900 text-white dark:bg-sky-400/20"
                    : "text-ink-500 hover:text-ink-900",
                )}
                aria-label={`Langue ${l.label}`}
              >
                {l.label}
              </button>
            ))}
            <Globe className="mr-1.5 size-3.5 text-ink-400" aria-hidden />
          </div>

          <Button variant="ghost" size="sm" onClick={toggle} aria-label="Basculer le thème">
            {theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
          </Button>

          <div className="relative">
            <button
              type="button"
              onClick={() => {
                setProfileOpen((v) => !v);
                setNotifOpen(false);
              }}
              className="flex items-center gap-2 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] px-2 py-1.5 text-left transition hover:border-sky-400/40"
              aria-expanded={profileOpen}
              aria-haspopup="menu"
            >
              <span className="flex size-8 items-center justify-center rounded-lg bg-navy-900 text-xs font-bold text-white">
                {(user?.username ?? "U").slice(0, 2).toUpperCase()}
              </span>
              <span className="hidden min-w-0 sm:block">
                <span className="block truncate text-sm font-semibold leading-tight">
                  {user?.username}
                </span>
                <span className="block truncate text-[11px] text-ink-500">
                  {user ? ROLE_LABELS[user.role] : ""}
                </span>
              </span>
              <ChevronDown className="size-3.5 text-ink-400" />
            </button>
            <AnimatePresence>
              {profileOpen ? (
                <motion.div
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 6 }}
                  className="absolute right-0 mt-2 w-52 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] py-1 shadow-xl"
                  role="menu"
                >
                  <button
                    type="button"
                    className="flex w-full items-center gap-2 px-3 py-2 text-sm hover:bg-[var(--color-surface-muted)]"
                    onClick={() => {
                      setProfileOpen(false);
                      navigate("/profile");
                    }}
                  >
                    <UserRound className="size-4" /> Profil
                  </button>
                  {!AUTH_DISABLED ? (
                    <button
                      type="button"
                      className="flex w-full items-center gap-2 px-3 py-2 text-sm text-danger hover:bg-red-50 dark:hover:bg-red-950/30"
                      onClick={() => {
                        setProfileOpen(false);
                        void handleLogout();
                      }}
                    >
                      <LogOut className="size-4" /> Déconnexion
                    </button>
                  ) : null}
                </motion.div>
              ) : null}
            </AnimatePresence>
          </div>
        </div>
      </div>

      <div className="flex h-9 items-center gap-2 border-t border-[var(--color-border)]/70 bg-[var(--color-surface-muted)]/80 px-4 text-xs text-ink-500 sm:px-6">
        <nav aria-label="Fil d'Ariane" className="flex flex-wrap items-center gap-1">
          {breadcrumbs.map((b, i) => (
            <span key={`${b.label}-${i}`} className="inline-flex items-center gap-1">
              {i > 0 ? <span className="text-ink-300">/</span> : null}
              {b.to ? (
                <Link to={b.to} className="hover:text-brand-600">
                  {b.label}
                </Link>
              ) : (
                <span className="font-medium text-ink-700">{b.label}</span>
              )}
            </span>
          ))}
        </nav>
      </div>
    </header>
  );
}
