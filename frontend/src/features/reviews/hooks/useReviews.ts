import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { reviewsApi } from '../api/reviewsApi'
import { useAuth } from '../../auth/hooks/useAuth'
import type { AdminReviewFilters, CreateReviewPayload, ReviewModerationStatus } from '../types/review.types'

export function useOrderReviews(orderId: number) {
  const { user } = useAuth()
  return useQuery({ queryKey: ['reviews', 'order', orderId, user?.id], queryFn: () => reviewsApi.order(orderId), enabled: Boolean(user) && Number.isInteger(orderId) && orderId > 0 })
}

export function useCreateReview(orderId: number) {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ itemId, payload }: { itemId: number; payload: CreateReviewPayload }) => reviewsApi.create(orderId, itemId, payload),
    onSuccess: () => client.invalidateQueries({ queryKey: ['reviews'] }),
    onError: () => { void client.invalidateQueries({ queryKey: ['reviews', 'order', orderId] }) },
  })
}

export function useSellerReputation(sellerId: number) {
  return useQuery({ queryKey: ['reviews', 'reputation', sellerId], queryFn: () => reviewsApi.reputation(sellerId) })
}

export function useSellerReviews(sellerId: number, page: number) {
  return useQuery({ queryKey: ['reviews', 'seller', sellerId, page], queryFn: () => reviewsApi.publicReviews(sellerId, page) })
}

export function useAdminReviews(filters: AdminReviewFilters) {
  const { user } = useAuth()
  return useQuery({ queryKey: ['reviews', 'admin', user?.id, filters], queryFn: () => reviewsApi.admin(filters), enabled: user?.role === 'ADMIN' })
}

export function useModerateReview() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ reviewId, status, reason }: { reviewId: number; status: ReviewModerationStatus; reason: string }) => reviewsApi.moderate(reviewId, status, reason),
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ['reviews'] }),
        client.invalidateQueries({ queryKey: ['admin', 'audit'] }),
      ])
    },
  })
}
