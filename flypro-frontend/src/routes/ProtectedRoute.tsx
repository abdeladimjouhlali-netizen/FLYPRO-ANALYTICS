import { Navigate, Outlet, useLocation } from "react-router-dom";
import { PageLoader } from "@/components/ui/Feedback";
import type { AppRole } from "@/constants/app";
import { ROLES } from "@/constants/app";
import { useAuth } from "@/contexts/AuthContext";

export function ProtectedRoute({ roles }: { roles?: AppRole[] }) {
  const { isAuthenticated, loading, user } = useAuth();
  const location = useLocation();

  if (loading) return <div className="p-8"><PageLoader /></div>;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  if (roles && user && !roles.includes(user.role)) {
    const fallback = user.role === ROLES.EMPLOYEE ? "/employee" : "/dashboard";
    return <Navigate to={fallback} replace />;
  }
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { isAuthenticated, loading, user } = useAuth();
  if (loading) return <div className="p-8"><PageLoader /></div>;
  if (isAuthenticated) {
    const home = user?.role === ROLES.EMPLOYEE ? "/employee" : "/dashboard";
    return <Navigate to={home} replace />;
  }
  return <Outlet />;
}
