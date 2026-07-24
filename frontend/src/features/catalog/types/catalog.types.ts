export type BookCondition =
  | 'NEW'
  | 'LIKE_NEW'
  | 'VERY_GOOD'
  | 'GOOD'
  | 'ACCEPTABLE'

export type BookStatus =
  | 'DRAFT'
  | 'AVAILABLE'
  | 'RESERVED'
  | 'SOLD'
  | 'ARCHIVED'

export type Category = {
  id: number
  name: string
  slug: string
  description: string | null
}

export type Book = {
  id: number
  title: string
  author: string
  isbn: string | null
  description: string | null
  price: number
  bookCondition: BookCondition
  language: string
  publisher: string | null
  publishedYear: number | null
  weightGrams: number
  lengthMm: number
  widthMm: number
  heightMm: number
  coverImageUrl: string | null
  sellerId: number
  sellerName: string
  category: Category
  status: BookStatus
  createdAt: string
  updatedAt: string
}

export type CatalogFilters = {
  searchTerm: string
  categorySlug: string
}
