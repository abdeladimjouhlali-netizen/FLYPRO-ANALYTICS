import {
  Activity,
  AlertTriangle,
  BadgeCheck,
  Building2,
  CalendarClock,
  DoorOpen,
  FileUp,
  LayoutDashboard,
  MapPin,
  Shield,
  Sparkles,
  UserRound,
  Users,
  Settings,
  KeyRound,
  IdCard,
  Map,
  ScanFace,
} from "lucide-react";
import type { AppRole } from "@/constants/app";
import { ROLES } from "@/constants/app";

export type NavItem = {
  label: string;
  to: string;
  icon: typeof LayoutDashboard;
  roles: AppRole[];
};

export const NAV_ITEMS: NavItem[] = [
  { label: "Dashboard", to: "/dashboard", icon: LayoutDashboard, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Employés", to: "/employees", icon: Users, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Départements", to: "/departments", icon: Building2, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Badges", to: "/badges", icon: BadgeCheck, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Affectations", to: "/badge-assignments", icon: IdCard, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Demandes de congé", to: "/leave-requests", icon: CalendarClock, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN] },
  { label: "Zones", to: "/zones", icon: MapPin, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Profils d'accès", to: "/access-profiles", icon: Shield, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Points d'accès", to: "/access-points", icon: DoorOpen, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Événements", to: "/access-events", icon: Activity, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Digital Twin", to: "/digital-twin", icon: Map, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Vérification faciale", to: "/face-verification", icon: ScanFace, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Imports", to: "/imports", icon: FileUp, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN] },
  { label: "Alertes", to: "/alerts", icon: AlertTriangle, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Analytics AI", to: "/analytics", icon: Sparkles, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
  { label: "Utilisateurs", to: "/users", icon: UserRound, roles: [ROLES.SUPER_ADMIN] },
  { label: "Rôles", to: "/roles", icon: KeyRound, roles: [ROLES.SUPER_ADMIN] },
  { label: "Paramètres", to: "/settings", icon: Settings, roles: [ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST] },
];

export type NavSection = {
  id: string;
  label: string;
  items: NavItem[];
};

export const NAV_SECTIONS: NavSection[] = [
  {
    id: "ops",
    label: "Opérations",
    items: NAV_ITEMS.filter((i) =>
      ["/dashboard", "/employees", "/departments", "/badges", "/badge-assignments", "/leave-requests"].includes(i.to),
    ),
  },
  {
    id: "access",
    label: "Contrôle d'accès",
    items: NAV_ITEMS.filter((i) =>
      ["/zones", "/access-profiles", "/access-points", "/access-events", "/digital-twin", "/face-verification"].includes(i.to),
    ),
  },
  {
    id: "intel",
    label: "Intelligence",
    items: NAV_ITEMS.filter((i) => ["/imports", "/alerts", "/analytics"].includes(i.to)),
  },
  {
    id: "admin",
    label: "Administration",
    items: NAV_ITEMS.filter((i) => ["/users", "/roles", "/settings"].includes(i.to)),
  },
];

export function canAccess(role: AppRole | undefined, roles: AppRole[]) {
  if (!role) return false;
  return roles.includes(role);
}
