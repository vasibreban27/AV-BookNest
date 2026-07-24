import type { Book } from '../../catalog/types/catalog.types'

export type ListingPayload = {
  title: string
  author: string
  isbn: string | null
  description: string | null
  price: number
  bookCondition: Book['bookCondition']
  language: string
  publisher: string | null
  publishedYear: number | null
  weightGrams: number
  lengthMm: number
  widthMm: number
  heightMm: number
  categoryId: number
}

export type CreateListingInput = {
  payload: ListingPayload
  coverFile: File | null
}

export type UpdateListingInput = CreateListingInput & {
  bookId: number
  removeCover: boolean
}

export type ListingEditLocationState = {
  coverUploadFailed?: boolean
}
