import type { FormEvent } from 'react'
import type { Book, Category } from '../../../features/catalog/types/catalog.types'

export type BookCardProps = {
  book: Book
}

export type CatalogSearchProps = {
  inputId: string
  value: string
  onChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export type CategoryFilterProps = {
  categories: Category[]
  selectedSlug: string
  onSelect: (slug: string) => void
}

export type CatalogStateProps = {
  onRetry?: () => void
}
