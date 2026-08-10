# syntax=docker/dockerfile:1

# Vite/React geliştirme imajı (bkz. docker-compose.yml -> frontend profili).
FROM node:22-alpine
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
