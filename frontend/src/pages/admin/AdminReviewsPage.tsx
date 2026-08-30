import { useDeferredValue, useState } from 'react'
import { AdminBadge, AdminNotice, AdminPageHeader, AdminPagination, AdminPanel, AdminState } from '../../components/admin/AdminUi'
import { ReviewCard } from '../../components/reviews/ReviewCard'
import { useAdminReviews, useModerateReview } from '../../features/reviews/hooks/useReviews'
import type { PrivateReview, ReviewModerationStatus } from '../../features/reviews/types/review.types'
import { reviewError } from '../../features/reviews/utils/reviewFormatters'

export function AdminReviewsPage() {
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<ReviewModerationStatus | ''>('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<PrivateReview | null>(null)
  const [reason, setReason] = useState('')
  const [notice, setNotice] = useState<string | null>(null)
  const q = useDeferredValue(search)
  const reviews = useAdminReviews({ q, status, page })
  const mutation = useModerateReview()

  const confirm = async () => {
    if (!selected || !reason.trim()) return
    const nextStatus = selected.moderationStatus === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE'
    try {
      await mutation.mutateAsync({ reviewId: selected.review.id, status: nextStatus, reason: reason.trim() })
      setSelected(null)
      setReason('')
      setNotice(nextStatus === 'HIDDEN' ? 'Recenzia a fost ascunsă și exclusă din reputație. Acțiunea este în audit.' : 'Recenzia a fost restaurată și inclusă în reputație. Acțiunea este în audit.')
    } catch { /* The mutation state renders the retryable error next to the form. */ }
  }

  return <main className="admin-page">
    <AdminPageHeader eyebrow="Încredere" title="Recenzii și reputație" description="Moderează feedbackul fără a schimba notele cumpărătorilor. Recenziile ascunse nu intră în mediile publice." />
    {notice && <AdminNotice tone="success" onClose={() => setNotice(null)}>{notice}</AdminNotice>}
    <AdminPanel title="Recenziile cumpărătorilor" description="Caută după carte, cumpărător sau vânzător. Feedbackul vechi, neverificat, nu poate fi publicat.">
      <div className="admin-filters">
        <label className="admin-field admin-field--search"><span>Caută recenzii</span><input type="search" value={search} maxLength={200} placeholder="Carte, cumpărător, vânzător" onChange={(event) => { setSearch(event.target.value); setPage(0) }} /></label>
        <label className="admin-field"><span>Vizibilitate</span><select value={status} onChange={(event) => { setStatus(event.target.value as ReviewModerationStatus | ''); setPage(0) }}><option value="">Toate</option><option value="VISIBLE">Vizibile</option><option value="HIDDEN">Ascunse</option></select></label>
      </div>
      {reviews.isPending && <AdminState kind="loading" title="Încărcăm recenziile" message="Pregătim feedbackul cumpărătorilor." />}
      {reviews.isError && <AdminState kind="error" title="Recenziile nu pot fi încărcate" message={reviewError(reviews.error)} onRetry={() => void reviews.refetch()} />}
      {reviews.data?.content.length === 0 && <AdminState kind="empty" title="Nicio recenzie găsită" message="Nu există recenzii pentru filtrele selectate." />}
      <div className="admin-reviews-list">{reviews.data?.content.map((entry) => <section className="admin-review-entry" key={entry.review.id}>
        <div className="admin-review-entry__meta"><AdminBadge tone={entry.moderationStatus === 'VISIBLE' ? 'success' : 'danger'}>{entry.moderationStatus === 'VISIBLE' ? 'Vizibilă' : 'Ascunsă'}</AdminBadge><span>Recenzie #{entry.review.id} · Vânzător #{entry.review.sellerId}{entry.orderItemId ? ` · Articol comandă #${entry.orderItemId}` : ' · Achiziție neverificată'}</span></div>
        <ReviewCard review={entry.review} />
        {entry.moderationReason && <p className="review-moderation-note">Ultimul motiv de moderare: {entry.moderationReason}</p>}
        {selected?.review.id === entry.review.id ? <form className="admin-form admin-review-form" onSubmit={(event) => { event.preventDefault(); void confirm() }}>
          <label><span>Motivul moderării</span><textarea required maxLength={500} value={reason} rows={3} disabled={mutation.isPending} onChange={(event) => setReason(event.target.value)} aria-describedby={`moderation-help-${entry.review.id}`} /></label>
          <small id={`moderation-help-${entry.review.id}`}>Motivul este vizibil cumpărătorului și păstrat în audit. Nu include informații interne sau date personale.</small>
          {mutation.isError && <AdminNotice tone="error">{reviewError(mutation.error)}</AdminNotice>}
          <div className="admin-form__actions"><button className="admin-button admin-button--secondary" type="button" disabled={mutation.isPending} onClick={() => setSelected(null)}>Renunță</button><button className="admin-button" type="submit" disabled={mutation.isPending || !reason.trim()}>{mutation.isPending ? 'Se salvează…' : entry.moderationStatus === 'VISIBLE' ? 'Confirmă ascunderea' : 'Confirmă restaurarea'}</button></div>
        </form> : entry.review.verifiedPurchase && <button className="admin-button admin-button--secondary" type="button" disabled={mutation.isPending} onClick={() => { setSelected(entry); setReason(''); mutation.reset() }}>{entry.moderationStatus === 'VISIBLE' ? 'Ascunde recenzia' : 'Restaurează recenzia'}</button>}
      </section>)}</div>
      {reviews.data && <AdminPagination page={reviews.data.page} totalPages={reviews.data.totalPages} totalElements={reviews.data.totalElements} onPageChange={setPage} />}
    </AdminPanel>
  </main>
}
