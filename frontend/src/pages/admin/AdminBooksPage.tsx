import { useDeferredValue, useState, type FormEvent } from 'react'
import {
  AdminBadge,
  AdminModal,
  AdminNotice,
  AdminPageHeader,
  AdminPagination,
  AdminPanel,
  AdminState,
  ReasonDialog,
} from '../../components/admin/AdminUi'
import { useAdminBooks, useHideAdminBook, useRestoreAdminBook } from '../../features/admin/hooks/useAdmin'
import type { AdminBook, BookModerationReason, BookModerationStatus } from '../../features/admin/types/admin.types'
import { formatAdminDate, formatAdminMoney, getAdminErrorMessage, moderationReasonLabels, moderationStatusLabels } from '../../features/admin/utils/adminFormatters'
import type { BookStatus } from '../../features/catalog/types/catalog.types'

const bookStatusLabels: Record<BookStatus, string> = { DRAFT: 'Ciornă', AVAILABLE: 'Disponibil', RESERVED: 'Rezervat', SOLD: 'Vândut', ARCHIVED: 'Arhivat' }

export function AdminBooksPage() {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<BookStatus | ''>('')
  const [moderation, setModeration] = useState<BookModerationStatus | ''>('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<AdminBook | null>(null)
  const [hideTarget, setHideTarget] = useState<AdminBook | null>(null)
  const [restoreTarget, setRestoreTarget] = useState<AdminBook | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const deferredQuery = useDeferredValue(query)
  const books = useAdminBooks({ query: deferredQuery, status: status || undefined, moderationStatus: moderation || undefined, page, size: 25 })
  const hideBook = useHideAdminBook()
  const restoreBook = useRestoreAdminBook()

  const handleHide = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!hideTarget) return
    const form = new FormData(event.currentTarget)
    setError(null)
    try {
      await hideBook.mutateAsync({ bookId: hideTarget.book.id, payload: { reason: String(form.get('reason')) as BookModerationReason, note: String(form.get('note')).trim() } })
      setHideTarget(null); setSelected(null); setNotice('Anunțul a fost ascuns din catalog și acțiunea a fost înregistrată.')
    } catch (mutationError) { setError(getAdminErrorMessage(mutationError)) }
  }

  const handleRestore = async (reason: string) => {
    if (!restoreTarget) return
    setError(null)
    try {
      await restoreBook.mutateAsync({ bookId: restoreTarget.book.id, reason })
      setRestoreTarget(null); setSelected(null); setNotice('Anunțul a fost restaurat în catalog.')
    } catch (mutationError) { setError(getAdminErrorMessage(mutationError)) }
  }

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Catalog" title="Moderarea anunțurilor" description="Starea comercială rămâne intactă; moderarea decide separat dacă un anunț este vizibil public." />
      {notice && <AdminNotice tone="success" onClose={() => setNotice(null)}>{notice}</AdminNotice>}
      <AdminPanel title="Anunțuri" description="Caută după titlu, autor sau emailul vânzătorului.">
        <div className="admin-filters admin-filters--three">
          <label className="admin-field admin-field--search"><span>Caută</span><input type="search" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} placeholder="Titlu, autor sau vânzător" /></label>
          <label className="admin-field"><span>Status comercial</span><select value={status} onChange={(event) => { setStatus(event.target.value as BookStatus | ''); setPage(0) }}><option value="">Toate</option>{Object.entries(bookStatusLabels).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
          <label className="admin-field"><span>Moderare</span><select value={moderation} onChange={(event) => { setModeration(event.target.value as BookModerationStatus | ''); setPage(0) }}><option value="">Toate</option><option value="VISIBLE">Vizibile</option><option value="HIDDEN">Ascunse</option></select></label>
        </div>
        {books.isLoading && <AdminState kind="loading" title="Încărcăm anunțurile" message="Aplicăm filtrele selectate." />}
        {books.isError && <AdminState kind="error" title="Anunțurile nu pot fi încărcate" message={getAdminErrorMessage(books.error)} onRetry={() => void books.refetch()} />}
        {books.data?.content.length === 0 && <AdminState kind="empty" title="Niciun anunț găsit" message="Încearcă alte filtre de căutare." />}
        {books.data && books.data.content.length > 0 && <>
          <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Carte</th><th>Vânzător</th><th>Preț</th><th>Status</th><th>Moderare</th><th><span className="sr-only">Acțiuni</span></th></tr></thead><tbody>{books.data.content.map((entry) => <tr key={entry.book.id}>
            <td data-label="Carte"><button className="admin-book-cell" type="button" onClick={() => setSelected(entry)}>{entry.book.coverImageUrl ? <img src={entry.book.coverImageUrl} alt="" /> : <span aria-hidden="true">BN</span>}<span><strong>{entry.book.title}</strong><small>{entry.book.author} · #{entry.book.id}</small></span></button></td>
            <td data-label="Vânzător">{entry.book.sellerName}<small className="admin-table__subline">ID {entry.book.sellerId}</small></td>
            <td data-label="Preț">{formatAdminMoney(entry.book.price)}</td>
            <td data-label="Status"><AdminBadge tone={entry.book.status === 'AVAILABLE' ? 'success' : 'neutral'}>{bookStatusLabels[entry.book.status]}</AdminBadge></td>
            <td data-label="Moderare"><AdminBadge tone={entry.moderationStatus === 'VISIBLE' ? 'success' : 'danger'}>{moderationStatusLabels[entry.moderationStatus]}</AdminBadge></td>
            <td><button className="admin-button admin-button--compact admin-button--secondary" type="button" onClick={() => setSelected(entry)}>Detalii</button></td>
          </tr>)}</tbody></table></div>
          <AdminPagination page={books.data.page} totalPages={books.data.totalPages} totalElements={books.data.totalElements} onPageChange={setPage} />
        </>}
      </AdminPanel>

      {selected && <AdminModal title={selected.book.title} description={`Anunț #${selected.book.id} · ${selected.book.author}`} onClose={() => { setSelected(null); setError(null) }} size="large">
        <div className="admin-book-detail">
          <div className="admin-book-detail__cover">{selected.book.coverImageUrl ? <img src={selected.book.coverImageUrl} alt={`Coperta ${selected.book.title}`} /> : <span>Fără copertă</span>}</div>
          <div className="admin-book-detail__content"><div className="admin-badge-row"><AdminBadge tone={selected.moderationStatus === 'VISIBLE' ? 'success' : 'danger'}>{moderationStatusLabels[selected.moderationStatus]}</AdminBadge><AdminBadge>{bookStatusLabels[selected.book.status]}</AdminBadge></div><h3>{selected.book.title}</h3><p>{selected.book.description || 'Anunțul nu are descriere.'}</p><dl className="admin-detail-grid"><div><dt>Vânzător</dt><dd>{selected.book.sellerName}</dd></div><div><dt>Categorie</dt><dd>{selected.book.category.name}</dd></div><div><dt>Preț</dt><dd>{formatAdminMoney(selected.book.price)}</dd></div><div><dt>Publicat</dt><dd>{formatAdminDate(selected.book.createdAt)}</dd></div></dl>
            {selected.moderationReason && <div className="admin-callout admin-callout--danger"><strong>{moderationReasonLabels[selected.moderationReason]}</strong><p>{selected.moderationNote}</p><small>{formatAdminDate(selected.moderatedAt)}</small></div>}
            {error && <AdminNotice tone="error">{error}</AdminNotice>}
            <div className="admin-detail__actions">{selected.moderationStatus === 'VISIBLE' ? <button type="button" className="admin-button admin-button--danger" onClick={() => { setHideTarget(selected); setError(null) }}>Ascunde anunțul</button> : <button type="button" className="admin-button" onClick={() => { setRestoreTarget(selected); setError(null) }}>Restaurează anunțul</button>}</div>
          </div>
        </div>
      </AdminModal>}

      {hideTarget && <AdminModal title="Ascunde anunțul" description="Anunțul va dispărea imediat din catalog, fără să-i fie schimbat statusul comercial." onClose={() => { setHideTarget(null); setError(null) }} size="small"><form className="admin-form" onSubmit={(event) => void handleHide(event)}><label><span>Motiv de moderare</span><select name="reason" required defaultValue="POLICY_VIOLATION">{Object.entries(moderationReasonLabels).filter(([value]) => value !== 'ACCOUNT_SUSPENDED').map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label><label><span>Notă pentru audit</span><textarea name="note" required maxLength={500} rows={4} placeholder="Descrie concret problema identificată..." /></label>{error && <AdminNotice tone="error">{error}</AdminNotice>}<div className="admin-form__actions"><button type="button" className="admin-button admin-button--ghost" onClick={() => setHideTarget(null)}>Renunță</button><button type="submit" className="admin-button admin-button--danger" disabled={hideBook.isPending}>{hideBook.isPending ? 'Se ascunde...' : 'Ascunde anunțul'}</button></div></form></AdminModal>}
      {restoreTarget && <ReasonDialog title="Restaurează anunțul" description="Anunțul va redeveni vizibil dacă statusul comercial și categoria permit acest lucru." confirmLabel="Restaurează" pending={restoreBook.isPending} error={error} onClose={() => { setRestoreTarget(null); setError(null) }} onConfirm={(reason) => void handleRestore(reason)} />}
    </main>
  )
}
