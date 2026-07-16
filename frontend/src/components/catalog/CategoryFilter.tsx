import type { CategoryFilterProps } from './types/catalog-component.types'

export function CategoryFilter({
  categories,
  selectedSlug,
  onSelect,
}: CategoryFilterProps) {
  if (!categories.length) return null

  return (
    <div className="category-filter" aria-label="Filtrează după categorie">
      <button
        className={!selectedSlug ? 'category-filter__item--active' : ''}
        type="button"
        onClick={() => onSelect('')}
      >
        Toate
      </button>
      {categories.map((category) => (
        <button
          className={selectedSlug === category.slug ? 'category-filter__item--active' : ''}
          type="button"
          key={category.id}
          onClick={() => onSelect(category.slug)}
        >
          {category.name}
        </button>
      ))}
    </div>
  )
}
