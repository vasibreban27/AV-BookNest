import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { BookCard } from '../../components/catalog/BookCard'
import { CatalogSearch } from '../../components/catalog/CatalogSearch'
import {
  BookGridSkeleton,
  CatalogEmptyState,
  CatalogErrorState,
} from '../../components/catalog/CatalogStates'
import { ArrowUpRightIcon } from '../../components/common/icons/AppIcons'
import { InteractiveBook } from '../../components/interactive-book/InteractiveBook'
import { useCatalog } from '../../features/catalog/hooks/useCatalog'

export function HomePage() {
  const [searchTerm, setSearchTerm] = useState('')
  const navigate = useNavigate()
  const {
    featuredBooks,
    categories,
    isLoading,
    isError,
    retry,
  } = useCatalog({
    searchTerm: '',
    categorySlug: '',
    condition: '',
    sort: 'newest',
    minimumPrice: '',
    maximumPrice: '',
    language: '',
    minimumYear: '',
    maximumYear: '',
  }, 5, false)

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const query = searchTerm.trim()
    navigate(query ? `/catalog?q=${encodeURIComponent(query)}` : '/catalog')
  }

  return (
    <main className="home-page">
        <section className="home-hero" id="acasa">
          <div className="home-hero__glow home-hero__glow--one" aria-hidden="true" />
          <div className="home-hero__glow home-hero__glow--two" aria-hidden="true" />
          <div className="home-container home-hero__inner">
            <div className="home-hero__copy">
              <h1>Următoarea ta <em>poveste</em> te așteaptă.</h1>
              <p>
                Descoperă cărți atent păstrate de alți cititori și oferă
                bibliotecii tale spațiu pentru noi aventuri.
              </p>
              <CatalogSearch
                inputId="hero-catalog-search"
                value={searchTerm}
                onChange={setSearchTerm}
                onSubmit={handleSearch}
              />
              <div className="home-hero__trust">
                <div><strong>{featuredBooks.length || '—'}</strong><span>selecții recente</span></div>
                <i />
                <div><strong>{categories.length || '—'}</strong><span>categorii de explorat</span></div>
                <i />
                <div><strong>100%</strong><span>pasiune pentru lectură</span></div>
              </div>
            </div>
            <InteractiveBook className="home-hero__book" />
          </div>
        </section>

        <section className="home-section home-section--featured" id="recomandari">
          <div className="home-container">
            <div className="section-heading">
              <div>
                <h2>Descoperiri pentru biblioteca ta</h2>
                <p>Volume disponibile recent în comunitatea BookNest.</p>
              </div>
              <Link to="/catalog">Explorează toate anunțurile <ArrowUpRightIcon /></Link>
            </div>

            {isLoading && <BookGridSkeleton />}
            {isError && <CatalogErrorState onRetry={retry} />}
            {!isLoading && !isError && featuredBooks.length > 0 && (
              <div className="book-grid">
                {featuredBooks.slice(0, 4).map((book) => (
                  <BookCard book={book} key={book.id} />
                ))}
              </div>
            )}
            {!isLoading && !isError && featuredBooks.length === 0 && <CatalogEmptyState />}
          </div>
        </section>

        {categories.length > 0 && (
          <section className="home-section home-section--categories">
            <div className="home-container">
              <div className="section-heading">
                <div>
                  <h2>Explorează după categorie</h2>
                  <p>Intră direct în raftul care se potrivește cu ce ai chef să citești.</p>
                </div>
              </div>
              <div className="home-category-grid">
                {categories.map((category) => (
                  <Link
                    className="home-category-card"
                    to={`/catalog?category=${encodeURIComponent(category.slug)}`}
                    key={category.id}
                  >
                    <span>{category.name}</span>
                    <small>
                      {category.bookCount} {category.bookCount === 1 ? 'anunț' : 'anunțuri'}
                    </small>
                    <ArrowUpRightIcon />
                  </Link>
                ))}
              </div>
            </div>
          </section>
        )}

        <section className="home-section home-section--steps">
          <div className="home-container">
            <div className="section-heading section-heading--centered">
              <div>
                <h2>De la un cititor la altul</h2>
                <p>BookNest păstrează procesul simplu, de la descoperirea cărții până la ridicare.</p>
              </div>
            </div>
            <div className="home-steps">
              <article>
                <span>1</span>
                <h3>Găsești cartea</h3>
                <p>Cauți după titlu sau autor și filtrezi catalogul după preferințele tale.</p>
              </article>
              <article>
                <span>2</span>
                <h3>Comanzi în siguranță</h3>
                <p>Plata este pregătită prin Stripe, iar vânzătorul confirmă expedierea.</p>
              </article>
              <article>
                <span>3</span>
                <h3>Ridici din Easybox</h3>
                <p>Alegi destinația convenabilă și urmărești evoluția comenzii din cont.</p>
              </article>
            </div>
          </div>
        </section>

        <section className="home-section home-section--sell">
          <div className="home-container">
            <div className="home-sell-card">
              <div>
                <h2>Fă loc pentru următoarea carte.</h2>
                <p>Publică volumele pe care le-ai terminat și ajută poveștile bune să ajungă la un nou cititor.</p>
              </div>
              <div className="home-sell-card__actions">
                <Link className="home-primary-link" to="/sell">Publică un anunț</Link>
                <Link className="home-secondary-link" to="/catalog">Vezi catalogul</Link>
              </div>
            </div>
          </div>
        </section>

    </main>
  )
}
