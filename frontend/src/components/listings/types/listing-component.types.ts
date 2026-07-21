import type { Book, Category } from '../../../features/catalog/types/catalog.types'
import type { ListingFormValues } from '../../../features/listings/types/listing-form.types'
import type { ListingPayload } from '../../../features/listings/types/listing.types'

export type ListingFormSubmission = {
  payload: ListingPayload
  coverFile: File | null
  removeCover: boolean
}

export type ListingFormProps = {
  categories: Category[]
  initialBook?: Book
  mode: 'create' | 'edit'
  submitError: string | null
  isPending: boolean
  onSubmit: (submission: ListingFormSubmission) => Promise<void>
}

export type ListingCardProps = {
  book: Book
  actionPending: boolean
  onArchive: (bookId: number) => void
  onPublish: (bookId: number) => void
}

export type ListingErrorStateProps = {
  onRetry: () => void
}

export type ListingFormFieldProps = {
  errors: Partial<Record<keyof ListingFormValues, { message?: string }>>
}
