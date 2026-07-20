# BookNest backend

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
