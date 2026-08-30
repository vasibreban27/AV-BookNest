import { useId, useState, type FormEvent } from 'react'
import { useCreateReview } from '../../features/reviews/hooks/useReviews'
import type { OrderReview } from '../../features/reviews/types/review.types'
import { reviewError } from '../../features/reviews/utils/reviewFormatters'

function RatingInput({ label, name, hint }: { label: string; name: string; hint: string }) {
  const id = useId()
  const [rating, setRating] = useState(0)
  return <fieldset className="review-rating-input" aria-describedby={`${id}-hint`}>
    <legend>{label} <span aria-hidden="true">*</span></legend>
    <p id={`${id}-hint`}>{hint}</p>
    <div className="review-rating-input__options">
      {[1, 2, 3, 4, 5].map((value) => <label key={value} className={value <= rating ? 'is-selected' : ''}>
        <input type="radio" name={name} value={value} required checked={rating === value} onChange={() => setRating(value)} aria-label={`${value} din 5`} />
        <span aria-hidden="true">★</span><small aria-hidden="true">{value}</small>
      </label>)}
    </div>
  </fieldset>
}

export function ReviewForm({ orderId, item }: { orderId: number; item: OrderReview }) {
  const mutation = useCreateReview(orderId)
  const commentId = useId()
  const [comment, setComment] = useState('')
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    mutation.mutate({ itemId: item.orderItemId, payload: {
      sellerRating: Number(data.get('sellerRating')),
      descriptionRating: Number(data.get('descriptionRating')),
      conditionRating: Number(data.get('conditionRating')),
      comment: comment.trim() || null,
    } })
  }

  if (mutation.isSuccess) return <p className="review-success" role="status">Mulțumim! Recenzia ta a fost publicată.</p>

  return <form className="review-form" onSubmit={submit} aria-label={`Recenzie pentru ${item.bookTitle}`}>
    <fieldset className="review-form__fields" disabled={mutation.isPending}>
      <RatingInput name="sellerRating" label="Experiența cu vânzătorul" hint="Cum a fost această achiziție, în ansamblu?" />
      <RatingInput name="descriptionRating" label="Corectitudinea descrierii" hint="Cât de bine corespunde cartea descrierii din anunț?" />
      <RatingInput name="conditionRating" label="Starea cărții" hint="Starea primită corespunde celei declarate de vânzător?" />
      <label className="review-form__comment" htmlFor={commentId}>Comentariu (opțional)</label>
      <textarea id={commentId} value={comment} onChange={(event) => setComment(event.target.value)} maxLength={2000} rows={4} placeholder="Ce ar fi util să știe următorul cumpărător?" aria-describedby={`${commentId}-help`} />
      <small id={`${commentId}-help`}>{comment.length}/2000 · Nu include date personale. Recenzia nu poate fi modificată după publicare.</small>
      <p className="review-form__privacy">Publicăm prenumele și inițiala numelui tău, fără datele comenzii. Notele sunt obligatorii: 1 = foarte slab, 5 = excelent.</p>
      <button className="review-button" type="submit">{mutation.isPending ? 'Se publică…' : 'Publică recenzia'}</button>
    </fieldset>
    {mutation.isError && <p className="review-error" role="alert">{reviewError(mutation.error)}</p>}
  </form>
}
