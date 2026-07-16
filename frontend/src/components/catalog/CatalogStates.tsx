import { BookOutlineIcon } from '../common/icons/AppIcons'
import type { CatalogStateProps } from './types/catalog-component.types'

export function BookGridSkeleton() {
  return (
    <div className="book-grid book-grid--skeleton" aria-label="Se încarcă volumele">
      {Array.from({ length: 4 }, (_, index) => (
        <div className="book-card-skeleton" key={index}>
          <span />
          <i />
          <i />
        </div>
      ))}
    </div>
  )
}

export function CatalogEmptyState() {
  return (
    <div className="catalog-state">
      <BookOutlineIcon />
      <h3>Nicio carte nu se potrivește încă</h3>
      <p>Încearcă un alt titlu, autor sau o categorie diferită.</p>
    </div>
  )
}

export function CatalogErrorState({ onRetry }: CatalogStateProps) {
  return (
    <div className="catalog-state catalog-state--error" role="alert">
      <BookOutlineIcon />
      <h3>Catalogul ia o scurtă pauză</h3>
      <p>Nu am putut încărca volumele. Verifică backend-ul și încearcă din nou.</p>
      {onRetry && <button type="button" onClick={onRetry}>Reîncearcă</button>}
    </div>
  )
}
