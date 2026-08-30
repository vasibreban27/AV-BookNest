# Sistem de suport

Migrarea Flyway V20 adaugă tichete, mesaje și o coadă persistentă de emailuri.
V21 extinde numele de contact la 201 caractere, pentru numele complet al contului.
Nu modifica migrările deja aplicate. Repornește backend-ul pentru aplicarea migrărilor restante.
V20 păstrează checksum-ul original `-301408915`; extinderea coloanei se face doar în V21,
fără `repair`, ștergerea istoricului Flyway sau pierderea tichetelor existente.

## Flux și acces

- `POST /api/contact` rămâne public, cu CSRF, validare, consimțământ, honeypot și
  limitare de rată. Salvează tichetul, primul mesaj și emailul de notificare în aceeași
  tranzacție. Răspunsul include `message`, `reference` și `ticketId` (doar pentru conturi).
- Pentru conturi autentificate, identitatea și emailul se iau din utilizatorul curent.
  Vizitatorii nu sunt asociați automat după adresa introdusă și nu primesc acces public
  la conversații. Aceștia primesc răspunsurile administratorului pe email.
- `orderId` și `bookId` sunt opționale și necesită autentificare. Comanda trebuie să
  aparțină cumpărătorului sau să conțină un colet al vânzătorului. Anunțul trebuie să fie
  propriu, public vizibil sau legat de achiziția/coletul autorizat; dacă ambele referințe
  sunt furnizate, trebuie să corespundă aceluiași articol autorizat.
- Utilizatorii văd și pot răspunde numai la propriile tichete. Administratorii folosesc
  endpoint-urile administrative. Conturile suspendate sunt refuzate inclusiv la nivel
  de serviciu. Motivele administrative și repartizarea nu sunt expuse în răspunsurile
  publice ale tichetelor. Mesajele sunt text simplu, nemodificabile după trimitere.
- Stări: `NEW`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`. Răspunsul administratorului trece
  tichetul în lucru. Un răspuns al solicitantului redeschide un tichet rezolvat.
  Un tichet închis trebuie redeschis explicit de administrator în `IN_PROGRESS`.
  Schimbările administrative, răspunsurile și retry-urile sunt în jurnalul de audit.
  Motivele interne nu sunt incluse în mesajele de sistem sau emailuri.
- Mesajele au maximum 4000 de caractere. Răspunsurile utilizatorilor sunt limitate la
  30/oră/utilizator. Crearea folosește limita existentă de contact (implicit 5/oră/IP
  și email). Aceste limite în memorie sunt per instanță; pentru mai multe instanțe,
  adaugă o limitare comună la gateway/Redis.

## API

Toate listele folosesc `PageResponse`, pagini de la 0 și `size` între 1 și 100.
Mesajele se returnează de la cele mai recente, pentru afișare inversată în conversație.

| Metodă | Rută | Scop |
| --- | --- | --- |
| POST | `/api/contact` | Creează solicitare |
| GET | `/api/support/tickets?status=&page=0&size=20` | Lista proprie |
| GET | `/api/support/tickets/{id}` | Detalii proprii |
| GET / POST | `/api/support/tickets/{id}/messages` | Istoric / răspuns `{body}` |
| GET | `/api/admin/support/tickets?status=&q=&assignedToId=&unassigned=false` | Coada administrativă |
| GET | `/api/admin/support/administrators` | Administratori activi pentru repartizare |
| GET | `/api/admin/support/tickets/{id}` | Detalii și contact |
| GET / POST | `/api/admin/support/tickets/{id}/messages` | Istoric cu stare email / răspuns `{body}` |
| PATCH | `/api/admin/support/tickets/{id}/status` | `{status,reason}` |
| PATCH | `/api/admin/support/tickets/{id}/assignment` | `{administratorId,reason}`, ID null = nerepartizat |
| POST | `/api/admin/support/tickets/{id}/messages/{messageId}/retry-email` | Reintroduce email eșuat/dezactivat în coadă |

Rutele administrative necesită rolul `ADMIN`. Toate scrierile folosesc protecția CSRF.
Notificările în aplicație conduc la `/support/{id}` sau `/admin/support/{id}`.

## Email și operare

Se folosesc variabilele existente, configurate doar pe backend:

```dotenv
MAIL_ENABLED=true
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=suport@example.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
CONTACT_RECIPIENT=suport@example.com
APP_FRONTEND_URL=https://booknest.example.com
```

Nu salva parole în Git. Cu `MAIL_ENABLED=false`, tichetele și conversațiile funcționează,
dar emailurile sunt marcate `DISABLED`, nu trimise. După activare și repornire, acestea
pot fi reintroduse manual în coadă din administrare.

Procesorul verifică implicit la 30 secunde maximum 20 de emailuri. Rulează pe un
scheduler dedicat, astfel încât timpii de așteptare SMTP să nu blocheze procesarea
comenzilor, plăților sau livrărilor. Folosește o tranzacție separată pe email, cu blocare
și reverificare înainte de trimitere. Erorile SMTP nu
anulează salvarea tichetului. Sunt maximum 5 încercări automate, cu pauze progresive,
după care este necesară verificarea SMTP și reîncercarea manuală. Timeout-urile SMTP
sunt de 5 secunde pentru conectare/citire/scriere. Stările sunt `PENDING`, `SENT`,
`FAILED`, `DISABLED`. `SENT` înseamnă acceptare de către SMTP, nu confirmare de citire
sau livrare în inbox. Erorile afișate sunt generice, fără credențiale sau corpul emailului.

Trimiterea este de tip cel puțin o dată: dacă procesul se oprește după acceptarea SMTP,
dar înaintea commit-ului, emailul poate fi retrimis. Nu se pretinde livrare exact o dată.
Mesajele solicitantului notifică adresa echipei; răspunsurile și schimbările de stare
notifică solicitantul. Răspunsurile primite direct în căsuța de email nu sunt importate
automat: operatorul trebuie să verifice căsuța. Pentru conversație completă în aplicație,
utilizatorul trebuie să creeze solicitarea autentificat. Atașamentele și integrarea IMAP
sau webhook pentru emailuri primite nu sunt incluse în această etapă.

## Teste

```powershell
.\mvnw.cmd '-Dtest=Support*Test' test
.\scripts\test-reviews-postgres.ps1
.\mvnw.cmd spotless:check
```

Scriptul PostgreSQL existent rulează întreaga suită, inclusiv suportul, pe un cluster
temporar în `target/`, cu scheme separate și date sintetice; oprește clusterul la final.
Nu utilizează baza aplicației. Testele PostgreSQL sunt omise dacă nu este definit
`BOOKNEST_TEST_DATABASE_URL`; scriptul configurează variabila automat. Necesită Java 21+
și PostgreSQL local (implicit versiunea 18; parametrul `-PostgresBin` poate schimba calea).
