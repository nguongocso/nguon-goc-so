#!/bin/sh
# ============================================================
# Runtime configuration substitution for the frontend container
# ------------------------------------------------------------
# Replaces placeholders in /usr/share/nginx/html/config.js and
# renders /etc/nginx/conf.d/default.conf from its template using
# the following environment variables:
#   API_BASE_URL     -> exposed to the SPA (default: /api/v1, same-origin)
#   ASSET_BASE_URL   -> base URL for downloadable/static assets
#   BACKEND_UPSTREAM -> Nginx upstream for /api (default: http://backend:8080)
# ============================================================

set -eu

CONFIG_JS="/usr/share/nginx/html/config.js"
NGINX_TEMPLATE="/etc/nginx/templates/default.conf.template"
NGINX_CONF="/etc/nginx/conf.d/default.conf"

# ---- HTTP headers to avoid proxies caching stale config ----
API_BASE_URL="${API_BASE_URL:-/api/v1}"
ASSET_BASE_URL="${ASSET_BASE_URL:-${API_BASE_URL%/api/v1}}"
BACKEND_UPSTREAM="${BACKEND_UPSTREAM:-http://backend:8080}"

if [ -f "${CONFIG_JS}" ]; then
  # Use '|' as the sed delimiter since URLs contain '/'.
  sed -i \
    -e "s|__API_BASE_URL__|${API_BASE_URL}|g" \
    -e "s|__ASSET_BASE_URL__|${ASSET_BASE_URL}|g" \
    "${CONFIG_JS}"
  echo "[runtime-config] Wrote ${CONFIG_JS}"
  echo "  API_BASE_URL   = ${API_BASE_URL}"
  echo "  ASSET_BASE_URL = ${ASSET_BASE_URL}"
else
  echo "[runtime-config] WARN: ${CONFIG_JS} not found; skipping config.js substitution"
fi

if [ -f "${NGINX_TEMPLATE}" ]; then
  export BACKEND_UPSTREAM
  # Substitute ${BACKEND_UPSTREAM} (and any other ${VAR}) in the template.
  envsubst '${BACKEND_UPSTREAM}' < "${NGINX_TEMPLATE}" > "${NGINX_CONF}"
  echo "[runtime-config] Rendered ${NGINX_CONF} with BACKEND_UPSTREAM=${BACKEND_UPSTREAM}"
else
  echo "[runtime-config] WARN: ${NGINX_TEMPLATE} not found; keeping existing nginx conf"
fi