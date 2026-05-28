const API_BASE_URL = "http://localhost:8080";

function buildUrl(path: string): string {
  return `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(buildUrl(path));

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`GET ${path} failed with status ${response.status}: ${text}`);
  }

  return response.json() as Promise<T>;
}

export async function apiPatch<T>(path: string): Promise<T> {
  const response = await fetch(buildUrl(path), {
    method: "PATCH",
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`PATCH ${path} failed with status ${response.status}: ${text}`);
  }

  return response.json() as Promise<T>;
}