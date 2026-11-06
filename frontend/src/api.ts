// Typed client for the LoginGuard API. The session lives in an HttpOnly cookie that this code
// never sees; the only thing it reads is the CSRF token, which it echoes in a header so the
// server can tell a request made by this app from a forged cross-site one.

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type ReviewStatus = "PENDING" | "CONFIRMED_FRAUD" | "LEGITIMATE";
export type Role = "ADMIN" | "ANALYST";

export interface Me {
  username: string;
  role: Role;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface MonitoredUser {
  id: number;
  username: string;
  email: string;
  department: string | null;
  createdAt: string;
}

export interface RiskFactor {
  feature: string;
  label: string;
  value: number;
}

export interface LoginEvent {
  id: number;
  userId: number;
  username: string;
  occurredAt: string;
  ipAddress: string;
  countryCode: string;
  deviceId: string;
  successful: boolean;
  risk: { score: number; level: RiskLevel; modelVersion: string; factors: RiskFactor[] } | null;
  reviewStatus: ReviewStatus;
  reviewedBy: string | null;
  reviewedAt: string | null;
}

export interface Stats {
  total: number;
  byRiskLevel: Record<RiskLevel, number>;
  byReviewStatus: Record<ReviewStatus, number>;
  pendingHighRisk: number;
}

export interface RiskModel {
  version: string;
  algorithm: string;
  syntheticTrainingData: boolean;
  features: { name: string; label: string; coefficient: number }[];
  thresholds: { medium: number; high: number };
  metrics: Record<string, number>;
  training: Record<string, string | number | boolean>;
}

export interface Operator {
  id: number;
  username: string;
  role: Role;
  enabled: boolean;
  lockedUntil: string | null;
  createdAt: string;
}

/** An error answered by the API as an RFC 9457 problem detail. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly fieldErrors: { field: string; message: string }[] = [],
  ) {
    super(message);
  }
}

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

export function readCsrfToken(cookies: string = document.cookie): string | undefined {
  const entry = cookies.split("; ").find((cookie) => cookie.startsWith(`${CSRF_COOKIE}=`));
  return entry ? decodeURIComponent(entry.slice(CSRF_COOKIE.length + 1)) : undefined;
}

/** Called when any request comes back 401, so the app can return to the login page. */
let onUnauthorized: () => void = () => {};
export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (method !== "GET") {
    const token = readCsrfToken();
    if (token) {
      headers[CSRF_HEADER] = token;
    }
  }
  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: "same-origin",
  });
  if (response.status === 401 && path !== "/api/auth/login") {
    onUnauthorized();
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(response.status, problem.detail ?? response.statusText, problem.errors ?? []);
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

function query(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}

export const api = {
  // Loads the CSRF cookie: needed before the very first POST, the login itself
  csrf: () => request<unknown>("GET", "/api/auth/csrf"),
  login: (username: string, password: string) => request<Me>("POST", "/api/auth/login", { username, password }),
  logout: () => request<void>("POST", "/api/auth/logout"),
  me: () => request<Me>("GET", "/api/auth/me"),

  stats: () => request<Stats>("GET", "/api/stats"),
  model: () => request<RiskModel>("GET", "/api/model"),

  events: (filters: { status?: ReviewStatus | ""; level?: RiskLevel | ""; page?: number }) =>
    request<Page<LoginEvent>>(
      "GET",
      `/api/login-events${query({ status: filters.status, level: filters.level, page: filters.page })}`,
    ),
  event: (id: number) => request<LoginEvent>("GET", `/api/login-events/${id}`),
  recordEvent: (event: {
    userId: number;
    occurredAt: string;
    ipAddress: string;
    countryCode: string;
    deviceId: string;
    successful: boolean;
  }) => request<LoginEvent>("POST", "/api/login-events", event),
  reviewEvent: (id: number, status: "CONFIRMED_FRAUD" | "LEGITIMATE") =>
    request<LoginEvent>("PUT", `/api/login-events/${id}/review`, { status }),
  deleteEvent: (id: number) => request<void>("DELETE", `/api/login-events/${id}`),

  users: (page = 0) => request<Page<MonitoredUser>>("GET", `/api/users${query({ page, size: 100 })}`),
  createUser: (user: { username: string; email: string; department: string }) =>
    request<MonitoredUser>("POST", "/api/users", user),
  updateUser: (id: number, user: { username: string; email: string; department: string }) =>
    request<MonitoredUser>("PUT", `/api/users/${id}`, user),
  deleteUser: (id: number) => request<void>("DELETE", `/api/users/${id}`),

  operators: () => request<Operator[]>("GET", "/api/operators"),
  createOperator: (operator: { username: string; password: string; role: Role }) =>
    request<Operator>("POST", "/api/operators", operator),
  deleteOperator: (id: number) => request<void>("DELETE", `/api/operators/${id}`),
};
