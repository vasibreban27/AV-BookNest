import { useState } from 'react'
import { ReportLink } from '../reports/ReportLink'
import axios from 'axios'
import { useSellerReputation, useSellerReviews } from '../../features/reviews/hooks/useReviews'
import { formatRating } from '../../features/reviews/utils/reviewFormatters'
import { ReviewCard } from './ReviewCard'

export function SellerReputation({ sellerId }: { sellerId: number }) {
  const [page, setPage] = useState(0)
  const reputation = useSellerReputation(sellerId)
  const reviews = useSellerReviews(sellerId, page)
  const stats = reputation.data?.ratings

  // A buyer-only account has no public seller profile yet (e.g. the first visit to Sales).
  if (axios.isAxiosError(reputation.error) && reputation.error.response?.status === 404) return null

  return <section className="reviews-panel seller-reputation" aria-label="Reputația vânzătorului">
    <p><ReportLink targetType="USER" targetId={sellerId} ownerId={sellerId} /></p>
    <header className="seller-reputation__header"><div><span className="home-kicker">Încredere între cititori</span><h2>Reputația vânzătorului</h2>{reputation.data && <p>{reputation.data.sellerName}</p>}</div>
      {stats && <div className="seller-reputation__score"><strong><span aria-hidden="true">★ </span>{formatRating(stats.sellerRating)}<small> / 5</small></strong><span>{stats.reviewCount} {stats.reviewCount === 1 ? 'recenzie verificată' : 'recenzii verificate'}</span></div>}
    </header>
    {reputation.isPending && <p role="status">Încărcăm reputația…</p>}
    {reputation.isError && <p role="alert">Reputația nu poate fi încărcată. <button className="review-button review-button--secondary" onClick={() => void reputation.refetch()}>Reîncearcă</button></p>}
    {stats && stats.reviewCount > 0 && <dl className="seller-reputation__dimensions"><div><dt>Descriere fidelă</dt><dd>{formatRating(stats.descriptionRating)} / 5</dd></div><div><dt>Stare conformă anunțului</dt><dd>{formatRating(stats.conditionRating)} / 5</dd></div></dl>}
    <p className="reviews-panel__intro">Media notelor primite pentru fiecare carte cumpărată și livrată. Sunt incluse doar recenziile verificate, vizibile public; nu este o evaluare a conținutului literar.</p>
    {reviews.isPending && <p role="status">Încărcăm recenziile…</p>}
    {reviews.isError && <div role="alert"><p>Recenziile nu pot fi încărcate.</p><button className="review-button review-button--secondary" onClick={() => void reviews.refetch()}>Reîncearcă</button></div>}
    {reviews.data?.totalElements === 0 && <div className="reviews-empty"><strong>Încă nu există recenzii.</strong><p>Acest vânzător nu are încă evaluări verificate. Lipsa recenziilor nu înseamnă o reputație negativă.</p></div>}
    <div className="reviews-list">{reviews.data?.content.map((review) => <ReviewCard review={review} key={review.id} />)}</div>
    {reviews.data && (reviews.data.totalPages > 1 || page > 0) && <nav className="reviews-pagination" aria-label="Paginare recenzii">
      <button className="review-button review-button--secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button>
      <span>Pagina {page + 1} din {Math.max(1, reviews.data.totalPages)}</span>
      <button className="review-button review-button--secondary" disabled={!reviews.data.hasNext} onClick={() => setPage(page + 1)}>Următor</button>
    </nav>}
  </section>
}
