import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { AlertTriangle, CalendarClock, IdCard, LayoutDashboard, LogOut, Menu, Moon, Sun, UserRound, X } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { APP_BRAND } from "@/constants/app";
import { useAuth } from "@/contexts/AuthContext";
import { useTheme } from "@/contexts/ThemeContext";
import { cn } from "@/utils/cn";

const EMPLOYEE_NAV = [
  { label: "Accueil", to: "/employee", icon: LayoutDashboard, end: true },
  { label: "Mes badges", to: "/employee/badges", icon: IdCard, end: false },
  { label: "Mes congés", to: "/employee/leave-requests", icon: CalendarClock, end: false },
  { label: "Mes alertes", to: "/employee/alerts", icon: AlertTriangle, end: false },
  { label: "Mon profil", to: "/employee/profile", icon: UserRound, end: false },
];

export function EmployeeLayout() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  const sidebar = (
    <aside className="flex h-full w-64 flex-col border-r border-white/20 bg-white/92 shadow-xl backdrop-blur-md dark:bg-[var(--color-surface)]/92">
      <div className="border-b border-[var(--color-border)] bg-white px-4 py-4">
        <Link to="/employee" className="block" aria-label="Royal Air Maroc">
          <img
            src="/brand/royal-air-maroc-logo.png"
            alt="Royal Air Maroc"
            className="mx-auto h-auto w-full max-w-[170px] object-contain object-center"
            decoding="async"
          />
        </Link>
      </div>
      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {EMPLOYEE_NAV.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  "relative flex items-center gap-3 rounded-md py-2.5 pr-3 pl-3.5 text-sm font-semibold tracking-wide transition",
                  isActive
                    ? "bg-gradient-to-r from-brand-700 to-brand-600 text-white shadow-sm shadow-brand-600/25"
                    : "text-[var(--color-ink-700)] hover:bg-brand-50 hover:text-brand-800",
                )
              }
            >
              {({ isActive }) =>
                isActive ? (
                  <>
                    <span
                      className="absolute top-1/2 left-0 h-5 w-0.5 -translate-y-1/2 rounded-full bg-[var(--color-gold)]"
                      aria-hidden
                    />
                    <Icon className="size-4" />
                    {item.label}
                  </>
                ) : (
                  <>
                    <Icon className="size-4" />
                    {item.label}
                  </>
                )
              }
            </NavLink>
          );
        })}
      </nav>
      <div className="border-t border-[var(--color-border)] p-4 text-xs text-[var(--color-ink-500)]">
        © {new Date().getFullYear()} {APP_BRAND}
      </div>
    </aside>
  );

  return (
    <div className="relative min-h-screen">
      <div
        className="pointer-events-none fixed inset-0 -z-10 bg-cover bg-center bg-no-repeat"
        style={{ backgroundImage: "url('/brand/ram-dreamliner-bg.png')" }}
        aria-hidden
      />
      <div className="pointer-events-none fixed inset-0 -z-10 bg-gradient-to-b from-black/55 via-black/40 to-black/65" aria-hidden />

      <div className="relative z-0 flex min-h-screen lg:flex">
        <div className="hidden lg:block">{sidebar}</div>
        {open ? (
          <div className="fixed inset-0 z-40 flex lg:hidden">
            <div className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} />
            <div className="relative z-10 h-full">{sidebar}</div>
          </div>
        ) : null}

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b-2 border-b-[var(--color-gold)]/60 bg-white/95 shadow-sm backdrop-blur-md dark:bg-[var(--color-surface)]/95">
            <div className="flex min-w-0 flex-1 items-center">
              <Button
                variant="ghost"
                size="sm"
                className="m-2 shrink-0 lg:hidden"
                onClick={() => setOpen(true)}
              >
                {open ? <X className="size-4" /> : <Menu className="size-4" />}
              </Button>
              <div className="min-w-0 px-3 sm:px-4">
                <p className="truncate text-sm font-semibold">{user?.username}</p>
                <p className="truncate text-xs text-[var(--color-ink-500)]">Portail employé</p>
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-2 px-3 sm:px-4">
              <Button variant="ghost" size="sm" onClick={toggle} aria-label="Basculer le thème">
                {theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
              </Button>
              <Button variant="ghost" size="sm" onClick={() => void handleLogout()}>
                <LogOut className="size-4" />
                Déconnexion
              </Button>
            </div>
          </header>
          <main className="flex-1 px-4 py-6 sm:px-6 lg:px-8">
            <Outlet />
          </main>
          <footer className="border-t border-white/15 bg-black/30 px-6 py-4 text-center text-xs text-white/80 backdrop-blur-sm">
            {APP_BRAND} — portail employé
          </footer>
        </div>
      </div>
    </div>
  );
}
