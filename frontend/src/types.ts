export type DeviceType=
    | "CAMERA"
    | "DOOR_SENSOR"
    | "WINDOW_SENSOR"
    | "MOTION_SENSOR"
    | "TEMPERATURE_SENSOR"
    | "SOUND_SENSOR";

export type DeviceStatus="ONLINE" | "OFFLINE";

export type SecurityEventType=
    | "MOTION_DETECTED"
    | "PERSON_DETECTED"
    | "DOOR_OPENED"
    | "DOOR_CLOSED"
    | "WINDOW_OPENED"
    | "WINDOW_CLOSED"
    | "SOUND_DETECTED"
    | "TEMPERATURE_ALERT"
    | "DEVICE_TAMPERED"
    | "UNKNOWN";

export type SecurityEventSeverity= "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type AlertSeverity= "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type AlertStatus= "OPEN" | "ACKNOWLEDGED" | "RESOLVED";

export interface Device
{
    id: string;
    name: string;
    type: DeviceType;
    location: string;
    status: DeviceStatus;
    lastSeenAt: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface SecurityEvent
{
    id: string;
    deviceId: string;
    deviceName: string;
    eventType: SecurityEventType;
    severity: SecurityEventSeverity;
    confidence: number | null;
    occurredAt: string;
    receivedAt: string;
    metadataJson: string | null;
}

export interface Alert
{
    id: string;
    securityEventId: string;
    deviceId: string;
    deviceName: string;
    severity: AlertSeverity;
    status: AlertStatus;
    title: string;
    message: string;
    createdAt: string;
    acknowledgedAt: string | null;
    resolvedAt: string | null;
}