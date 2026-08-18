import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

const updatedAt = '18 august 2026'

type LegalDocumentProps = {
  title: string
  intro: string
  children: ReactNode
}

function LegalDocument({ title, intro, children }: LegalDocumentProps) {
  return (
    <main className="legal-page">
      <header className="legal-hero">
        <div className="legal-container">
          <Link to="/" className="legal-back">← Înapoi la BookNest</Link>
          <h1>{title}</h1>
          <p>{intro}</p>
          <span>Ultima actualizare: {updatedAt}</span>
        </div>
      </header>
      <div className="legal-container legal-layout">
        <aside className="legal-summary">
          <strong>Pe scurt</strong>
          <p>Am scris documentul într-un limbaj direct. Pentru întrebări, ne poți scrie oricând.</p>
          <Link to="/contact">Contactează BookNest</Link>
          <a href="mailto:av.booknest@gmail.com">av.booknest@gmail.com</a>
        </aside>
        <article className="legal-document">
          <div className="legal-draft-notice">
            <strong>Document pentru etapa de dezvoltare</strong>
            <p>Înainte de lansarea comercială trebuie completate denumirea legală a operatorului, sediul, CUI-ul și numărul din Registrul Comerțului, apoi documentul trebuie verificat juridic.</p>
          </div>
          {children}
        </article>
      </div>
    </main>
  )
}

export function PrivacyPage() {
  return (
    <LegalDocument
      title="Politica de confidențialitate"
      intro="Află ce date folosește BookNest, de ce sunt necesare și ce drepturi ai asupra lor."
    >
      <section>
        <h2>1. Cine prelucrează datele</h2>
        <p>Operatorul platformei BookNest va fi identificat prin datele legale complete înainte de lansarea publică. Până atunci, pentru orice solicitare privind datele personale, punctul de contact este <a href="mailto:av.booknest@gmail.com">av.booknest@gmail.com</a>.</p>
      </section>

      <section>
        <h2>2. Datele pe care le folosim</h2>
        <ul>
          <li>date de cont și profil: nume, email, telefon, parolă stocată numai în formă criptată și starea verificării emailului;</li>
          <li>date despre anunțuri: titlu, autor, descriere, fotografie, preț și informații despre carte;</li>
          <li>date despre comenzi, plăți și livrare, inclusiv destinatarul, telefonul și easybox-ul ales;</li>
          <li>mesaje trimise către suport și informațiile pe care alegi să le incluzi;</li>
          <li>date tehnice și de securitate, precum adresa IP, momentele autentificării și evenimente necesare prevenirii abuzului.</li>
        </ul>
      </section>

      <section>
        <h2>3. Scopuri și temeiuri</h2>
        <p>Folosim datele pentru crearea și securizarea contului, publicarea anunțurilor, executarea comenzilor, comunicarea între părți, prevenirea fraudei și soluționarea solicitărilor. Temeiul poate fi executarea contractului, îndeplinirea unei obligații legale, interesul legitim pentru securitate și îmbunătățirea serviciului sau consimțământul, când legea îl cere.</p>
      </section>

      <section>
        <h2>4. Furnizori și destinatari</h2>
        <p>Datele sunt comunicate numai cât este necesar furnizorilor tehnici implicați: serviciului de email Google, găzduirii și bazei de date, Cloudinary pentru imaginile anunțurilor și, când integrările reale vor fi activate, Stripe pentru plăți și Sameday pentru livrare. Cumpărătorii și vânzătorii primesc doar informațiile necesare tranzacției.</p>
        <p>Unii furnizori pot prelucra date în afara Spațiului Economic European; înainte de producție vor fi documentate locațiile și garanțiile aplicabile fiecărui transfer.</p>
      </section>

      <section>
        <h2>5. Cât timp păstrăm datele</h2>
        <p>Datele de cont se păstrează cât timp contul este activ și apoi atât cât este necesar pentru obligații legale, securitate sau litigii. Datele comenzilor urmează perioadele legale aplicabile. Mesajele de suport sunt șterse când solicitarea și eventualele obligații asociate au fost închise. Înainte de lansare va fi aprobat un calendar intern exact de retenție.</p>
      </section>

      <section>
        <h2>6. Drepturile tale</h2>
        <p>Poți solicita accesul, rectificarea, ștergerea, restricționarea, portabilitatea sau opoziția, după caz, și îți poți retrage consimțământul fără a afecta prelucrarea anterioară. Vom putea cere informații rezonabile pentru confirmarea identității.</p>
        <p>Ai și dreptul de a depune o plângere la <a href="https://www.dataprotection.ro/" target="_blank" rel="noreferrer">Autoritatea Națională de Supraveghere a Prelucrării Datelor cu Caracter Personal</a>.</p>
      </section>

      <section>
        <h2>7. Securitate și modificări</h2>
        <p>Aplicăm verificarea emailului, parole criptate, linkuri temporare, limitarea încercărilor și acces pe bază de rol. Nicio metodă nu elimină complet riscul; nu transmite parole sau date de card prin formularul de contact. Modificările importante ale politicii vor fi anunțate în platformă.</p>
      </section>
    </LegalDocument>
  )
}

