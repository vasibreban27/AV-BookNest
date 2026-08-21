import { useState, type FormEvent } from 'react'
import { AdminBadge, AdminModal, AdminNotice, AdminPageHeader, AdminPagination, AdminPanel, AdminState } from '../../components/admin/AdminUi'
import { useAdminIssues, useResolveAdminIssue } from '../../features/admin/hooks/useAdmin'
import type { AdminIssue, IssueResolution } from '../../features/admin/types/admin.types'
import { formatAdminDate, formatAdminMoney, getAdminErrorMessage, issueResolutionLabels } from '../../features/admin/utils/adminFormatters'
import type { OrderIssueStatus } from '../../features/orders/types/orders.types'
import { formatSellerOrderStatus } from '../../features/orders/utils/orderFormatters'

export function AdminIssuesPage() {
  const [status, setStatus] = useState<OrderIssueStatus>('OPEN')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<AdminIssue | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const issues = useAdminIssues({ status, page, size: 25 })
  const resolveIssue = useResolveAdminIssue()

  const handleResolve = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selected) return
    const form = new FormData(event.currentTarget)
    const resolution = String(form.get('resolution')) as IssueResolution
    const note = String(form.get('note')).trim()
    setError(null)
    try {
      await resolveIssue.mutateAsync({ sellerOrderId: selected.sellerOrderId, payload: { resolution, note } })
      setNotice(resolution === 'REFUND_BUYER' ? 'Disputa a fost soluționată și rambursarea a fost trimisă în coada Stripe.' : 'Disputa a fost soluționată, iar plata vânzătorului a fost deblocată.')
      setSelected(null)
    } catch (mutationError) { setError(getAdminErrorMessage(mutationError)) }
  }

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Rezoluții" title="Dispute" description="Analizează sesizarea și alege un rezultat sigur: rambursarea cumpărătorului sau eliberarea plății vânzătorului." />
      {notice && <AdminNotice tone="success" onClose={() => setNotice(null)}>{notice}</AdminNotice>}
      <AdminPanel title="Cazuri" description="Fiecare decizie este definitivă, notificată ambelor părți și salvată în audit." actions={<div className="admin-segmented"><button type="button" className={status === 'OPEN' ? 'active' : ''} onClick={() => { setStatus('OPEN'); setPage(0) }}>Deschise</button><button type="button" className={status === 'RESOLVED' ? 'active' : ''} onClick={() => { setStatus('RESOLVED'); setPage(0) }}>Soluționate</button></div>}>
        {issues.isLoading && <AdminState kind="loading" title="Încărcăm disputele" message="Pregătim cazurile pentru analiză." />}
        {issues.isError && <AdminState kind="error" title="Disputele nu pot fi încărcate" message={getAdminErrorMessage(issues.error)} onRetry={() => void issues.refetch()} />}
        {issues.data?.content.length === 0 && <AdminState kind="empty" title={status === 'OPEN' ? 'Nu există dispute deschise' : 'Nu există dispute soluționate'} message={status === 'OPEN' ? 'Coada este curată.' : 'Deciziile finalizate vor apărea aici.'} />}
        {issues.data && issues.data.content.length > 0 && <><div className="admin-issue-list">{issues.data.content.map((issue) => <article key={issue.sellerOrderId}>
          <header><span><small>{issue.orderNumber}</small><strong>Sesizare #{issue.sellerOrderId}</strong></span><AdminBadge tone={issue.issueStatus === 'OPEN' ? 'danger' : 'success'}>{issue.issueStatus === 'OPEN' ? 'Necesită decizie' : 'Soluționată'}</AdminBadge></header>
          <blockquote>{issue.issueReason || 'Cumpărătorul nu a furnizat detalii.'}</blockquote>
          <div className="admin-issue-parties"><span><small>Cumpărător</small><strong>{issue.buyerEmail}</strong></span><i aria-hidden="true">↔</i><span><small>Vânzător</small><strong>{issue.sellerEmail}</strong></span></div>
          <dl><div><dt>Valoare articole</dt><dd>{formatAdminMoney(issue.itemSubtotal)}</dd></div><div><dt>Încasare vânzător</dt><dd>{formatAdminMoney(issue.sellerProceeds)}</dd></div><div><dt>Status vânzare</dt><dd>{formatSellerOrderStatus(issue.sellerOrderStatus)}</dd></div><div><dt>Deschisă</dt><dd>{formatAdminDate(issue.issueOpenedAt)}</dd></div></dl>
          {issue.resolution && <div className="admin-callout"><strong>{issueResolutionLabels[issue.resolution]}</strong><p>{issue.resolutionNote}</p><small>{formatAdminDate(issue.issueResolvedAt)}</small></div>}
          {issue.issueStatus === 'OPEN' && <footer><button type="button" className="admin-button" onClick={() => { setSelected(issue); setError(null) }}>Soluționează cazul</button></footer>}
        </article>)}</div><AdminPagination page={issues.data.page} totalPages={issues.data.totalPages} totalElements={issues.data.totalElements} onPageChange={setPage} /></>}
      </AdminPanel>

      {selected && <AdminModal title="Decizie asupra disputei" description={`${selected.orderNumber} · cumpărător ${selected.buyerEmail}`} onClose={() => { setSelected(null); setError(null) }} size="medium"><form className="admin-form" onSubmit={(event) => void handleResolve(event)}>
        <div className="admin-callout admin-callout--warning"><strong>Verifică decizia cu atenție</strong><p>Backend-ul împiedică rambursarea dacă transferul către vânzător a fost deja creat.</p></div>
        <fieldset className="admin-choice"><legend>Rezoluție</legend><label><input type="radio" name="resolution" value="REFUND_BUYER" defaultChecked /><span><strong>Rambursează cumpărătorul</strong><small>Stripe va procesa rambursarea, iar payout-ul va fi blocat permanent.</small></span></label><label><input type="radio" name="resolution" value="RELEASE_SELLER_PAYOUT" /><span><strong>Eliberează plata vânzătorului</strong><small>Reținerea cauzată de dispută va fi eliminată.</small></span></label></fieldset>
        <label><span>Motivarea deciziei</span><textarea name="note" required maxLength={500} rows={5} placeholder="Documentează faptele analizate și motivul deciziei..." /></label>
        {error && <AdminNotice tone="error">{error}</AdminNotice>}
        <div className="admin-form__actions"><button type="button" className="admin-button admin-button--ghost" onClick={() => setSelected(null)}>Renunță</button><button type="submit" className="admin-button" disabled={resolveIssue.isPending}>{resolveIssue.isPending ? 'Se finalizează...' : 'Confirmă decizia'}</button></div>
      </form></AdminModal>}
    </main>
  )
}
