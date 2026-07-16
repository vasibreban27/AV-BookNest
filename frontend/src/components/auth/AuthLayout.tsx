import { Outlet } from 'react-router-dom'
import { CheckIcon } from '../common/Icons'
import { Logo } from '../common/Logo'

const benefits = [
  'Descoperă cărți de la cititori ca tine',
  'Vinde simplu volumele din biblioteca ta',
  'Comunitate sigură și pasionată',
]

export function AuthLayout() {
  return (
    <main className="auth-shell">
      <section className="auth-story" aria-label="Despre BookNest">
        <Logo />
        <div className="auth-story__content">
          <p className="eyebrow">O nouă poveste pentru fiecare carte</p>
          <h1>Cărțile merită să fie citite, nu uitate pe raft.</h1>
          <p className="auth-story__lead">
            Intră în comunitatea BookNest și dă mai departe poveștile care
            te-au inspirat.
          </p>
          <ul className="benefit-list">
            {benefits.map((benefit) => (
              <li key={benefit}>
                <span><CheckIcon /></span>
                {benefit}
              </li>
            ))}
          </ul>
          <p className="auth-story__quote">
            „O cameră fără cărți este ca un trup fără suflet.”
          </p>
        </div>

        <div className="book-scene" aria-hidden="true">
          <div className="book-scene__sun" />
          <div className="book book--one" />
          <div className="book book--two" />
          <div className="book book--three" />
          <div className="book book--four" />
          <div className="book-scene__plant"><i /><i /><i /></div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-panel__mobile-logo"><Logo /></div>
        <div className="auth-panel__content">
          <Outlet />
        </div>
        <p className="auth-panel__footer">© {new Date().getFullYear()} BookNest. Poveștile circulă.</p>
      </section>
    </main>
  )
}
