# BookNest backend

## Configurare Cloudinary

Upload-ul coperților este făcut server-side, astfel încât secretul Cloudinary nu ajunge în
frontend. Adaugă următoarele variabile în configurația de rulare:

```text
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

Opțional, directorul poate fi schimbat cu `CLOUDINARY_FOLDER`; valoarea implicită este
`booknest/book-covers`. Sunt acceptate imagini JPEG, PNG și WebP de maximum 5 MB.

Fluxul API pentru o listare este:

1. `POST /api/books` cu metadatele JSON ale cărții;
2. `POST /api/books/{bookId}/cover` ca `multipart/form-data`, câmpul `file`;
3. `PUT /api/books/{bookId}` pentru modificarea metadatelor;
4. `DELETE /api/books/{bookId}/cover` pentru eliminarea coperții;
5. `PATCH /api/books/{bookId}/archive` și `PATCH /api/books/{bookId}/publish` pentru
   administrarea vizibilității.

Statusul și URL-ul coperții nu sunt acceptate în request-ul de creare/modificare. Backend-ul
setează statusul inițial `AVAILABLE`, iar URL-ul este preluat exclusiv din răspunsul Cloudinary.

## Date demo pentru dezvoltare

Datele de test sunt izolate în profilul Spring `dev`; profilurile obișnuite și producția nu sunt populate automat.

În configurația de rulare IntelliJ păstrează variabilele existente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` și `JWT_SECRET`) și adaugă:

```text
SPRING_PROFILES_ACTIVE=dev
```

La următoarea pornire, backend-ul creează sau actualizează idempotent:

- 5 utilizatori demo;
- 8 categorii;
- 18 cărți, dintre care 15 disponibile în catalog;
- un coș cu 2 cărți și un wishlist cu 3 cărți pentru Ana;
- 2 comenzi istorice, cu plăți și livrări în stări diferite;
- notificări demo pentru cumpărător și un vânzător.

### Conturi

| Rol | Email | Parolă implicită |
| --- | --- | --- |
| Cumpărător principal | `ana.popescu@booknest.local` | `BookNest123!` |
| Vânzător | `mihai.ionescu@booknest.local` | `BookNest123!` |
| Vânzător | `elena.marinescu@booknest.local` | `BookNest123!` |
| Vânzător | `radu.georgescu@booknest.local` | `BookNest123!` |
| Administrator | `admin@booknest.local` | `BookNest123!` |

Parola poate fi suprascrisă din configurația IntelliJ prin `APP_SEED_PASSWORD`. Seed-ul poate fi oprit fără dezactivarea profilului cu `APP_SEED_ENABLED=false`.

Datele standard din coș și wishlist sunt readăugate la fiecare pornire în profilul `dev` dacă lipsesc. Astfel, scenariul principal de test rămâne repetabil după un checkout.

## Configurare Sameday eAWB

Integrarea este dezactivată implicit, astfel încât lipsa credențialelor Sameday nu oprește
backend-ul. Pentru activare sunt necesare un contract Sameday business, acces eAWB/API și
serviciul de livrare Easybox activ în cont.

Contul business se solicită la `https://newcustomers.sameday.ro/register`. La ofertare trebuie
cerute explicit accesul API/eAWB, livrarea la easybox și predarea coletului de către vânzător la
easybox (first-mile), plus credențiale separate pentru mediul demo. Dacă serviciile nu apar
active în eAWB, activarea se cere responsabilului de cont sau suportului Sameday; nu poate fi
realizată din BookNest.

Variabile pentru mediul demo:

```text
SAMEDAY_ENABLED=true
SAMEDAY_BASE_URL=https://sameday-api.demo.zitec.com
SAMEDAY_USERNAME=...
SAMEDAY_PASSWORD=...
SAMEDAY_SERVICE_ID=...
SAMEDAY_PICKUP_POINT_ID=...
```

