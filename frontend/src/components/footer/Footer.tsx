import { Link } from 'react-router-dom'
import { Logo } from '../common/Logo'

function InstagramIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="3.5" y="3.5" width="17" height="17" rx="5" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="12" cy="12" r="4" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="17.5" cy="6.8" r="1" fill="currentColor" />
    </svg>
  )
}

function TikTokIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M14.2 4.2v10.2a4.2 4.2 0 1 1-3.6-4.15" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      <path d="M14.2 4.2c.55 2.6 2.05 4.05 4.6 4.5" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
    </svg>
  )
}

function VintedIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M8.4 5.5 12 9l3.6-3.5 3 2.4-2.25 3.15V19H7.65v-7.95L5.4 7.9l3-2.4Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
      <path d="M9.2 5.8c.55 1.1 1.5 1.7 2.8 1.7s2.25-.6 2.8-1.7" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  )
}

export function Footer() {
  return (
    <footer className="app-footer" id="footer">
      <div className="app-footer__grid">
        <div className="app-footer__brand">
          <Link to="/" aria-label="BookNest — pagina principală">
            <Logo />
          </Link>
          <p>Dăm cărților citite șansa la un capitol nou.</p>
          <div className="app-footer__socials">
            <span>Urmărește BookNest</span>
            <div>
              <a
                href="https://www.instagram.com/av.booknest/"
                target="_blank"
                rel="noreferrer"
                aria-label="BookNest pe Instagram"
                title="Instagram — @av.booknest"
              >
                <InstagramIcon />
              </a>
              <a
                href="https://www.tiktok.com/@avs.booknest"
                target="_blank"
                rel="noreferrer"
                aria-label="BookNest pe TikTok"
                title="TikTok — @avs.booknest"
              >
                <TikTokIcon />
              </a>
              <a
                href="https://www.vinted.ro/member/3172711487-avbooknest1"
                target="_blank"
                rel="noreferrer"
                aria-label="BookNest pe Vinted"
                title="Vinted — avbooknest1"
              >
                <VintedIcon />
              </a>
            </div>
          </div>
        </div>
        <nav className="app-footer__links" aria-label="Linkuri din subsol">
          <div className="app-footer__column">
            <strong>Explorează</strong>
            <Link to={{ pathname: '/', hash: '#recomandari' }}>Recomandări</Link>
            <Link to="/catalog">Catalog</Link>
            <Link to="/sell">Vinde o carte</Link>
          </div>
          <div className="app-footer__column">
            <strong>Cont</strong>
            <Link to="/account">Contul meu</Link>
            <Link to="/orders">Comenzile mele</Link>
            <Link to="/my-books">Cărțile mele</Link>
            <Link to="/wishlist">Favorite</Link>
          </div>
          <div className="app-footer__column">
            <strong>Ajutor</strong>
            <Link to="/contact">Contact</Link>
            <Link to="/consumer-rights">Retururi și reclamații</Link>
          </div>
          <div className="app-footer__column">
            <strong>Legal</strong>
            <Link to="/terms">Termeni și condiții</Link>
            <Link to="/privacy">Confidențialitate</Link>
            <Link to="/cookies">Cookie-uri</Link>
          </div>
        </nav>
      </div>
      <div className="app-footer__bottom">
        <span>© {new Date().getFullYear()} BookNest</span>
        <span>Marketplace pentru cărți care merită recitite.</span>
      </div>
    </footer>
  )
}
