import { Link, NavLink } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { APP_BRAND, APP_NAME } from "@/constants/app";
import { NAV_SECTIONS, canAccess } from "@/constants/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { cn } from "@/utils/cn";

type SidebarProps = {
  collapsed: boolean;
  onToggle: () => void;
  mobile?: boolean;
  onNavigate?: () => void;
};

export function Sidebar({ collapsed, onToggle, mobile, onNavigate }: SidebarProps) {
  const { user } = useAuth();

  return (
    <aside
      className={cn(
        "oc-sidebar relative flex h-full flex-col text-slate-200 transition-[width] duration-300 ease-out",
        collapsed && !mobile ? "w-[72px]" : "w-72",
      )}
      aria-label="Navigation principale"
    >
      <div className="flex h-16 items-center gap-3 border-b border-white/10 px-4">
        <Link
          to="/dashboard"
          className="flex min-w-0 items-center gap-3"
          onClick={onNavigate}
          aria-label={`${APP_NAME} — ${APP_BRAND}`}
        >
          <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-sky-400/15 ring-1 ring-sky-400/30">
            <img
              src="/brand/royal-air-maroc-logo.png"
              alt=""
              className="size-7 object-contain"
              decoding="async"
            />
          </span>
          <AnimatePresence initial={false}>
            {(!collapsed || mobile) && (
              <motion.span
                initial={{ opacity: 0, x: -8 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -8 }}
                className="min-w-0"
              >
                <span className="block truncate text-sm font-semibold tracking-tight text-white">
                  {APP_NAME}
                </span>
                <span className="block truncate text-[10px] font-medium tracking-[0.14em] text-sky-300/80 uppercase">
                  Operations Center
                </span>
              </motion.span>
            )}
          </AnimatePresence>
        </Link>
      </div>

      <nav className="flex-1 space-y-5 overflow-y-auto px-3 py-4">
        {NAV_SECTIONS.map((section) => {
          const items = section.items.filter((item) => canAccess(user?.role, item.roles));
          if (!items.length) return null;
          return (
            <div key={section.id}>
              {(!collapsed || mobile) && (
                <p className="mb-2 px-2 text-[10px] font-semibold tracking-[0.16em] text-slate-500 uppercase">
                  {section.label}
                </p>
              )}
              <ul className="space-y-0.5">
                {items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <li key={`${section.id}-${item.to}-${item.label}`}>
                      <NavLink
                        to={item.to}
                        end={item.to === "/dashboard"}
                        onClick={onNavigate}
                        title={collapsed && !mobile ? item.label : undefined}
                        className={({ isActive }) =>
                          cn(
                            "group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                            isActive
                              ? "bg-sky-400/15 text-white shadow-inner ring-1 ring-sky-400/25"
                              : "text-slate-300 hover:bg-white/5 hover:text-white",
                          )
                        }
                      >
                        <Icon className="size-[18px] shrink-0 opacity-90" aria-hidden />
                        {(!collapsed || mobile) && <span className="truncate">{item.label}</span>}
                      </NavLink>
                    </li>
                  );
                })}
              </ul>
            </div>
          );
        })}
      </nav>

      {!mobile && (
        <div className="border-t border-white/10 p-3">
          <button
            type="button"
            onClick={onToggle}
            className="flex w-full items-center justify-center gap-2 rounded-xl px-3 py-2 text-xs font-medium text-slate-400 transition hover:bg-white/5 hover:text-white"
            aria-label={collapsed ? "Étendre la sidebar" : "Réduire la sidebar"}
          >
            {collapsed ? <ChevronRight className="size-4" /> : <ChevronLeft className="size-4" />}
            {!collapsed && <span>Réduire</span>}
          </button>
        </div>
      )}
    </aside>
  );
}
