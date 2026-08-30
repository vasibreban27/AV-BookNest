import type { Review } from '../../features/reviews/types/review.types'
import '../../styles/components/reviews/reviews.css'

export function ReviewCard({ review }: { review: Review }) {
  return <article className="review-card">
    <header className="review-card__header">
      <div><strong>{review.reviewerName}</strong><span className="review-card__date">{new Intl.DateTimeFormat('ro-RO', { dateStyle: 'medium' }).format(new Date(review.createdAt))}</span></div>
      {review.verifiedPurchase && <span className="review-verified">✓ Achiziție verificată</span>}
    </header>
    <p className="review-card__book">{review.bookTitle}</p>
    <dl className="review-card__ratings">
      <div><dt>Vânzător</dt><dd><span aria-hidden="true">★ </span>{review.sellerRating}/5</dd></div>
      {review.descriptionRating != null && <div><dt>Descriere fidelă</dt><dd>{review.descriptionRating}/5</dd></div>}
      {review.conditionRating != null && <div><dt>Stare conformă</dt><dd>{review.conditionRating}/5</dd></div>}
    </dl>
    {review.comment && <p className="review-card__comment">{review.comment}</p>}
  </article>
}