export function TermsPage() {
  return (
    <LegalDocument
      title="Termeni și condiții"
      intro="Regulile de folosire a marketplace-ului BookNest pentru cumpărători și vânzători."
    >
      <section>
        <h2>1. Rolul BookNest</h2>
        <p>BookNest este o platformă care pune în legătură persoane interesate să vândă și să cumpere cărți. Dacă într-o ofertă nu se precizează altfel, contractul de vânzare este încheiat între cumpărător și vânzător; BookNest facilitează listarea, plata și livrarea și nu devine proprietarul cărții.</p>
      </section>

      <section>
        <h2>2. Contul</h2>
        <p>Utilizatorul trebuie să ofere informații corecte, să își verifice adresa de email și să păstreze confidențiale datele de acces. Contul nu poate fi folosit pentru fraudă, hărțuire, automatizări abuzive sau eludarea măsurilor de securitate.</p>
      </section>

      <section>
        <h2>3. Reguli pentru anunțuri</h2>
        <p>Vânzătorul răspunde pentru descrierea, starea, autenticitatea, dreptul de a vinde și legalitatea cărții. Sunt interzise produsele contrafăcute, conținutul ilegal, fotografiile folosite fără drept, prețurile înșelătoare și anunțurile care mută intenționat tranzacția în afara platformei pentru a evita măsurile de protecție.</p>
      </section>

      <section>
        <h2>4. Vânzător privat sau profesionist</h2>
        <p>Regulile de protecție a consumatorului diferă în funcție de statutul vânzătorului. Înainte de lansarea comercială, platforma trebuie să introducă declararea și afișarea statutului de profesionist și verificările necesare. Un utilizator nu poate pretinde că este vânzător privat dacă acționează în scop comercial sau profesional.</p>
      </section>

      <section>
        <h2>5. Comenzi, plată și livrare</h2>
        <p>Prețul, costul livrării și pașii tranzacției sunt afișați înainte de confirmare. Integrarea Stripe procesează plata, iar Sameday gestionează livrarea atunci când serviciile reale sunt activate. Funcțiile marcate drept sandbox sau mock sunt exclusiv pentru testare și nu trebuie folosite pentru tranzacții reale.</p>
      </section>

      <section>
        <h2>6. Moderare și suspendare</h2>
        <p>BookNest poate ascunde un anunț, bloca o acțiune sau suspenda un cont când există indicii rezonabile de încălcare a legii, a acestor reguli sau a securității platformei. Utilizatorul poate cere explicații și reanalizarea deciziei prin pagina de contact.</p>
      </section>

      <section>
        <h2>7. Răspundere și lege aplicabilă</h2>
        <p>Fiecare parte răspunde pentru propriile obligații. Nicio clauză nu limitează drepturile care nu pot fi înlăturate prin contract. Termenii sunt guvernați de legea română, iar părțile vor încerca mai întâi soluționarea amiabilă a unei neînțelegeri.</p>
      </section>
    </LegalDocument>
  )
}