Pentru producție, `SAMEDAY_BASE_URL` devine `https://api.sameday.ro`, iar toate
credențialele și identificatoarele trebuie înlocuite cu valorile de producție. ID-urile
serviciilor și punctelor de ridicare sunt diferite între demo și producție.

După obținerea credențialelor, identificatorii se pot afla din API:

1. `POST /api/authenticate`, cu headerele `X-Auth-Username` și `X-Auth-Password`;
2. `GET /api/client/services`, cu tokenul în `X-AUTH-TOKEN`;
3. `GET /api/client/pickup-points`, cu același token.

Din lista serviciilor se alege serviciul contractual Sameday Basic/Easybox, iar din lista
punctelor de ridicare se alege punctul BookNest folosit pentru generarea AWB-urilor.
Nu salva credențialele în Git și nu le introduce în variabile `VITE_*`.

Fluxul aplicației folosește apoi:

- `GET /api/client/lockers` pentru alegerea Easybox-ului;
- `POST /api/awb/estimate-cost` pentru tariful contractual înainte de plată;
- `POST /api/awb` pentru AWB, prin procesarea evenimentului de integrare;
- `GET /api/client/status-sync` pentru livrare și eligibilitatea payout-ului Stripe la 24 de ore.

După înlocuirea sau ștergerea unei migrări încă neaplicate, rulează o dată:

```text
mvnw.cmd clean spring-boot:run
```

Comanda `clean` elimină migrările vechi rămase în `target/classes`.

### Sameday mock pentru dezvoltare

Checkout-ul poate fi testat fără contract sau credențiale Sameday:

```text
SAMEDAY_ENABLED=true
SAMEDAY_MOCK_ENABLED=true
```

În acest mod, backend-ul furnizează lockere de test, calculează un tarif fictiv și generează
AWB-uri cu prefixul `TEST-SD-`. Nu se face nicio cerere către Sameday și niciun colet real nu
este creat.

## Stripe sandbox și Connect

Implementarea acceptă exclusiv chei Stripe de test cât timp aplicația este în această etapă.
Backend-ul refuză cheile live pentru a preveni plățile reale accidentale.
Pentru acest flux tehnic nu ai nevoie acum de firmă și nu sunt mutați bani reali; verificarea
juridică și datele comerciale reale vor fi necesare abia înainte de activarea modului live.

1. Creează un cont Stripe și activează sandbox-ul din Dashboard.
2. Activează Connect pentru modelul marketplace.
3. Adaugă în configurația backend:

```text
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_CONNECT_RETURN_URL=http://localhost:5173/account?stripe=return
STRIPE_CONNECT_REFRESH_URL=http://localhost:5173/account?stripe=refresh
STRIPE_PAYMENT_RETURN_URL=http://localhost:5173/orders
```

Cheia `sk_test_` și secretul `whsec_` rămân exclusiv în backend. Nu le salva în Git și nu le
expune prin variabile `VITE_*`.

Pentru webhook local, instalează Stripe CLI, autentifică-te și rulează:

```text
stripe listen --forward-to localhost:8085/api/stripe/webhook
```

Valoarea `whsec_...` afișată de comandă devine `STRIPE_WEBHOOK_SECRET`. Evenimentele folosite
sunt `payment_intent.succeeded`, `payment_intent.payment_failed`, `payment_intent.canceled`,
`account.updated` și `transfer.reversed`.

Fluxul de test:

1. vânzătorul deschide pagina **Contul meu** și finalizează onboarding-ul Stripe sandbox;
2. cumpărătorul alege un Easybox mock și primește tariful de test;
3. checkout-ul creează comanda rezervată și un `PaymentIntent`;
4. Payment Element acceptă cardul Stripe `4242 4242 4242 4242`, o dată viitoare și orice CVC;
5. numai webhook-ul de succes activează comanda și termenul de acceptare de 24 de ore;
6. o plată nefinalizată expiră după 30 de minute și eliberează cărțile;
7. după livrare și încă 24 de ore, suma de 95% este transferată în soldul Stripe sandbox al
   vânzătorului.
