/**
 * HTTP client for profile operations: GET to fetch, PUT to save, POST /import to upload and
 * extract résumé via server-side LLM call.
 */
import type { ProfileView } from "../types/profile";

const BASE_URL = "/api/profile";

async function unwrap(response: Response): Promise<ProfileView> {
  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(body.message ?? `Request failed with status ${response.status}`);
  }
  return response.json();
}

export function getProfile(): Promise<ProfileView> {
  return fetch(BASE_URL).then(unwrap);
}

export function saveProfile(view: ProfileView): Promise<ProfileView> {
  return fetch(BASE_URL, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(view),
  }).then(unwrap);
}

export function importResume(file: File): Promise<ProfileView> {
  const formData = new FormData();
  formData.append("file", file);
  return fetch(`${BASE_URL}/import`, { method: "POST", body: formData }).then(unwrap);
}
