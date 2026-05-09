# Notification Multichannel Frontend

Frontend independente para operar o backend Spring Boot `notification-multichannel-main`.

## Requisitos

- Node.js 20+
- Backend rodando em `http://localhost:8080`

## Como rodar

```bash
npm install
npm run dev
```

Acesse:

```text
http://localhost:5173
```

O Vite esta configurado com proxy:

```text
/v1 -> http://localhost:8080
```

Assim o front chama `/v1/notifications` sem precisar alterar CORS no backend.
