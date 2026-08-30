import { useState } from 'react'
import { useOrderReviews } from '../../features/reviews/hooks/useReviews'
import { reviewError } from '../../features/reviews/utils/reviewFormatters'
import { ReviewCard } from './ReviewCard'
import { ReviewForm } from './ReviewForm'

export function OrderReviews({ orderId }: { orderId: number }) {
  const query = useOrderReviews(orderId)
  const [selectedItem, setSelectedItem] = useState<number | null>(null)
  return <section className="reviews-panel" aria-labelledby="order-reviews-title">
    <span className="home-kicker">Părerea ta contează</span>
    <h2 id="order-reviews-title">Recenziile achizițiilor</h2>
    <p className="reviews-panel__intro">Evaluează fiecare carte după livrare. Feedbackul tău îi ajută pe ceilalți cititori să aleagă cu încredere.</p>
    {query.isPending && <p role="status">Verificăm achizițiile eligibile…</p>}
    {query.isError && <div role="alert"><p>{reviewError(query.error)}</p><button className="review-button review-button--secondary" onClick={() => void query.refetch()}>Reîncearcă</button></div>}
    {query.data?.length === 0 && <p>Nu există cărți de evaluat în această comandă.</p>}
    {query.data?.map((item) => <div className="order-review" key={item.orderItemId}>
      {item.existingReview ? <>
        <ReviewCard review={item.existingReview.review} />
        {item.existingReview.moderationStatus === 'HIDDEN' ? <p className="review-moderation-note">Recenzia ta este ascunsă și nu intră în reputație. Motiv: {item.existingReview.moderationReason}</p> : <p className="review-success" role="status">Recenzia ta este publicată. Mulțumim!</p>}
      </> : <>
        <h3>{item.bookTitle}</h3>
        {!item.canReview && <p>{item.ineligibilityReason === 'SELF_REVIEW' ? 'Nu poți evalua propriile anunțuri.' : 'Vei putea scrie o recenzie după confirmarea livrării acestui colet.'}</p>}
        {item.canReview && (selectedItem === item.orderItemId ? <ReviewForm orderId={orderId} item={item} /> : <button className="review-button review-button--secondary" type="button" onClick={() => setSelectedItem(item.orderItemId)}>Scrie o recenzie</button>)}
      </>}
    </div>)}
  </section>
}
