import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AuthLayout } from "@/layouts/AuthLayout";
import { DashboardLayout } from "@/layouts/DashboardLayout";
import { EmployeeLayout } from "@/layouts/EmployeeLayout";
import { ProtectedRoute, PublicOnlyRoute } from "@/routes/ProtectedRoute";
import { PageLoader } from "@/components/ui/Feedback";
import { ROLES } from "@/constants/app";
import { useAuth } from "@/contexts/AuthContext";

const LoginPage = lazy(() =>
  import("@/features/auth/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const DashboardPage = lazy(() =>
  import("@/features/dashboard/DashboardPage").then((m) => ({ default: m.DashboardPage })),
);
const DepartmentsListPage = lazy(() =>
  import("@/features/departments/DepartmentsListPage").then((m) => ({ default: m.DepartmentsListPage })),
);
const DepartmentFormPage = lazy(() =>
  import("@/features/departments/DepartmentFormPage").then((m) => ({ default: m.DepartmentFormPage })),
);
const DepartmentDetailPage = lazy(() =>
  import("@/features/departments/DepartmentDetailPage").then((m) => ({ default: m.DepartmentDetailPage })),
);
const ZonesListPage = lazy(() =>
  import("@/features/zones/ZonesListPage").then((m) => ({ default: m.ZonesListPage })),
);
const ZoneFormPage = lazy(() =>
  import("@/features/zones/ZoneFormPage").then((m) => ({ default: m.ZoneFormPage })),
);
const ZoneDetailPage = lazy(() =>
  import("@/features/zones/ZoneDetailPage").then((m) => ({ default: m.ZoneDetailPage })),
);
const EmployeesListPage = lazy(() =>
  import("@/features/employees/EmployeesPages").then((m) => ({ default: m.EmployeesListPage })),
);
const EmployeeFormPage = lazy(() =>
  import("@/features/employees/EmployeesPages").then((m) => ({ default: m.EmployeeFormPage })),
);
const EmployeeDetailPage = lazy(() =>
  import("@/features/employees/EmployeesPages").then((m) => ({ default: m.EmployeeDetailPage })),
);
const BadgesListPage = lazy(() =>
  import("@/features/badges/BadgesPages").then((m) => ({ default: m.BadgesListPage })),
);
const BadgeFormPage = lazy(() =>
  import("@/features/badges/BadgesPages").then((m) => ({ default: m.BadgeFormPage })),
);
const BadgeDetailPage = lazy(() =>
  import("@/features/badges/BadgesPages").then((m) => ({ default: m.BadgeDetailPage })),
);
const BadgeAssignmentsPage = lazy(() =>
  import("@/features/badges/BadgesPages").then((m) => ({ default: m.BadgeAssignmentsPage })),
);
const LeaveRequestsListPage = lazy(() =>
  import("@/features/leaveRequests/LeaveRequestsPages").then((m) => ({ default: m.LeaveRequestsListPage })),
);
const AccessProfilesListPage = lazy(() =>
  import("@/features/accessProfiles/AccessControlPages").then((m) => ({ default: m.AccessProfilesListPage })),
);
const AccessProfileFormPage = lazy(() =>
  import("@/features/accessProfiles/AccessControlPages").then((m) => ({ default: m.AccessProfileFormPage })),
);
const AccessProfileDetailPage = lazy(() =>
  import("@/features/accessProfiles/AccessControlPages").then((m) => ({ default: m.AccessProfileDetailPage })),
);
const AccessPointsListPage = lazy(() =>
  import("@/features/accessProfiles/AccessControlPages").then((m) => ({ default: m.AccessPointsListPage })),
);
const AccessPointFormPage = lazy(() =>
  import("@/features/accessProfiles/AccessControlPages").then((m) => ({ default: m.AccessPointFormPage })),
);
const AccessPointDetailPage = lazy(() =>
  import("@/features/accessProfiles/AccessControlPages").then((m) => ({ default: m.AccessPointDetailPage })),
);
const AccessEventsListPage = lazy(() =>
  import("@/features/accessEvents/AccessEventsPages").then((m) => ({ default: m.AccessEventsListPage })),
);
const AccessEventFormPage = lazy(() =>
  import("@/features/accessEvents/AccessEventsPages").then((m) => ({ default: m.AccessEventFormPage })),
);
const AccessEventDetailPage = lazy(() =>
  import("@/features/accessEvents/AccessEventsPages").then((m) => ({ default: m.AccessEventDetailPage })),
);
const AlertsListPage = lazy(() =>
  import("@/features/alerts/AlertsPages").then((m) => ({ default: m.AlertsListPage })),
);
const AlertDetailPage = lazy(() =>
  import("@/features/alerts/AlertsPages").then((m) => ({ default: m.AlertDetailPage })),
);
const ImportsListPage = lazy(() =>
  import("@/features/imports/ImportsPages").then((m) => ({ default: m.ImportsListPage })),
);
const ImportUploadPage = lazy(() =>
  import("@/features/imports/ImportsPages").then((m) => ({ default: m.ImportUploadPage })),
);
const ImportDetailPage = lazy(() =>
  import("@/features/imports/ImportsPages").then((m) => ({ default: m.ImportDetailPage })),
);
const AnalyticsPage = lazy(() =>
  import("@/features/analytics/AnalyticsPage").then((m) => ({ default: m.AnalyticsPage })),
);
const DigitalTwinPage = lazy(() =>
  import("@/features/digitalTwin/DigitalTwinPage").then((m) => ({ default: m.DigitalTwinPage })),
);
const FaceVerificationPage = lazy(() =>
  import("@/features/faceVerification/FaceVerificationPage").then((m) => ({ default: m.FaceVerificationPage })),
);
const ProfilePage = lazy(() =>
  import("@/features/settings/MiscPages").then((m) => ({ default: m.ProfilePage })),
);
const RolesPage = lazy(() =>
  import("@/features/settings/MiscPages").then((m) => ({ default: m.RolesPage })),
);
const SettingsPage = lazy(() =>
  import("@/features/settings/MiscPages").then((m) => ({ default: m.SettingsPage })),
);
const UsersPage = lazy(() =>
  import("@/features/settings/MiscPages").then((m) => ({ default: m.UsersPage })),
);
const EmployeeDashboardPage = lazy(() =>
  import("@/features/employeePortal/EmployeePortalPages").then((m) => ({
    default: m.EmployeeDashboardPage,
  })),
);
const EmployeeBadgesPage = lazy(() =>
  import("@/features/employeePortal/EmployeePortalPages").then((m) => ({
    default: m.EmployeeBadgesPage,
  })),
);
const EmployeeAlertsPage = lazy(() =>
  import("@/features/employeePortal/EmployeePortalPages").then((m) => ({
    default: m.EmployeeAlertsPage,
  })),
);
const EmployeeLeaveRequestsPage = lazy(() =>
  import("@/features/employeePortal/EmployeePortalPages").then((m) => ({
    default: m.EmployeeLeaveRequestsPage,
  })),
);
const EmployeeProfilePage = lazy(() =>
  import("@/features/employeePortal/EmployeePortalPages").then((m) => ({
    default: m.EmployeeProfilePage,
  })),
);

function LazyPage({ children }: { children: ReactNode }) {
  return <Suspense fallback={<PageLoader />}>{children}</Suspense>;
}

function HomeRedirect() {
  const { user } = useAuth();
  if (user?.role === ROLES.EMPLOYEE) return <Navigate to="/employee" replace />;
  return <Navigate to="/dashboard" replace />;
}

export function AppRouter() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route element={<AuthLayout />}>
          <Route
            path="/login"
            element={
              <LazyPage>
                <LoginPage />
              </LazyPage>
            }
          />
        </Route>
      </Route>

      {/* Portail employé */}
      <Route element={<ProtectedRoute roles={[ROLES.EMPLOYEE]} />}>
        <Route element={<EmployeeLayout />}>
          <Route
            path="/employee"
            element={
              <LazyPage>
                <EmployeeDashboardPage />
              </LazyPage>
            }
          />
          <Route
            path="/employee/badges"
            element={
              <LazyPage>
                <EmployeeBadgesPage />
              </LazyPage>
            }
          />
          <Route
            path="/employee/leave-requests"
            element={
              <LazyPage>
                <EmployeeLeaveRequestsPage />
              </LazyPage>
            }
          />
          <Route
            path="/employee/alerts"
            element={
              <LazyPage>
                <EmployeeAlertsPage />
              </LazyPage>
            }
          />
          <Route
            path="/employee/profile"
            element={
              <LazyPage>
                <EmployeeProfilePage />
              </LazyPage>
            }
          />
        </Route>
      </Route>

      {/* Console administration */}
      <Route element={<ProtectedRoute roles={[ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.ANALYST]} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/" element={<HomeRedirect />} />
          <Route
            path="/dashboard"
            element={
              <LazyPage>
                <DashboardPage />
              </LazyPage>
            }
          />

          <Route
            path="/employees"
            element={
              <LazyPage>
                <EmployeesListPage />
              </LazyPage>
            }
          />
          <Route
            path="/employees/new"
            element={
              <LazyPage>
                <EmployeeFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/employees/:id"
            element={
              <LazyPage>
                <EmployeeDetailPage />
              </LazyPage>
            }
          />
          <Route
            path="/employees/:id/edit"
            element={
              <LazyPage>
                <EmployeeFormPage />
              </LazyPage>
            }
          />

          <Route
            path="/departments"
            element={
              <LazyPage>
                <DepartmentsListPage />
              </LazyPage>
            }
          />
          <Route
            path="/departments/new"
            element={
              <LazyPage>
                <DepartmentFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/departments/:id"
            element={
              <LazyPage>
                <DepartmentDetailPage />
              </LazyPage>
            }
          />
          <Route
            path="/departments/:id/edit"
            element={
              <LazyPage>
                <DepartmentFormPage />
              </LazyPage>
            }
          />

          <Route
            path="/badges"
            element={
              <LazyPage>
                <BadgesListPage />
              </LazyPage>
            }
          />
          <Route
            path="/badges/new"
            element={
              <LazyPage>
                <BadgeFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/badges/:id"
            element={
              <LazyPage>
                <BadgeDetailPage />
              </LazyPage>
            }
          />
          <Route
            path="/badges/:id/edit"
            element={
              <LazyPage>
                <BadgeFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/badge-assignments"
            element={
              <LazyPage>
                <BadgeAssignmentsPage />
              </LazyPage>
            }
          />
          <Route
            path="/leave-requests"
            element={
              <LazyPage>
                <LeaveRequestsListPage />
              </LazyPage>
            }
          />

          <Route
            path="/zones"
            element={
              <LazyPage>
                <ZonesListPage />
              </LazyPage>
            }
          />
          <Route
            path="/zones/new"
            element={
              <LazyPage>
                <ZoneFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/zones/:id"
            element={
              <LazyPage>
                <ZoneDetailPage />
              </LazyPage>
            }
          />
          <Route
            path="/zones/:id/edit"
            element={
              <LazyPage>
                <ZoneFormPage />
              </LazyPage>
            }
          />

          <Route
            path="/access-profiles"
            element={
              <LazyPage>
                <AccessProfilesListPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-profiles/new"
            element={
              <LazyPage>
                <AccessProfileFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-profiles/:id"
            element={
              <LazyPage>
                <AccessProfileDetailPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-profiles/:id/edit"
            element={
              <LazyPage>
                <AccessProfileFormPage />
              </LazyPage>
            }
          />

          <Route
            path="/access-points"
            element={
              <LazyPage>
                <AccessPointsListPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-points/new"
            element={
              <LazyPage>
                <AccessPointFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-points/:id"
            element={
              <LazyPage>
                <AccessPointDetailPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-points/:id/edit"
            element={
              <LazyPage>
                <AccessPointFormPage />
              </LazyPage>
            }
          />

          <Route
            path="/access-events"
            element={
              <LazyPage>
                <AccessEventsListPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-events/new"
            element={
              <LazyPage>
                <AccessEventFormPage />
              </LazyPage>
            }
          />
          <Route
            path="/access-events/:id"
            element={
              <LazyPage>
                <AccessEventDetailPage />
              </LazyPage>
            }
          />

          <Route element={<ProtectedRoute roles={[ROLES.SUPER_ADMIN, ROLES.ADMIN]} />}>
            <Route
              path="/imports"
              element={
                <LazyPage>
                  <ImportsListPage />
                </LazyPage>
              }
            />
            <Route
              path="/imports/upload"
              element={
                <LazyPage>
                  <ImportUploadPage />
                </LazyPage>
              }
            />
            <Route
              path="/imports/:id"
              element={
                <LazyPage>
                  <ImportDetailPage />
                </LazyPage>
              }
            />
          </Route>

          <Route
            path="/alerts"
            element={
              <LazyPage>
                <AlertsListPage />
              </LazyPage>
            }
          />
          <Route
            path="/alerts/:id"
            element={
              <LazyPage>
                <AlertDetailPage />
              </LazyPage>
            }
          />
          <Route
            path="/analytics"
            element={
              <LazyPage>
                <AnalyticsPage />
              </LazyPage>
            }
          />
          <Route
            path="/digital-twin"
            element={
              <LazyPage>
                <DigitalTwinPage />
              </LazyPage>
            }
          />
          <Route
            path="/face-verification"
            element={
              <LazyPage>
                <FaceVerificationPage />
              </LazyPage>
            }
          />
          <Route
            path="/settings"
            element={
              <LazyPage>
                <SettingsPage />
              </LazyPage>
            }
          />
          <Route
            path="/profile"
            element={
              <LazyPage>
                <ProfilePage />
              </LazyPage>
            }
          />

          <Route element={<ProtectedRoute roles={[ROLES.SUPER_ADMIN]} />}>
            <Route
              path="/users"
              element={
                <LazyPage>
                  <UsersPage />
                </LazyPage>
              }
            />
            <Route
              path="/roles"
              element={
                <LazyPage>
                  <RolesPage />
                </LazyPage>
              }
            />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<HomeRedirect />} />
    </Routes>
  );
}
