# =========================
# Stage 1: Build frontend
# =========================
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files trước để tận dụng Docker cache
COPY package*.json ./

# Cài dependencies đúng theo package-lock.json
RUN npm ci

# Copy source code
COPY . .

# Nhận biến môi trường Vite khi docker build
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Build production
RUN npm run build


# =========================
# Stage 2: Serve with Nginx
# =========================
FROM nginx:alpine

# Cấu hình Nginx cho React SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy kết quả build từ stage builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx chạy cổng 80
EXPOSE 80

# Chạy Nginx foreground
CMD ["nginx", "-g", "daemon off;"]