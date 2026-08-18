import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { BookCard } from '../../components/catalog/BookCard'
import { CatalogSearch } from '../../components/catalog/CatalogSearch'
import {
  BookGridSkeleton,
  CatalogEmptyState,
  CatalogErrorState,
} from '../../components/catalog/CatalogStates'
import { ArrowUpRightIcon, SparkleIcon } from '../../components/common/icons/AppIcons'
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
              <span className="home-kicker"><SparkleIcon /> Biblioteca care continuă să crească</span>
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
                <span className="home-kicker"><SparkleIcon /> Proaspăt adăugate</span>
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

    </main>
  )
}
