export type ReviewModerationStatus = 'VISIBLE' | 'HIDDEN'

export type Review = {
  id: number
  bookId: number | null
  bookTitle: string
  sellerId: number
  reviewerName: string
  sellerRating: number
  descriptionRating: number | null
  conditionRating: number | null
  comment: string | null
  verifiedPurchase: boolean
  createdAt: string
}

export type PrivateReview = {
  review: Review
  orderItemId: number | null
  moderationStatus: ReviewModerationStatus
  moderationReason: string | null
  moderatedAt: string | null
}

export type OrderReview = {
  orderItemId: number
  sellerId: number
  bookTitle: string
  canReview: boolean
  ineligibilityReason: 'ALREADY_REVIEWED' | 'NOT_DELIVERED' | 'SELF_REVIEW' | null
  existingReview: PrivateReview | null
}

export type CreateReviewPayload = {
  sellerRating: number
  descriptionRating: number
  conditionRating: number
  comment: string | null
}

export type SellerReputation = {
  sellerId: number
  sellerName: string
  ratings: {
    reviewCount: number
    sellerRating: number | null
    descriptionRating: number | null
    conditionRating: number | null
  }
}

export type ReviewPage<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  hasNext: boolean
}

export type AdminReviewFilters = { q: string; status: ReviewModerationStatus | ''; page: number }
