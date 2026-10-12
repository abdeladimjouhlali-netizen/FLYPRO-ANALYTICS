import { api, aiApi, unwrap } from "@/api/client";
import { tokenStore } from "@/api/tokenStore";
import type {
  AccessEvent,
  AccessEventRequest,
  AccessPoint,
  AccessPointRequest,
  AccessProfile,
  AccessProfileRequest,
  AiPrediction,
  Alert,
  ApiResponse,
  Badge,
  BadgeRequest,
  ChartDto,
  Department,
  DepartmentRequest,
  Employee,
  EmployeeRequest,
  ImportBatch,
  Kpi,
  LeaveRequest,
  LeaveRequestCreate,
  LeaveRequestReview,
  LeaveStatus,
  LeaveType,
  LoginResponse,
  NamedCount,
  PageQuery,
  PageResponse,
  RoleName,
  Statistics,
  Trend,
  User,
  Zone,
  ZoneRequest,
} from "@/types/api";

function toParams(query?: PageQuery) {
  if (!query) return undefined;
  const params: Record<string, string | number | boolean> = {};
  Object.entries(query).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") params[k] = v;
  });
  return params;
}

export const authApi = {
  async login(username: string, password: string) {
    const { data } = await api.post<ApiResponse<LoginResponse>>("/api/v1/auth/login", {
      username,
      password,
    });
    const payload = unwrap(data);
    tokenStore.set(payload.accessToken, payload.refreshToken);
    return payload;
  },
  async me() {
    const { data } = await api.get<ApiResponse<User>>("/api/v1/auth/me");
    return unwrap(data);
  },
  async logout() {
    const refreshToken = tokenStore.getRefresh();
    try {
      await api.post("/api/v1/auth/logout", refreshToken ? { refreshToken } : {});
    } finally {
      tokenStore.clear();
    }
  },
};

export const departmentsApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<Department>>>("/api/v1/departments", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<Department>>(`/api/v1/departments/${id}`)).data),
  create: async (body: DepartmentRequest) =>
    unwrap((await api.post<ApiResponse<Department>>("/api/v1/departments", body)).data),
  update: async (id: number, body: DepartmentRequest) =>
    unwrap((await api.put<ApiResponse<Department>>(`/api/v1/departments/${id}`, body)).data),
  remove: async (id: number) => {
    await api.delete(`/api/v1/departments/${id}`);
  },
};

export const employeesApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<Employee>>>("/api/v1/employees", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<Employee>>(`/api/v1/employees/${id}`)).data),
  create: async (body: EmployeeRequest) =>
    unwrap((await api.post<ApiResponse<Employee>>("/api/v1/employees", body)).data),
  update: async (id: number, body: EmployeeRequest) =>
    unwrap((await api.put<ApiResponse<Employee>>(`/api/v1/employees/${id}`, body)).data),
  remove: async (id: number) => {
    await api.delete(`/api/v1/employees/${id}`);
  },
  activate: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Employee>>(`/api/v1/employees/${id}/activate`)).data),
  deactivate: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Employee>>(`/api/v1/employees/${id}/deactivate`)).data),
  archive: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Employee>>(`/api/v1/employees/${id}/archive`)).data),
  badges: async (id: number) =>
    unwrap((await api.get<ApiResponse<Badge[]>>(`/api/v1/employees/${id}/badges`)).data),
};

export const badgesApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<Badge>>>("/api/v1/badges", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<Badge>>(`/api/v1/badges/${id}`)).data),
  create: async (body: BadgeRequest) =>
    unwrap((await api.post<ApiResponse<Badge>>("/api/v1/badges", body)).data),
  update: async (id: number, body: BadgeRequest) =>
    unwrap((await api.put<ApiResponse<Badge>>(`/api/v1/badges/${id}`, body)).data),
  remove: async (id: number) => {
    await api.delete(`/api/v1/badges/${id}`);
  },
  assign: async (id: number, employeeId: number) =>
    unwrap((await api.post<ApiResponse<Badge>>(`/api/v1/badges/${id}/assign/${employeeId}`)).data),
  unassign: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Badge>>(`/api/v1/badges/${id}/unassign`)).data),
  suspend: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Badge>>(`/api/v1/badges/${id}/suspend`)).data),
  activate: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Badge>>(`/api/v1/badges/${id}/activate`)).data),
  revoke: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Badge>>(`/api/v1/badges/${id}/revoke`)).data),
};

