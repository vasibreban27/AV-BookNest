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

În development, cererile către `/api` sunt redirecționate de Vite către `http://localhost:8085`. Pentru un backend găzduit separat, copiază `.env.example` ca `.env` și schimbă `VITE_API_URL`.

Fișierul local `frontend/.env` este ignorat de Git. Variabilele frontend care încep cu `VITE_` sunt incluse în bundle-ul trimis browserului și nu trebuie să conțină parole, tokenuri sau alte secrete. Variabilele backend rămân configurate separat în mediul de rulare IntelliJ.

## Rute

- `/login` — autentificare;
- `/register` — creare cont;
- `/account` — rută protejată și confirmarea sesiunii active.

Sesiunea folosește access token-ul primit de backend și reînnoiește automat token-urile prin `/api/auth/refresh` atunci când o cerere protejată răspunde cu `401`.

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
│       ├── storage/        # persistența tokenurilor
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
