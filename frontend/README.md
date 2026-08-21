# BookNest frontend

Interfață React + TypeScript pentru aplicația BookNest. Fluxul de autentificare este conectat la API-ul Spring Boot din folderul `backend`.

## Rulare locală

Backend-ul rulează implicit pe portul `8085`, iar Vite pe `5173`:

```bash
# din backend
./mvnw spring-boot:run

# din frontend, într-un alt terminal
npm install
npm run dev
```

În development, cererile către `/api` sunt redirecționate de Vite către `http://localhost:8085`.
Păstrarea valorii `VITE_API_URL=/api` reproduce configurația recomandată din producție, unde
un reverse proxy publică frontend-ul și API-ul pe aceeași origine.

Fișierul local `frontend/.env` este ignorat de Git. Variabilele frontend care încep cu `VITE_` sunt incluse în bundle-ul trimis browserului și nu trebuie să conțină parole, tokenuri sau alte secrete. Variabilele backend rămân configurate separat în mediul de rulare IntelliJ.

## Rute

- `/login` — autentificare;
- `/register` — creare cont;
- `/account` — rută protejată și confirmarea sesiunii active.

Sesiunea folosește cookie-uri JWT `HttpOnly`, trimise automat cu `withCredentials`. Tokenurile
nu sunt accesibile codului JavaScript și nu sunt salvate în `localStorage`. Frontend-ul obține
cookie-ul CSRF de la `/api/auth/csrf`, iar Axios trimite automat header-ul `X-XSRF-TOKEN`.
Access token-ul este reînnoit prin `/api/auth/refresh` atunci când o cerere protejată răspunde
cu `401`.

## Structură

```text
src/
├── api/                    # infrastructură HTTP comună
├── components/
│   ├── auth/               # componente reutilizabile pentru formularele auth
│   └── common/             # logo și iconuri comune
├── features/
│   └── auth/
│       ├── api/            # apelurile către endpoint-urile auth
│       ├── context/        # provider-ul sesiunii
│       ├── hooks/          # accesul la contextul auth
│       ├── schemas/        # validările Zod
│       ├── events/         # sincronizarea expirării sesiunii în interfață
│       ├── types/          # toate tipurile feature-ului
│       └── utils/          # maparea erorilor API/formular
├── pages/
│   ├── account/
│   └── auth/
│       ├── login/
│       └── register/
├── routes/                 # configurarea și protecția rutelor
└── styles/
    ├── base/
    ├── components/
    └── pages/
```

## Verificare

```bash
npm run lint
npm run build
```
