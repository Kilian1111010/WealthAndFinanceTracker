# Frontend

React + TypeScript + Vite.

## Entwicklung

1. Backend mit Profil `dev` starten (läuft auf Port 8080).
2. Im Ordner `frontend`:

   ```bash
   npm install
   npm run dev
   ```

3. http://localhost:5173 öffnen.

Der Vite-Dev-Server leitet `/auth` und `/api` an das Backend weiter, Frontend und Backend laufen
aus Sicht des Browsers also unter derselben Adresse. Session-Cookie und CSRF-Token funktionieren
dadurch ohne CORS-Konfiguration.

## Befehle

| Befehl            | Zweck                                  |
|-------------------|----------------------------------------|
| `npm run dev`     | Dev-Server mit Hot Reload              |
| `npm run build`   | Typprüfung und Produktions-Build       |
| `npm run lint`    | Linting mit oxlint                     |
| `npm run preview` | Produktions-Build lokal ansehen        |
