import { useRef, useState, type FormEvent } from 'react'
import { BookCard } from '../../components/catalog/BookCard'
import { CatalogSearch } from '../../components/catalog/CatalogSearch'
import {
  BookGridSkeleton,
  CatalogEmptyState,
  CatalogErrorState,
} from '../../components/catalog/CatalogStates'
import { CategoryFilter } from '../../components/catalog/CategoryFilter'
import { ArrowUpRightIcon, SparkleIcon } from '../../components/common/icons/AppIcons'
import { InteractiveBook } from '../../components/interactive-book/InteractiveBook'
import { useCatalog } from '../../features/catalog/hooks/useCatalog'
import type { BookCondition, CatalogSort } from '../../features/catalog/types/catalog.types'

export function HomePage() {
  const [searchTerm, setSearchTerm] = useState('')
  const [categorySlug, setCategorySlug] = useState('')
  const [condition, setCondition] = useState<BookCondition | ''>('')
  const [sort, setSort] = useState<CatalogSort>('newest')
  const [minimumPrice, setMinimumPrice] = useState('')
  const [maximumPrice, setMaximumPrice] = useState('')
  const catalogRef = useRef<HTMLElement>(null)
  const {
    books,
    featuredBooks,
    totalBooks,
    categories,
    isLoading,
    isError,
    retry,
    hasMore,
    isLoadingMore,
    loadMore,
  } = useCatalog({
    searchTerm,
    categorySlug,
    condition,
    sort,
    minimumPrice,
    maximumPrice,
  })

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    catalogRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
              <a href="#catalog">Vezi catalogul <ArrowUpRightIcon /></a>
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

        <section className="home-section home-section--catalog" id="catalog" ref={catalogRef}>
          <div className="home-container">
            <div className="section-heading section-heading--catalog">
              <div>
                <span className="home-kicker">Catalog BookNest</span>
                <h2>Caută. Alege. Citește.</h2>
                <p>{totalBooks} {totalBooks === 1 ? 'carte disponibilă' : 'cărți disponibile'}</p>
              </div>
              <CatalogSearch
                inputId="catalog-search"
                value={searchTerm}
                onChange={setSearchTerm}
                onSubmit={handleSearch}
              />
            </div>

            <CategoryFilter
              categories={categories}
              selectedSlug={categorySlug}
              onSelect={setCategorySlug}
            />

            <div className="catalog-toolbar" aria-label="Filtre catalog">
              <label>
                <span>Preț minim</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={minimumPrice}
                  onChange={(event) => setMinimumPrice(event.target.value)}
                  placeholder="0 RON"
                />
              </label>
              <label>
                <span>Preț maxim</span>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={maximumPrice}
                  onChange={(event) => setMaximumPrice(event.target.value)}
                  placeholder="Orice preț"
                />
              </label>
              <label>
                <span>Stare</span>
                <select
                  value={condition}
                  onChange={(event) => setCondition(event.target.value as BookCondition | '')}
                >
                  <option value="">Toate stările</option>
                  <option value="NEW">Nouă</option>
                  <option value="LIKE_NEW">Ca nouă</option>
                  <option value="VERY_GOOD">Foarte bună</option>
                  <option value="GOOD">Bună</option>
                  <option value="ACCEPTABLE">Acceptabilă</option>
                </select>
              </label>
              <label>
                <span>Sortare</span>
                <select
                  value={sort}
                  onChange={(event) => setSort(event.target.value as CatalogSort)}
                >
                  <option value="newest">Cele mai noi</option>
                  <option value="price_asc">Preț crescător</option>
                  <option value="price_desc">Preț descrescător</option>
                  <option value="title_asc">Titlu A–Z</option>
                </select>
              </label>
            </div>

            {isLoading && <BookGridSkeleton />}
            {isError && <CatalogErrorState onRetry={retry} />}
            {!isLoading && !isError && books.length > 0 && (
              <>
                <div className="book-grid book-grid--catalog">
                  {books.map((book) => <BookCard book={book} key={book.id} />)}
                </div>
                {hasMore && (
                  <button
                    className="catalog-load-more"
                    type="button"
                    onClick={() => void loadMore()}
                    disabled={isLoadingMore}
                  >
                    {isLoadingMore ? 'Se încarcă…' : 'Arată mai multe'}
                  </button>
                )}
              </>
            )}
            {!isLoading && !isError && books.length === 0 && <CatalogEmptyState />}
          </div>
        </section>
    </main>
  )
}
