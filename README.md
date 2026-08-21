<div align="center">
  <img src="https://raw.githubusercontent.com/vasibreban27/AV-BookNest/main/frontend/public/favicon.svg" alt="AV's BookNest logo" width="86" />

  <h1>AV's BookNest</h1>

  <p><strong>A modern full-stack marketplace for discovering, buying and selling books.</strong></p>

  <p>
    <a href="#-about-the-project">About</a> •
    <a href="#-features">Features</a> •
    <a href="#-tech-stack">Tech stack</a> •
    <a href="#-getting-started">Getting started</a> •
    <a href="#-roadmap">Roadmap</a>
  </p>

  <p>
    <img alt="Status: in development" src="https://img.shields.io/badge/status-in%20development-F59E0B?style=for-the-badge" />
    <img alt="Java 21" src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
    <img alt="Spring Boot 3.5" src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  </p>
  <p>
    <img alt="React 19" src="https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=111827" />
    <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-6-3178C6?style=flat-square&logo=typescript&logoColor=white" />
    <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-database-4169E1?style=flat-square&logo=postgresql&logoColor=white" />
    <img alt="Stripe" src="https://img.shields.io/badge/Stripe-sandbox-635BFF?style=flat-square&logo=stripe&logoColor=white" />
  </p>

  <p><em>🚧 The application is under active development. Core marketplace flows are implemented, while production integrations, deployment and UI refinements are still in progress.</em></p>
</div>

---

## 📖 About the project

**AV's BookNest** is a personal full-stack project built as an online marketplace for books. It brings buyers and independent sellers into the same application: users can publish books, explore the catalog, save favorites, add items to their cart and place online orders.

The project uses a monorepo structure:

| Application | Role | Main technologies |
| --- | --- | --- |
| `backend` | REST API, business logic and integrations | Java, Spring Boot, Spring Security, JPA |
| `frontend` | Responsive single-page application | React, TypeScript, Vite |
| Database | Persistent marketplace data | PostgreSQL, Flyway |

---

## ✨ Features

| 🔐 Accounts & security | 📚 Book marketplace | 🛒 Shopping experience |
| --- | --- | --- |
| Registration, email verification and login | Searchable book catalog | Shopping cart |
| JWT access and refresh tokens | Category filtering | Wishlist |
| Protected frontend routes | Book details and condition | Easybox selection |
| Password reset and auth rate limiting | Create and edit listings | Shipping estimates |

| 📦 Seller tools | 💳 Orders & payments | 🔌 Integrations |
| --- | --- | --- |
| Publish or archive listings | Buyer order history | Cloudinary cover storage |
| Upload book covers | Detailed order tracking | Stripe sandbox payments |
| View and manage sales | Seller payment onboarding | Stripe Connect transfers |
| Track seller orders | Payment expiration flow | Sameday API and mock mode |

---

## 🧰 Tech stack

<div align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,react,ts,vite,postgres,maven,git&theme=dark" alt="Java, Spring, React, TypeScript, Vite, PostgreSQL, Maven and Git" />
</div>

### Backend

`Java 21` · `Spring Boot 3.5` · `Spring Web` · `Spring Security` · `Spring Data JPA` · `Hibernate` · `Bean Validation` · `JWT` · `Maven` · `Flyway`

### Frontend

`React 19` · `TypeScript` · `Vite` · `React Router` · `TanStack Query` · `Axios` · `React Hook Form` · `Zod` · `CSS`

### Services

`PostgreSQL` · `Cloudinary` · `Stripe Connect` · `Sameday eAWB / Easybox`

---

## 🗂️ Project structure

```text
AV-BookNest/
├── backend/                  # Spring Boot REST API
│   ├── src/main/java/        # Application code grouped by feature
│   ├── src/main/resources/   # Configuration and Flyway migrations
│   └── src/test/             # Backend tests
│
└── frontend/                 # React and TypeScript client
    ├── public/               # Static assets
    └── src/
        ├── api/              # Shared HTTP configuration
        ├── components/       # Reusable UI components
        ├── features/         # Feature API, hooks, schemas and types
        ├── pages/            # Application pages
        ├── routes/           # Routing and route guards
        └── styles/           # Global, component and page styles
```

---

## 🚀 Getting started

### Prerequisites

- Java 21
- Node.js 20.19+ and npm
- PostgreSQL
- Cloudinary account for book-cover uploads
- Stripe sandbox account for payment testing

### 1. Clone the repository

```bash
git clone https://github.com/vasibreban27/AV-BookNest.git
cd AV-BookNest
```

### 2. Configure the backend

Set the required environment variables in your IDE run configuration or terminal:

```text
DB_URL=jdbc:postgresql://localhost:5432/booknest
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password
JWT_SECRET=replace_with_a_secret_of_at_least_32_characters
```

Pentru rularea locală pe HTTP, valorile implicite ale cookie-urilor sunt suficiente. În
producție, după activarea HTTPS, setează și:

```text
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=https://booknest.example
```

JWT-urile sunt trimise exclusiv prin cookie-uri `HttpOnly`; frontend-ul nu le salvează și nu
le poate citi. Cererile care modifică date folosesc protecție CSRF prin cookie-ul
`XSRF-TOKEN` și header-ul `X-XSRF-TOKEN`.

To enable book-cover uploads, also set:

```text
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CLOUDINARY_FOLDER=booknest/book-covers
```

Stripe and Sameday configuration is documented in [`backend/README.md`](backend/README.md). Keep all credentials outside Git and never expose secrets through frontend `VITE_*` variables.

### 3. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API starts at **http://localhost:8085**.

### 4. Start the frontend

Open another terminal from the repository root:

```bash
cd frontend
npm install
npm run dev
```

The application starts at **http://localhost:5173**. In development, Vite proxies `/api` requests to the local backend.

În configurația recomandată, frontend-ul și API-ul sunt publicate pe aceeași origine, iar
`/api` este redirecționat către backend. Pentru development poți lăsa:

```text
VITE_API_URL=/api
```

<details>
<summary><strong>Optional: enable development seed data</strong></summary>

The Spring `dev` profile can populate repeatable demo users, categories, books, cart items, wishlist entries and sample orders.

```text
SPRING_PROFILES_ACTIVE=dev
APP_SEED_ENABLED=true
APP_SEED_PASSWORD=choose_a_demo_password
```

Demo accounts and integration-testing flows are documented in [`backend/README.md`](backend/README.md).

</details>

---

## ✅ Running checks

| Area | Commands |
| --- | --- |
| Backend | `cd backend` → `./mvnw verify` |
| Backend on Windows | `cd backend` → `.\mvnw.cmd verify` |
| Frontend lint | `cd frontend` → `npm run lint` |
| Frontend build | `cd frontend` → `npm run build` |

---

## 🧭 Roadmap

- [x] Authentication and protected routes
- [x] Catalog, listings, cart and wishlist
- [x] Buyer and seller order flows
- [x] Stripe sandbox and Sameday mock integrations
- [ ] Continue refining the interface and user experience
- [ ] Expand automated and end-to-end test coverage
- [ ] Complete production configuration for external integrations
- [ ] Deploy the frontend, backend and database
- [ ] Add project screenshots and a public live demo

---

## 👨‍💻 Author

<div align="center">
  <p>Developed by <a href="https://github.com/vasibreban27"><strong>Vasile Breban</strong></a> as a personal full-stack project.</p>
  <a href="https://github.com/vasibreban27">
    <img src="https://img.shields.io/badge/GitHub-vasibreban27-181717?style=for-the-badge&logo=github" alt="Vasile Breban on GitHub" />
  </a>
</div>
