import { SearchIcon } from '../common/icons/AppIcons'
import type { CatalogSearchProps } from './types/catalog-component.types'

export function CatalogSearch({ inputId, value, onChange, onSubmit }: CatalogSearchProps) {
  return (
    <form className="catalog-search" role="search" onSubmit={onSubmit}>
      <SearchIcon className="catalog-search__icon" />
      <label className="sr-only" htmlFor={inputId}>
        Caută după titlu sau autor
      </label>
      <input
        id={inputId}
        type="search"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Caută după titlu sau autor..."
        autoComplete="off"
      />
      <button type="submit">Caută cărți</button>
    </form>
  )
}