export const zonesApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<Zone>>>("/api/v1/zones", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<Zone>>(`/api/v1/zones/${id}`)).data),
  create: async (body: ZoneRequest) =>
    unwrap((await api.post<ApiResponse<Zone>>("/api/v1/zones", body)).data),
  update: async (id: number, body: ZoneRequest) =>
    unwrap((await api.put<ApiResponse<Zone>>(`/api/v1/zones/${id}`, body)).data),
  remove: async (id: number) => {
    await api.delete(`/api/v1/zones/${id}`);
  },
};

export const accessProfilesApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<AccessProfile>>>("/api/v1/access-profiles", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<AccessProfile>>(`/api/v1/access-profiles/${id}`)).data),
  create: async (body: AccessProfileRequest) =>
    unwrap((await api.post<ApiResponse<AccessProfile>>("/api/v1/access-profiles", body)).data),
  update: async (id: number, body: AccessProfileRequest) =>
    unwrap((await api.put<ApiResponse<AccessProfile>>(`/api/v1/access-profiles/${id}`, body)).data),
  remove: async (id: number) => {
    await api.delete(`/api/v1/access-profiles/${id}`);
  },
};

export const accessPointsApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<AccessPoint>>>("/api/v1/access-points", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<AccessPoint>>(`/api/v1/access-points/${id}`)).data),
  create: async (body: AccessPointRequest) =>
    unwrap((await api.post<ApiResponse<AccessPoint>>("/api/v1/access-points", body)).data),
  update: async (id: number, body: AccessPointRequest) =>
    unwrap((await api.put<ApiResponse<AccessPoint>>(`/api/v1/access-points/${id}`, body)).data),
  remove: async (id: number) => {
    await api.delete(`/api/v1/access-points/${id}`);
  },
};

export const accessEventsApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<AccessEvent>>>("/api/v1/access-events", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<AccessEvent>>(`/api/v1/access-events/${id}`)).data),
  create: async (body: AccessEventRequest) =>
    unwrap((await api.post<ApiResponse<AccessEvent>>("/api/v1/access-events", body)).data),
};

export const alertsApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<Alert>>>("/api/v1/alerts", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<Alert>>(`/api/v1/alerts/${id}`)).data),
  acknowledge: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Alert>>(`/api/v1/alerts/${id}/acknowledge`)).data),
  resolve: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Alert>>(`/api/v1/alerts/${id}/resolve`)).data),
  close: async (id: number) =>
    unwrap((await api.patch<ApiResponse<Alert>>(`/api/v1/alerts/${id}/close`)).data),
};

export const importsApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<ImportBatch>>>("/api/v1/imports", { params: toParams(query) })).data),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<ImportBatch>>(`/api/v1/imports/${id}`)).data),
  report: async (id: number) =>
    unwrap((await api.get<ApiResponse<unknown>>(`/api/v1/imports/${id}/report`)).data),
  preview: async (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return unwrap(
      (await api.post<ApiResponse<ImportBatch>>("/api/v1/imports/preview", form, {
        headers: { "Content-Type": "multipart/form-data" },
      })).data,
    );
  },
  execute: async (batchId: number) =>
    unwrap((await api.post<ApiResponse<ImportBatch>>("/api/v1/imports/execute", { batchId })).data),
};

export const dashboardApi = {
  kpis: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<Kpi[]>>("/api/v1/dashboard/kpis", { params: toParams(params) })).data),
  charts: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<ChartDto[]>>("/api/v1/dashboard/charts", { params: toParams(params) })).data),
  statistics: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<Statistics>>("/api/v1/dashboard/statistics", { params: toParams(params) })).data),
  trends: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<Trend[]>>("/api/v1/dashboard/trends", { params: toParams(params) })).data),
  topAccessPoints: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<NamedCount[]>>("/api/v1/dashboard/top-access-points", { params: toParams(params) })).data),
  topZones: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<NamedCount[]>>("/api/v1/dashboard/top-zones", { params: toParams(params) })).data),
  topEmployees: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<NamedCount[]>>("/api/v1/dashboard/top-employees", { params: toParams(params) })).data),
  peakHours: async (params?: PageQuery) =>
    unwrap((await api.get<ApiResponse<NamedCount[]>>("/api/v1/dashboard/peak-hours", { params: toParams(params) })).data),
};

