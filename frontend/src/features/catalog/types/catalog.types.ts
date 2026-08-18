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
  condition: BookCondition | ''
  sort: CatalogSort
  minimumPrice: string
  maximumPrice: string
  language: string
  minimumYear: string
  maximumYear: string
}

export type CatalogCategory = {
  id: number
  name: string
  slug: string
  bookCount: number
}

export type CatalogSort =
  | 'newest'
  | 'price_asc'
  | 'price_desc'
  | 'title_asc'
  | 'year_desc'

export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
}

export type CatalogRequest = {
  q?: string
  category?: string
  condition?: BookCondition
  minPrice?: number
  maxPrice?: number
  language?: string
  minYear?: number
  maxYear?: number
  sort?: CatalogSort
  page?: number
  size?: number
}
