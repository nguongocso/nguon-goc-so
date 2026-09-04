# ============================================================
# Stage 1: Build frontend
# ============================================================
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files first to leverage Docker layer caching.
COPY package.json package-lock.json ./

# Install dependencies exactly as pinned in package-lock.json.
RUN npm ci

# Copy source code.
COPY . .

# The API base URL is resolved at RUNTIME (via /config.js), so the
# build-time Vite vars are intentionally left unset. Nginx will
# reverse-proxy /api -> backend service. This produces a single
# environment-agnostic image.
RUN npm run build


# ============================================================
# Stage 2: Serve with Nginx
# ============================================================
FROM nginx:1.27-alpine

# TC-04: OCI labels — Docker image → Git commit traceability.
# CI passes --build-arg GIT_COMMIT=${{ github.sha }}; default for local builds.
ARG GIT_COMMIT=local
LABEL org.opencontainers.image.revision=${GIT_COMMIT}
# CI passes --build-arg RELEASE_VERSION; default for local builds.
ARG RELEASE_VERSION=1.0.0
LABEL org.opencontainers.image.version=${RELEASE_VERSION}

# Install envsubst (provided by gettext) plus a POSIX shell.
# envsubst substitutes ${BACKEND_UPSTREAM} in the nginx template.
RUN apk add --no-cache gettext

# Copy the nginx template; it is rendered at startup by the entrypoint.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Copy the runtime config placeholder; it is populated at startup.
COPY public/config.js /usr/share/nginx/html/config.js

# Copy build output from the builder stage.
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy our runtime substitution script into the official entrypoint dir.
# The nginx image's docker-entrypoint.sh auto-executes *.sh here,
# then (after 10-envsubst-on-templates.sh) renders templates and starts nginx.
COPY docker-entrypoint.d/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
RUN chmod +x /docker-entrypoint.d/40-runtime-config.sh

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]