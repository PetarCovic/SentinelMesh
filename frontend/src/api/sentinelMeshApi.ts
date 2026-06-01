import { apiGet, apiPatch } from "./client";
import type { Alert, Device, PageResponse, SecurityEvent } from "../types";

export function getDevices(): Promise<Device[]>
{
    return apiGet<Device[]>("/api/devices");
}

export function acknowledgeAlert(id: string): Promise<Alert>
{
    return apiPatch<Alert>(`/api/alerts/${id}/acknowledge`);
}

export function resolveAlert(id: string): Promise<Alert>
{
    return apiPatch<Alert>(`/api/alerts/${id}/resolve`);
}

export function getRecentEvents(
  page: number,
  size = 50
): Promise<PageResponse<SecurityEvent>> {
  return apiGet<PageResponse<SecurityEvent>>(
    `/api/events/recent?page=${page}&size=${size}`
  );
}

export function getRecentAlerts(
  page: number,
  size = 50
): Promise<PageResponse<Alert>> {
  return apiGet<PageResponse<Alert>>(
    `/api/alerts/recent?page=${page}&size=${size}`
  );
}

export function getOpenAlertsPaged(
  page: number,
  size = 50
): Promise<PageResponse<Alert>> {
  return apiGet<PageResponse<Alert>>(
    `/api/alerts/open?page=${page}&size=${size}`
  );
}