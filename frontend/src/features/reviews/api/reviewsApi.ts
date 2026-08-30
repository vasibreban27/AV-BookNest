import { api } from '../../../api/client'
import type { AdminReviewFilters, CreateReviewPayload, OrderReview, PrivateReview, Review, ReviewModerationStatus, ReviewPage, SellerReputation } from '../types/review.types'

export const reviewsApi = {
  async order(orderId: number) {
    return (await api.get<OrderReview[]>(`/orders/${orderId}/reviews`)).data
  },
  async create(orderId: number, itemId: number, payload: CreateReviewPayload) {
    return (await api.post<PrivateReview>(`/orders/${orderId}/items/${itemId}/review`, payload)).data
  },
  async reputation(sellerId: number) {
    return (await api.get<SellerReputation>(`/sellers/${sellerId}/reputation`)).data
  },
  async publicReviews(sellerId: number, page: number) {
    return (await api.get<ReviewPage<Review>>(`/sellers/${sellerId}/reviews`, { params: { page, size: 5 } })).data
  },
  async admin(filters: AdminReviewFilters) {
    return (await api.get<ReviewPage<PrivateReview>>('/admin/reviews', { params: { ...filters, status: filters.status || undefined, size: 25 } })).data
  },
  async moderate(reviewId: number, status: ReviewModerationStatus, reason: string) {
    return (await api.patch<PrivateReview>(`/admin/reviews/${reviewId}/moderation`, { status, reason })).data
  },
}
