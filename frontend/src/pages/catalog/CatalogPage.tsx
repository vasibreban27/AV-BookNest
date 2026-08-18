import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { BookCard } from '../../components/catalog/BookCard'
import { CatalogSearch } from '../../components/catalog/CatalogSearch'
import {
  BookGridSkeleton,
  CatalogEmptyState,
  CatalogErrorState,
} from '../../components/catalog/CatalogStates'
import { useCatalog } from '../../features/catalog/hooks/useCatalog'
import type {
  BookCondition,
  CatalogSort,
} from '../../features/catalog/types/catalog.types'

const conditions = new Set<BookCondition>([
  'NEW',
  'LIKE_NEW',
  'VERY_GOOD',
  'GOOD',
  'ACCEPTABLE',
])
const sorts = new Set<CatalogSort>([
  'newest',
  'price_asc',
  'price_desc',
  'title_asc',
  'year_desc',
])

export function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const conditionParam = searchParams.get('condition') ?? ''
  const sortParam = searchParams.get('sort') ?? 'newest'
  const condition: BookCondition | '' = conditions.has(conditionParam as BookCondition)
    ? conditionParam as BookCondition
    : ''
  const sort = sorts.has(sortParam as CatalogSort)
    ? sortParam as CatalogSort
    : 'newest'

  const filters = {
    searchTerm: searchParams.get('q') ?? '',
    categorySlug: searchParams.get('category') ?? '',
    minimumPrice: searchParams.get('minPrice') ?? '',
    maximumPrice: searchParams.get('maxPrice') ?? '',
    condition,
    language: searchParams.get('language') ?? '',
    minimumYear: searchParams.get('minYear') ?? '',
    maximumYear: searchParams.get('maxYear') ?? '',
    sort,
  }
  const minimumPrice = Number(filters.minimumPrice)
  const maximumPrice = Number(filters.maximumPrice)
  const minimumYear = Number(filters.minimumYear)
  const maximumYear = Number(filters.maximumYear)
  const currentYear = new Date().getFullYear()
  const invalidPriceRange = Boolean(
    filters.minimumPrice &&
    filters.maximumPrice &&
    minimumPrice > maximumPrice,
  )
  const invalidYearRange = Boolean(
    (filters.minimumYear && (minimumYear < 1 || minimumYear > currentYear)) ||
    (filters.maximumYear && (maximumYear < 1 || maximumYear > currentYear)) ||
    (filters.minimumYear && filters.maximumYear && minimumYear > maximumYear),
  )
  const filtersAreValid = !invalidPriceRange && !invalidYearRange
  const catalog = useCatalog(filters, 12, filtersAreValid)

  const updateFilter = (name: string, value: string) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (value) next.set(name, value)
      else next.delete(name)
      return next
    }, { replace: true })
  }

  const resetFilters = () => setSearchParams({}, { replace: true })
  const handleSearch = (event: FormEvent<HTMLFormElement>) => event.preventDefault()

  return (
    <main className="catalog-page">
      <section className="catalog-page__hero">
        <div className="catalog-page__container">
          <nav className="catalog-page__breadcrumb" aria-label="Navigare secundară">
            <Link to="/">Acasă</Link><span>/</span><strong>Catalog</strong>
          </nav>
          <span className="home-kicker">Toate anunțurile</span>
          <h1>Găsește cartea potrivită.</h1>
          <p>Caută în biblioteca comunității și restrânge rezultatele după ce contează pentru tine.</p>
          <CatalogSearch
            inputId="catalog-page-search"
            value={filters.searchTerm}
            onChange={(value) => updateFilter('q', value)}
            onSubmit={handleSearch}
          />
        </div>
      </section>

      <section className="catalog-page__content">
        <div className="catalog-page__container catalog-page__layout">
          <aside className="catalog-filters" aria-label="Filtre catalog">
            <div className="catalog-filters__heading">
              <div>
                <span>Rafinează</span>
                <h2>Filtre</h2>
              </div>
              <button type="button" onClick={resetFilters}>Resetează</button>
            </div>

            <label>
              <span>Categorie</span>
              <select
                value={filters.categorySlug}
                onChange={(event) => updateFilter('category', event.target.value)}
              >
                <option value="">Toate categoriile</option>
                {catalog.categories.map((category) => (
                  <option value={category.slug} key={category.id}>
                    {category.name} ({category.bookCount})
                  </option>
                ))}
              </select>
            </label>

            <div className="catalog-filters__row">
              <label>
                <span>Preț minim</span>
                <input
                  type="number"
                  min="0"
                  value={filters.minimumPrice}
                  onChange={(event) => updateFilter('minPrice', event.target.value)}
                  placeholder="0"
                />
              </label>
              <label>
                <span>Preț maxim</span>
                <input
                  type="number"
                  min="0"
                  value={filters.maximumPrice}
                  onChange={(event) => updateFilter('maxPrice', event.target.value)}
                  placeholder="Oricare"
                />
              </label>
            </div>

            <label>
              <span>Starea cărții</span>
              <select
                value={filters.condition}
                onChange={(event) => updateFilter('condition', event.target.value)}
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
              <span>Limbă</span>
              <select
                value={filters.language}
                onChange={(event) => updateFilter('language', event.target.value)}
              >
                <option value="">Toate limbile</option>
                {catalog.languages.map((language) => (
                  <option value={language} key={language}>{language}</option>
                ))}
              </select>
            </label>

            <div className="catalog-filters__row">
              <label>
                <span>An de la</span>
                <input
                  type="number"
                  min="1"
                  max={new Date().getFullYear()}
                  value={filters.minimumYear}
                  onChange={(event) => updateFilter('minYear', event.target.value)}
                  placeholder="Ex. 1990"
                />
              </label>
              <label>
                <span>An până la</span>
                <input
                  type="number"
                  min="1"
                  max={new Date().getFullYear()}
                  value={filters.maximumYear}
                  onChange={(event) => updateFilter('maxYear', event.target.value)}
                  placeholder="Ex. 2026"
                />
              </label>
            </div>
          </aside>

          <div className="catalog-results">
            <div className="catalog-results__heading">
              <div>
                <span>Rezultate</span>
                <h2>
                  {catalog.totalBooks} {catalog.totalBooks === 1 ? 'carte găsită' : 'cărți găsite'}
                </h2>
              </div>
              <label>
                <span>Sortare</span>
                <select
                  value={filters.sort}
                  onChange={(event) => updateFilter('sort', event.target.value)}
                >
                  <option value="newest">Cele mai noi</option>
                  <option value="price_asc">Preț crescător</option>
                  <option value="price_desc">Preț descrescător</option>
                  <option value="title_asc">Titlu A–Z</option>
                  <option value="year_desc">Anul publicării</option>
                </select>
              </label>
            </div>

            {!filtersAreValid && (
              <div className="catalog-filter-error" role="alert">
                {invalidPriceRange
                  ? 'Prețul minim nu poate fi mai mare decât prețul maxim.'
                  : `Intervalul de ani trebuie să fie între 1 și ${currentYear}, în ordine crescătoare.`}
              </div>
            )}

            {filtersAreValid && catalog.isLoading && <BookGridSkeleton />}
            {filtersAreValid && catalog.isError && <CatalogErrorState onRetry={catalog.retry} />}
            {filtersAreValid && !catalog.isLoading && !catalog.isError && catalog.books.length === 0 && (
              <CatalogEmptyState />
            )}
            {filtersAreValid && !catalog.isLoading && !catalog.isError && catalog.books.length > 0 && (
              <>
                <div className="book-grid book-grid--catalog-page">
                  {catalog.books.map((book) => <BookCard book={book} key={book.id} />)}
                </div>
                {catalog.hasMore && (
                  <button
                    className="catalog-load-more"
                    type="button"
                    onClick={() => void catalog.loadMore()}
                    disabled={catalog.isLoadingMore}
                  >
                    {catalog.isLoadingMore ? 'Se încarcă…' : 'Arată mai multe'}
                  </button>
                )}
              </>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}
