# =============================================================================
# FlyPro Analytics – Frontend Dockerfile (multi-stage)
# Build Vite/React → Nginx static + reverse proxy
# =============================================================================

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci || npm install

COPY . .

# Empty API base → same-origin via Nginx (/api, /ai-api)
ARG VITE_API_BASE_URL=
ARG VITE_AI_BASE_URL=/ai-api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_AI_BASE_URL=$VITE_AI_BASE_URL

RUN npm run build

FROM nginx:1.27-alpine AS runtime

RUN apk add --no-cache curl

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=20s --timeout=5s --start-period=15s --retries=5 \
  CMD curl -fsS http://127.0.0.1/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
