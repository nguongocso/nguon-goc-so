# AWS EC2 Deployment Guide

This guide explains how to provision an EC2 instance and deploy the
Nguồn Gốc Số stack (Spring Boot backend + React frontend + MySQL) using
Docker, Kubernetes (k3s), and the manifests in `k8s/`.

## 1. Architecture Overview

```
Internet
   │  (80/443)
   ▼
[ Ingress-Nginx Controller (LoadBalancer) ]
   │
   ▼
[ Ingress: nguongocso-ingress ]
   │  /              └── /api (proxied by frontend nginx)
   ▼
[ frontend-service ] ──[ frontend Deployment ]── nginx (SPA + /api proxy)
                                                       │
                                http://backend-service:8080
                                                       ▼
                                          [ backend-service ] ──[ backend Deployment ]── Spring Boot
                                                                           │
                                                                           ▼
                                                                      [ mysql-service ] ──[ mysql Stateful/Deployment ] ── MySQL
```

Key points:

- The frontend nginx reverse-proxies `/api` to the backend Service's internal
  DNS (`http://backend-service:8080`). The SPA reads its API base URL at runtime
  from `/config.js` (default `/api/v1`), so no rebuild is needed per environment.
- The backend reads all configuration from environment variables injected via
  a `ConfigMap` (non-secret) and a `Secret` (credentials).
- The Kubernetes manifests expect an external MySQL database such as Amazon RDS.
  This repository does not include an active self-hosted MySQL manifest.

## 2. EC2 Setup

### 2.1 Launch an instance

- AMI: Ubuntu 22.04 LTS (or 24.04)
- Instance type: `t3.medium` minimum (2 vCPU / 4 GB RAM) for a single-node k3s
  cluster running backend (x2 replicas), frontend (x2), and MySQL.
- Storage: 40 GB gp3 root volume (more if storing uploads locally).
- Security group: open
  - `22` (SSH) from your IP
  - `80` (HTTP) and `443` (HTTPS) from `0.0.0.0/0`
  - `6443` (k3s API) only if you need remote `kubectl` (recommended: restrict to your IP)

### 2.2 Install Docker (optional, used for image pull/build)

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io
sudo usermod -aG docker $USER
```

### 2.3 Install k3s (lightweight Kubernetes)

```bash
curl -sfL https://get.k3s.io | sh -
sudo chmod 644 /etc/rancher/k3s/k3s.yaml
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
kubectl get nodes
```

> For the GitHub Actions deploy step, base64-encode your kubeconfig and add it
> as the `KUBECONFIG_B64` repository/environment secret:
> `cat ~/.kube/config | base64 -w0`

### 2.4 Install the Ingress-Nginx controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/aws/deploy.yaml
kubectl -n ingress-nginx get svc ingress-nginx-controller
```

The controller creates an AWS LoadBalancer (ELB) exposing ports 80/443. Point
your DNS A record (`nguongocso.example.com`) to that ELB's DNS name.

### 2.5 (optional) Install cert-manager for TLS

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.15.3/cert-manager.yaml
```

Then create a `ClusterIssuer` and a `Certificate` in the `nguongocso` namespace
to auto-provision the `nguongocso-tls` secret referenced by `k8s/ingress.yaml`.
Alternatively, create the TLS secret manually:

```bash
kubectl -n nguongocso create secret tls nguongocso-tls \
  --cert=path/to/fullchain.pem \
  --key=path/to/privkey.pem
```

## 3. Configure Secrets

The `k8s/secrets.yaml` file contains **placeholder** base64 values only. Do not
apply it to a real environment. Create `backend-secrets` through a secret
manager or directly in the target namespace:

```bash
echo -n 'your_db_password'   | base64
echo -n 'your_jwt_secret'    | base64
echo -n 'your_mail_password' | base64
echo -n 'your_locationiq_key'| base64
```

> Recommendation: use AWS Secrets Manager + External Secrets Operator instead
> of committing base64 values to Git. The Kubernetes `Secret` approach above is
> a minimal starting point.

## 4. Deploy

### Option A: Automated (GitHub Actions)

Push to `develop` deploys staging. Production is started with
`workflow_dispatch` from `main`, a required SemVer `release_version`, and approval on the
GitHub `production` Environment. The `ci-cd.yml` workflow:

1. Tests the backend (H2 profile), then lints, tests, and builds the frontend.
2. Builds multi-stage Docker images and pushes them to GHCR.
3. Deploys commit-SHA images and runs rollout plus smoke checks.

Required repository secrets / GitHub environments:

| Secret | Environment | Purpose |
| --- | --- | --- |
| `KUBECONFIG_B64` | staging, production | base64-encoded kubeconfig |
| `KUBE_NAMESPACE` | staging, production | optional; defaults to environment name |
| `GITHUB_TOKEN` | automatic | push images to GHCR |

> Every deployment uses the immutable `github.sha` image tag. Mutable tags
> `develop`, `staging`, and `production` are convenience tags only.

### Option B: Manual

Create `backend-secrets` and `ghcr-secret` in the target namespace using your
secret manager or `kubectl`. Do not apply the placeholder values from
`k8s/secrets.yaml` to a real environment.

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml -n staging
kubectl apply -f k8s/persistent-volumes.yaml -n staging
kubectl apply -f k8s/backend-service.yaml -n staging
kubectl apply -f k8s/backend-deployment.yaml -n staging
kubectl apply -f k8s/frontend-service.yaml -n staging
kubectl apply -f k8s/frontend-deployment.yaml -n staging
kubectl apply -f k8s/ingress-staging.yaml -n staging
```

Verify:

```bash
kubectl -n staging get pods
kubectl -n staging rollout status deployment/backend
kubectl -n staging rollout status deployment/frontend
kubectl -n staging get ingress
```

## 5. Using Amazon RDS (recommended) or ECR/EKS

- **RDS**: Create a MySQL 8.x instance, note the endpoint, and update
  `k8s/configmap.yaml` `DB_HOST` to `your-rds-endpoint` and `DB_PORT` to `3306`.
- **ECR**: To push to Amazon ECR instead of GHCR, change the workflow's
  `docker/login-action` to use `aws-actions/amazon-ecr-login` and tag images with
  `${{ steps.login-ecr.outputs.registry }}/...`.
- **EKS**: The same `k8s/*.yaml` manifests work on EKS. Replace `k3s` setup with
  `eksctl create cluster` and configure an ALB ingress controller if desired.

## 6. Post-deployment checks

- `curl -I https://nguongocso.example.com` returns 200.
- `curl https://nguongocso.example.com/config.js` shows `API_BASE_URL: "/api/v1"`.
- Backend health: `kubectl -n nguongocso exec deploy/backend -- curl -s localhost:8080/actuator/health`.

## 7. Important security notes

- The default admin password seeded by Flyway (`V17__seed_default_admin.sql`) is
  `admin123` (a bcrypt hash in source). **Change it immediately** in production.
- Never commit real secrets. Use `.env` (gitignored) locally and Kubernetes
  `Secret`s / AWS Secrets Manager in deployed environments.
