export type ApiResponse<T> = {
  success: boolean;
  message?: string | null;
  data?: T | null;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type ErrorResponse = {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[] | null;
};

export type RoleName = "ROLE_SUPER_ADMIN" | "ROLE_ADMIN" | "ROLE_ANALYST" | "ROLE_EMPLOYEE";

export type User = {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  accountLocked: boolean;
  role: RoleName;
  employeeId?: number | null;
  createdAt?: string;
  updatedAt?: string;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
};

export type RefreshResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export type Department = {
  id: number;
  name: string;
  code: string;
  description?: string | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type DepartmentRequest = {
  name: string;
  code: string;
  description?: string;
};

export type EmployeeStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "ARCHIVED";

export type Employee = {
  id: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  position: string;
  status: EmployeeStatus;
  department: Pick<Department, "id" | "name" | "code" | "active">;
  accessProfileId?: number | null;
  createdAt?: string;
  updatedAt?: string;
  portalUsername?: string | null;
  temporaryPassword?: string | null;
};

export type EmployeeRequest = {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  position: string;
  status: EmployeeStatus;
  departmentId: number;
  accessProfileId?: number | null;
};

export type BadgeType = "RFID" | "NFC" | "MAGNETIC" | "QR_CODE" | "OTHER";
export type BadgeStatus = "ACTIVE" | "SUSPENDED" | "EXPIRED" | "REVOKED" | "LOST" | "PENDING_ACTIVATION";

export type BadgeAssignment = {
  id: number;
  badgeId: number;
  badgeNumber: string;
  employeeId: number;
  employeeNumber: string;
  employeeFirstName: string;
  employeeLastName: string;
  assignedAt: string;
  returnedAt?: string | null;
  active: boolean;
};

export type Badge = {
  id: number;
  badgeNumber: string;
  type: BadgeType;
  status: BadgeStatus;
  issueDate: string;
  expirationDate?: string | null;
  currentAssignment?: BadgeAssignment | null;
  createdAt?: string;
  updatedAt?: string;
};

export type BadgeRequest = {
  badgeNumber: string;
  type: BadgeType;
  status: BadgeStatus;
  issueDate: string;
  expirationDate?: string;
};

export type Zone = {
  id: number;
  name: string;
  building: string;
  floor: string;
  sensitive: boolean;
  description?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type ZoneRequest = {
  name: string;
  building: string;
  floor: string;
  sensitive: boolean;
  description?: string;
};

export type ZoneSummary = {
  id: number;
  name: string;
  building: string;
  floor: string;
  sensitive: boolean;
};

export type SecurityLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type AccessProfile = {
  id: number;
  name: string;
  description?: string | null;
  securityLevel: SecurityLevel;
  startTime: string;
  endTime: string;
  zones: ZoneSummary[];
  createdAt?: string;
  updatedAt?: string;
};

export type AccessProfileRequest = {
  name: string;
  description?: string;
  securityLevel: SecurityLevel;
  startTime: string;
  endTime: string;
  zoneIds: number[];
};

export type AccessPointType = "DOOR" | "TURNSTILE" | "GATE" | "MOBILE_READER" | "VIRTUAL";
export type AccessPointStatus = "ACTIVE" | "INACTIVE" | "MAINTENANCE" | "OUT_OF_SERVICE";

export type AccessPoint = {
  id: number;
  name: string;
  code: string;
  type: AccessPointType;
  description?: string | null;
  status: AccessPointStatus;
  ipAddress?: string | null;
  location?: string | null;
  zone: ZoneSummary;
  createdAt?: string;
  updatedAt?: string;
};

export type AccessPointRequest = {
  name: string;
  code: string;
  type: AccessPointType;
  description?: string;
  status: AccessPointStatus;
  ipAddress?: string;
  location?: string;
  zoneId: number;
};

export type AccessDirection = "ENTRY" | "EXIT" | "NEUTRAL";
export type AccessResult = "AUTHORIZED" | "DENIED" | "EXPIRED" | "UNKNOWN" | "TECHNICAL_ERROR";
export type EventSource = "API" | "CSV" | "JSON" | "EXCEL" | "SIMULATOR" | "MANUAL";

export type AccessEvent = {
  id: number;
  eventTimestamp: string;
  direction: AccessDirection;
  result: AccessResult;
  refusalReason?: string | null;
  source: EventSource;
  rawPayload?: string | null;
  badgeId: number;
  badgeNumber?: string;
  employeeId?: number;
  employeeNumber?: string;
  employeeFullName?: string;
  accessPointId: number;
  accessPointName?: string;
  zone?: ZoneSummary | null;
  createdAt?: string;
};

export type AccessEventRequest = {
  eventTimestamp: string;
  direction: AccessDirection;
  result: AccessResult;
  refusalReason?: string;
  source: EventSource;
  rawPayload?: string;
  badgeId: number;
  accessPointId: number;
  zoneId?: number;
};

export type AlertSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type AlertStatus = "OPEN" | "ACKNOWLEDGED" | "RESOLVED" | "CLOSED";
export type AlertType =
  | "BADGE_EXPIRED"
  | "BADGE_REVOKED"
  | "BADGE_SUSPENDED"
  | "ACCESS_DENIED"
  | "MULTIPLE_ACCESS_DENIED"
  | "OUTSIDE_WORKING_HOURS"
  | "UNAUTHORIZED_ZONE"
  | "UNKNOWN_BADGE"
  | "INACTIVE_EMPLOYEE"
  | "SYSTEM_WARNING";

export type Alert = {
  id: number;
  title: string;
  description?: string | null;
  severity: AlertSeverity;
  status: AlertStatus;
  type: AlertType;
  createdDate: string;
  resolvedDate?: string | null;
  resolvedBy?: string | null;
  employeeId?: number | null;
  employeeNumber?: string | null;
  employeeFullName?: string | null;
  badgeId?: number | null;
  badgeNumber?: string | null;
  accessEventId?: number | null;
  zoneId?: number | null;
  zoneName?: string | null;
  departmentId?: number | null;
  createdAt?: string;
};

export type LeaveType = "ANNUAL" | "SICK" | "UNPAID" | "OTHER";
export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type LeaveRequest = {
  id: number;
  employeeId: number;
  employeeNumber: string;
  employeeFirstName: string;
  employeeLastName: string;
  departmentId?: number | null;
  departmentName?: string | null;
  startDate: string;
  endDate: string;
  type: LeaveType;
  reason?: string | null;
  status: LeaveStatus;
  reviewComment?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  createdAt?: string;
};

export type LeaveRequestCreate = {
  startDate: string;
  endDate: string;
  type: LeaveType;
  reason?: string;
};

export type LeaveRequestReview = {
  comment?: string;
};

export type ImportStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "PARTIALLY_COMPLETED";
export type ImportFileType = "CSV" | "XLSX" | "JSON";

export type ImportIssue = {
  id: number;
  lineNumber?: number | null;
  fieldName?: string | null;
  message: string;
  severity?: string | null;
};

export type ImportBatch = {
  id: number;
  fileName: string;
  fileType: ImportFileType;
  status: ImportStatus;
  totalRecords: number;
  validRecords: number;
  invalidRecords: number;
  duplicateRecords: number;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt?: string;
  createdBy?: string | null;
  issues?: ImportIssue[];
};

export type Kpi = { key: string; label: string; value: number; unit: string };
export type ChartSeries = { name: string; data: number[] };
export type ChartDto = { key: string; title: string; labels: string[]; series: ChartSeries[] };
export type NamedCount = { id?: number | null; name: string; count: number };
export type Trend = {
  key: string;
  label: string;
  currentValue: number;
  previousValue: number;
  changePercent: number;
};
export type Statistics = {
  topAccessPoints: NamedCount[];
  topZones: NamedCount[];
  mostActiveEmployees: NamedCount[];
  employeesWithDeniedAccesses: NamedCount[];
  badgesExpiringSoon: NamedCount[];
  departmentsWithMostAccesses: NamedCount[];
  mostFrequentAlertTypes: NamedCount[];
};

export type AiPrediction = {
  prediction: "NORMAL" | "ANOMALY";
  riskScore: number;
  confidence: number;
  reason: string;
  recommendedAction: string;
  modelVersion?: string | null;
  ruleFlags: string[];
  anomalyScore?: number | null;
};

export type PageQuery = {
  page?: number;
  size?: number;
  sort?: string;
  [key: string]: string | number | boolean | undefined | null;
};
