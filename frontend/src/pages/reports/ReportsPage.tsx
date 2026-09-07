import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useReports } from '../../features/reports/hooks'
import { reportError, reportReasons, reportStatuses, type ReportStatus } from '../../features/reports/api'
import { referenceId, supportDate } from '../../features/support/utils'
import '../../styles/pages/reports/reports.css'

export function ReportsPage({ admin = false }: { admin?: boolean }) {
  const [params] = useSearchParams()
  const [status, setStatus] = useState<ReportStatus | ''>(admin && !params.get('targetUserId') ? 'NEW' : '')
  const [page, setPage] = useState(0)
  const targetUser = admin ? referenceId(params.get('targetUserId')) : undefined
  const reports = useReports(admin, page, status, targetUser)
  return <main className={`reports-page${admin ? ' reports-page--admin' : ''}`}>
    <header><span className="home-kicker">Siguranța comunității</span><h1>{admin ? 'Coada de moderare' : 'Raportările mele'}</h1><p>{admin ? 'Verifică dovezile și istoricul înainte de a lua o decizie. O sesizare nu este o abatere confirmată.' : 'Urmărește verificarea conținutului semnalat. Pentru o raportare nouă, folosește butonul de lângă anunț sau utilizator.'}</p></header>
    {targetUser && <p>Raportări pentru utilizatorul #{targetUser}. <Link to="/admin/reports" onClick={() => setPage(0)}>Elimină filtrul de utilizator</Link></p>}
    {admin && <label className="report-filter">Starea raportării<select value={status} onChange={e => { setStatus(e.target.value as ReportStatus | ''); setPage(0) }}><option value="">Toate</option>{Object.entries(reportStatuses).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>}
    {reports.isPending && <p role="status">Încărcăm raportările…</p>}
    {reports.isError && <p role="alert">{reportError(reports.error)} <button onClick={() => void reports.refetch()}>Reîncearcă</button></p>}
    {reports.data?.content.length === 0 && <section className="report-card"><h2>Nicio raportare găsită</h2><p>Nu există raportări pentru selecția curentă.</p></section>}
    <div className="report-list">{reports.data?.content.map(report => <article className="report-card" key={report.id}>
      <span className={`report-status report-status--${report.status.toLowerCase()}`}>{reportStatuses[report.status]}</span>
      <h2><Link to={`${admin ? '/admin' : ''}/reports/${report.id}`}>#{report.id} · {report.targetLabel}</Link></h2>
      <p>{report.targetType === 'BOOK' ? 'Anunț' : 'Utilizator'} · {reportReasons[report.reason]}</p>
      <small>{supportDate(report.createdAt)}{admin && report.assignedToId ? ` · Responsabil #${report.assignedToId}` : ''}</small>
    </article>)}</div>
    {reports.data && (reports.data.totalPages > 1 || page > 0) && <nav className="report-actions" aria-label="Paginare raportări"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button><span>Pagina {page + 1} din {Math.max(1, reports.data.totalPages)}</span><button disabled={!reports.data.hasNext} onClick={() => setPage(page + 1)}>Următor</button></nav>}
  </main>
}
