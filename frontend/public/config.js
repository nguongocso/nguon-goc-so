// ============================================================
// Runtime SPA configuration
// ------------------------------------------------------------
// This file is loaded BEFORE the application bundle (see index.html).
// The placeholder tokens below are replaced at container startup by:
//   docker-entrypoint.d/40-runtime-config.sh
// which reads the API_BASE_URL / ASSET_BASE_URL environment variables.
//
// This keeps the API base URL configurable at runtime and avoids
// baking environment-specific URLs into the JS bundle.
// ============================================================
window.__RUNTIME_CONFIG__ = {
  // Base URL used by the Axios client. Defaults to same-origin "/api/v1"
  // which Nginx reverse-proxies to the backend service.
  API_BASE_URL: "__API_BASE_URL__",
  // Base URL for downloadable/static assets (e.g. QR code images).
  ASSET_BASE_URL: "__ASSET_BASE_URL__",
};