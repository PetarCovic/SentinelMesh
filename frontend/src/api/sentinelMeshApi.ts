import type {
  Alert,
  AlertRule,
  CreateAlertRuleRequest,
  Device,
  PageResponse,
  SecurityEvent,
  UpdateAlertRuleRequest,
} from "../types";

const API_BASE_URL = "http://localhost:8080";

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {}),
    },
  });

  if (!response.ok) {
    const errorText = await response.text();

    throw new Error(
      errorText || `Request failed with status ${response.status}`
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

/**
 * Devices
 */

export async function getDevices(): Promise<Device[]> {
  return request<Device[]>("/api/devices");
}

/**
 * Security Events
 */

export async function getRecentEvents(
  page = 0,
  size = 50
): Promise<PageResponse<SecurityEvent>> {
  return request<PageResponse<SecurityEvent>>(
    `/api/events/recent?page=${page}&size=${size}`
  );
}

/**
 * Alerts
 */

export async function getRecentAlerts(
  page = 0,
  size = 50
): Promise<PageResponse<Alert>> {
  return request<PageResponse<Alert>>(
    `/api/alerts/recent?page=${page}&size=${size}`
  );
}

export async function getOpenAlertsPaged(
  page = 0,
  size = 50
): Promise<PageResponse<Alert>> {
  return request<PageResponse<Alert>>(
    `/api/alerts/open?page=${page}&size=${size}`
  );
}

export async function acknowledgeAlert(id: string): Promise<Alert> {
  return request<Alert>(`/api/alerts/${id}/acknowledge`, {
    method: "PATCH",
  });
}

export async function resolveAlert(id: string): Promise<Alert> {
  return request<Alert>(`/api/alerts/${id}/resolve`, {
    method: "PATCH",
  });
}

/**
 * Alert Rules
 */

export async function getAlertRules(): Promise<AlertRule[]> {
  return request<AlertRule[]>("/api/rules");
}

export async function createAlertRule(
  payload: CreateAlertRuleRequest
): Promise<AlertRule> {
  return request<AlertRule>("/api/rules", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateAlertRule(
  id: string,
  payload: UpdateAlertRuleRequest
): Promise<AlertRule> {
  return request<AlertRule>(`/api/rules/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function deleteAlertRule(id: string): Promise<void> {
  return request<void>(`/api/rules/${id}`, {
    method: "DELETE",
  });
}

export async function enableAlertRule(id: string): Promise<AlertRule> {
  return request<AlertRule>(`/api/rules/${id}/enable`, {
    method: "PATCH",
  });
}

export async function disableAlertRule(id: string): Promise<AlertRule> {
  return request<AlertRule>(`/api/rules/${id}/disable`, {
    method: "PATCH",
  });
}