export const analyticsAiApi = {
  predict: async (event: Record<string, unknown>) =>
    unwrap((await aiApi.post<ApiResponse<AiPrediction>>("/api/v1/ai/predict", { event })).data),
  health: async () =>
    unwrap((await aiApi.get<ApiResponse<Record<string, unknown>>>("/api/v1/ai/health")).data),
};

export type FaceEnrollResult = { employeeId: number; enrolled: boolean; samplesCount: number };
export type FaceVerifyResult = {
  employeeId: number;
  matched: boolean;
  confidence: number;
  distance: number;
  message: string;
};

export const faceApi = {
  enroll: async (employeeId: number, imageBase64: string) =>
    unwrap(
      (
        await aiApi.post<ApiResponse<FaceEnrollResult>>(
          "/api/v1/ai/face/enroll",
          { employeeId, imageBase64 },
          { skipErrorToast: true },
        )
      ).data,
    ),
  verify: async (employeeId: number, imageBase64: string) =>
    unwrap(
      (
        await aiApi.post<ApiResponse<FaceVerifyResult>>(
          "/api/v1/ai/face/verify",
          { employeeId, imageBase64 },
          { skipErrorToast: true },
        )
      ).data,
    ),
};

export type CreateUserRequest = {
  username: string;
  email: string;
  password: string;
  role: Extract<RoleName, "ROLE_ADMIN" | "ROLE_ANALYST">;
};

export const usersApi = {
  list: async (query?: PageQuery) =>
    unwrap((await api.get<ApiResponse<PageResponse<User>>>("/api/v1/users", { params: toParams(query) })).data),
  create: async (body: CreateUserRequest) =>
    unwrap((await api.post<ApiResponse<User>>("/api/v1/users", body)).data),
  setEnabled: async (id: number, enabled: boolean) =>
    unwrap(
      (await api.patch<ApiResponse<User>>(`/api/v1/users/${id}/enabled`, undefined, { params: { enabled } }))
        .data,
    ),
};

export const employeePortalApi = {
  me: async () => unwrap((await api.get<ApiResponse<Employee>>("/api/v1/me/employee")).data),
  badges: async () => unwrap((await api.get<ApiResponse<Badge[]>>("/api/v1/me/badges")).data),
  alerts: async () => unwrap((await api.get<ApiResponse<Alert[]>>("/api/v1/me/alerts")).data),
  accessEvents: async (page = 0, size = 10) =>
    unwrap(
      (await api.get<ApiResponse<PageResponse<AccessEvent>>>("/api/v1/me/access-events", {
        params: { page, size },
      })).data,
    ),
  leaveRequests: async (page = 0, size = 10) =>
    unwrap(
      (await api.get<ApiResponse<PageResponse<LeaveRequest>>>("/api/v1/me/leave-requests", {
        params: { page, size },
      })).data,
    ),
  requestLeave: async (body: LeaveRequestCreate) =>
    unwrap((await api.post<ApiResponse<LeaveRequest>>("/api/v1/me/leave-requests", body)).data),
  cancelLeaveRequest: async (id: number) => {
    await api.delete(`/api/v1/me/leave-requests/${id}`);
  },
};

export type LeaveRequestSearchQuery = PageQuery & {
  employeeId?: number;
  departmentId?: number;
  status?: LeaveStatus;
  type?: LeaveType;
  from?: string;
  to?: string;
  keyword?: string;
};

export const leaveRequestsApi = {
  list: async (query?: LeaveRequestSearchQuery) =>
    unwrap(
      (await api.get<ApiResponse<PageResponse<LeaveRequest>>>("/api/v1/leave-requests", {
        params: toParams(query),
      })).data,
    ),
  get: async (id: number) =>
    unwrap((await api.get<ApiResponse<LeaveRequest>>(`/api/v1/leave-requests/${id}`)).data),
  approve: async (id: number, body?: LeaveRequestReview) =>
    unwrap((await api.patch<ApiResponse<LeaveRequest>>(`/api/v1/leave-requests/${id}/approve`, body ?? {})).data),
  reject: async (id: number, body?: LeaveRequestReview) =>
    unwrap((await api.patch<ApiResponse<LeaveRequest>>(`/api/v1/leave-requests/${id}/reject`, body ?? {})).data),
};
