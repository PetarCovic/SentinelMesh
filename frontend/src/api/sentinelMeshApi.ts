import { apiGet, apiPatch } from "./client";
import type { Alert, Device, SecurityEvent } from "../types";

export function getDevices(): Promise<Device[]>
{
    return apiGet<Device[]>("/api/devices");
}

export function getEvents(): Promise<SecurityEvent[]>
{
    return apiGet<SecurityEvent[]>("/api/events");
}

export function getAlerts(): Promise<Alert[]>
{
    return apiGet<Alert[]>("/api/alerts");
}

export function getOpenAlerts(): Promise<Alert[]>
{
    return apiGet<Alert[]>("api/alerts/open");
}

export function acknowledgeAlert(id: string): Promise<Alert>
{
    return apiPatch<Alert>(`/api/alerts/${id}/acknowledge`);
}

export function resolveAlert(id: string): Promise<Alert>
{
    return apiPatch<Alert>(`/api/alerts/${id}/resolve`);
}