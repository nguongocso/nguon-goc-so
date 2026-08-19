// ============================================================
// Runtime configuration resolver
// ------------------------------------------------------------
// Resolves environment-specific values with the following priority:
//   1. window.__RUNTIME_CONFIG__ (populated by /config.js at runtime)
//   2. Vite build-time env vars (import.meta.env.*)
//   3. Same-origin defaults (Nginx reverse-proxies /api -> backend)
//
// This keeps the API base URL configurable at runtime per environment
// without rebuilding the frontend image.
// ============================================================

interface RuntimeConfig {
  API_BASE_URL: string;
  ASSET_BASE_URL: string;
}

declare global {
  interface Window {
    __RUNTIME_CONFIG__?: Partial<RuntimeConfig>;
  }
}

/** Returns the configured value if it is a real value (non-empty, not a placeholder). */
function isConfigured(value: string | undefined): value is string {
  return (
    !!value &&
    value.trim().length > 0 &&
    !value.startsWith("__") &&
    !value.endsWith("__")
  );
}

/** Resolve the API base URL used by the Axios client (expected to end with /api/v1). */
export function getApiBaseUrl(): string {
  const fromWindow = window.__RUNTIME_CONFIG__?.API_BASE_URL;
  if (isConfigured(fromWindow)) {
    return normalizeApiBaseUrl(fromWindow);
  }

  const fromVite =
    import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL;
  if (isConfigured(fromVite)) {
    return normalizeApiBaseUrl(fromVite);
  }

  // Same-origin default: Nginx proxies /api -> backend service.
  return normalizeApiBaseUrl("/api/v1");
}

/** Resolve the base URL for downloadable/static assets (e.g. QR code images). */
export function getAssetBaseUrl(): string {
  const fromWindow = window.__RUNTIME_CONFIG__?.ASSET_BASE_URL;
  if (isConfigured(fromWindow)) {
    return fromWindow.replace(/\/$/, "");
  }

  const fromVite = import.meta.env.VITE_ASSET_BASE_URL;
  if (isConfigured(fromVite)) {
    return fromVite.replace(/\/$/, "");
  }

  // Derive from the API URL: strip a trailing /api/v1 (or /api) suffix.
  const apiBase = getApiBaseUrl();
  return apiBase.replace(/\/api(?:\/v1)?\/?$/, "").replace(/\/$/, "");
}

/** Ensure the value ends with /api/v1. */
function normalizeApiBaseUrl(raw: string): string {
  const value = raw.trim();
  if (/\/api\/v1\/?$/.test(value)) {
    return value.replace(/\/$/, "");
  }
  // If a shorter /api suffix is present, extend it to /api/v1.
  if (/\/api\/?$/.test(value)) {
    return value.replace(/\/+$/, "") + "/v1";
  }
  // Otherwise treat it as the origin root and append /api/v1.
  return value.replace(/\/+$/, "") + "/api/v1";
}