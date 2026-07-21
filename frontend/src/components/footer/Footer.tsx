import { Link } from 'react-router-dom'
import { Logo } from '../common/Logo'

export function Footer() {
  return (
    <footer className="app-footer" id="footer">
      <div className="app-footer__top">
        <div>
          <Link to="/" aria-label="BookNest — pagina principală">
            <Logo />
          </Link>
          <p>Dăm cărților citite șansa la un capitol nou.</p>
        </div>
        <div className="app-footer__links">
          <div>
            <strong>Explorează</strong>
            <Link to={{ pathname: '/', hash: '#recomandari' }}>Recomandări</Link>
            <Link to={{ pathname: '/', hash: '#catalog' }}>Catalog</Link>
          </div>
          <div>
            <strong>BookNest</strong>
            <Link to="/account">Contul meu</Link>
            <Link to="/orders">Comenzile mele</Link>
            <Link to="/my-books">Cărțile mele</Link>
            <Link to="/sell">Vinde o carte</Link>
            <Link to="/wishlist">Favorite</Link>
            <span>Comunitate</span>
          </div>
        </div>
      </div>
      <div className="app-footer__bottom">
        <span>© {new Date().getFullYear()} BookNest</span>
        <span>Făcut cu grijă pentru cititori.</span>
      </div>
    </footer>
  )
}