export function CookiesPage() {
  return (
    <LegalDocument
      title="Politica privind cookie-urile"
      intro="Ce stochează BookNest în browser și cum poți controla aceste informații."
    >
      <section>
        <h2>1. Situația actuală</h2>
        <p>Versiunea actuală BookNest nu folosește cookie-uri de publicitate sau analiză. Pentru menținerea autentificării, aplicația salvează în spațiul local al browserului cheia tehnică <code>booknest.auth.session</code>, care conține tokenurile sesiunii și datele de bază ale contului.</p>
      </section>

      <section>
        <h2>2. De ce este necesară stocarea locală</h2>
        <p>Această informație permite păstrarea sesiunii, autorizarea cererilor către server și deconectarea. Nu este folosită pentru publicitate și nu urmărește navigarea pe alte site-uri. Este eliminată când te deconectezi; o poți șterge și din setările browserului, caz în care vei fi deconectat.</p>
      </section>

      <section>
        <h2>3. Servicii terțe</h2>
        <p>Stripe, Sameday sau alte pagini externe pot folosi propriile tehnologii atunci când utilizatorul accesează serviciile lor. Politicile acelor furnizori se aplică pe domeniile lor. BookNest va actualiza această pagină înainte de activarea oricărei tehnologii suplimentare.</p>
      </section>

      <section>
        <h2>4. Consimțământ</h2>
        <p>Dacă vor fi introduse tehnologii neesențiale de analiză sau marketing, acestea nu vor fi activate înainte ca utilizatorul să primească informații clare și să își exprime opțiunea. Refuzul lor nu va bloca funcțiile de bază ale platformei.</p>
      </section>
    </LegalDocument>
  )
}

export function ConsumerRightsPage() {
  return (
    <LegalDocument
      title="Retururi, reclamații și drepturile cumpărătorului"
      intro="Pașii utili când o carte nu corespunde și diferența dintre vânzătorii privați și profesioniști."
    >
      <section>
        <h2>1. Verifică statutul vânzătorului</h2>
        <p>Dreptul legal de retragere de 14 zile privește, în principal, contractele la distanță dintre un consumator și un profesionist. El nu se aplică automat unei vânzări ocazionale între două persoane fizice. De aceea, statutul vânzătorului trebuie afișat clar înainte de comandă.</p>
      </section>

      <section>
        <h2>2. Dacă vânzătorul este profesionist</h2>
        <p>Consumatorul beneficiază de informațiile precontractuale și de drepturile prevăzute de legislația aplicabilă, inclusiv retragerea în termenul legal atunci când nu există o excepție. Profesionistul trebuie să ofere instrucțiunile de retur, datele sale și condițiile de rambursare.</p>
      </section>

      <section>
        <h2>3. Dacă vânzătorul este persoană privată</h2>
        <p>Condițiile anunțului și regulile civile ale vânzării rămân relevante, însă mecanismele speciale de protecție a consumatorului nu se aplică automat. Descrierea falsă, ascunderea defectelor sau neexecutarea obligațiilor pot fi totuși contestate.</p>
      </section>

      <section>
        <h2>4. Cum semnalezi o problemă</h2>
        <ol>
          <li>păstrează fotografiile, conversațiile, confirmarea comenzii și documentele de livrare;</li>
          <li>folosește acțiunea de raportare a problemei din comandă, dacă este disponibilă;</li>
          <li>scrie-ne prin <Link to="/contact">formularul de contact</Link>, menționând numărul comenzii și soluția solicitată;</li>
          <li>nu returna produsul în afara fluxului agreat înainte de a păstra dovezile necesare.</li>
        </ol>
      </section>

      <section>
        <h2>5. Soluționare amiabilă și autorități</h2>
        <p>Pentru un litigiu între consumator și profesionist, consumatorul poate consulta procedura de <a href="https://anpc.ro/sal/" target="_blank" rel="noreferrer">Soluționare Alternativă a Litigiilor a ANPC</a>. Mecanismul SAL nu este destinat, în mod obișnuit, disputelor dintre două persoane private și nu înlocuiește dreptul de acces la instanță.</p>
      </section>
    </LegalDocument>
  )
}
