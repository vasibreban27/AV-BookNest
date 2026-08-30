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

## Recenzii și reputație

- `/orders/:orderId`: cumpărătorul vede eligibilitatea fiecărei cărți și poate publica
  o recenzie după livrarea coletului. Notele pentru vânzător, descriere și starea cărții
  sunt obligatorii; comentariul are maximum 2000 de caractere. Formularele păstrează
  textul la erori, iar recenziile deja publicate nu pot fi duplicate sau editate.
- `/books/:bookId`: reputația vânzătorului și recenziile verificate, paginate.
- `/sales`: vânzătorul își vede reputația și feedbackul primit.
- `/admin/reviews`: căutare, filtrare și ascundere/restaurare cu motiv obligatoriu.
  Motivul este vizibil autorului. Recenziile ascunse nu intră în mediile publice;
  cele vechi, neverificate, nu pot fi publicate.

Frontend-ul necesită backend-ul cu migrarea V19. Nu sunt necesare variabile de mediu noi.
Cache-ul recenziilor private este separat pe utilizator; publicarea și moderarea
invalidează recenziile și reputația, iar moderarea reîmprospătează și auditul.

### Teste UI

```bash
npm test
npx playwright install chromium
npm run test:e2e:reviews
```

Testele Vitest verifică eligibilitatea, cele trei note, erorile, duplicatele ascunse,
reputația fără recenzii, paginarea și moderarea. Testele Playwright verifică paginile
reale la 320 px, 390 px și 1440 px, cu API complet simulat: nu folosesc conturi,
plăți sau comenzi reale. Serverul Vite dedicat pornește automat pe portul 4179.
Capturile și trace-urile sunt salvate în `test-results/` (ignorat de Git).

Pentru Edge deja instalat, fără descărcarea Chromium, în PowerShell:

```powershell
$env:PLAYWRIGHT_CHANNEL='msedge'
npm run test:e2e:reviews
```
