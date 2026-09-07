import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { reportDecisions, reportError, reportReasons, reportStatuses, reportsApi, type ContentReport, type ReportDecision } from '../../features/reports/api'
import { useModerationHistory, useReport, useReportAction } from '../../features/reports/hooks'
import { supportDate } from '../../features/support/utils'
import { useAuth } from '../../features/auth/hooks/useAuth'
import '../../styles/pages/reports/reports.css'

function EvidenceImage({ reportId, id, index }: { reportId: number; id: number; index: number }) {
  const [url, setUrl] = useState<string>()
  const [failed, setFailed] = useState(false)
  const [attempt, setAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    let objectUrl: string | undefined
    reportsApi.evidence(reportId, id, controller.signal).then(blob => {
      if (controller.signal.aborted) return
      objectUrl = URL.createObjectURL(blob)
      setUrl(objectUrl); setFailed(false)
    }).catch(() => { if (!controller.signal.aborted) setFailed(true) })
    return () => { controller.abort(); if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [reportId, id, attempt])
  return <figure>{url ? <a href={url} target="_blank" rel="noreferrer"><img src={url} alt={`Dovadă ${index + 1}`} /><figcaption>Deschide fotografia {index + 1}</figcaption></a> : failed ? <p role="alert">Fotografia nu poate fi încărcată. <button onClick={() => setAttempt(attempt + 1)}>Reîncearcă</button></p> : <p role="status">Încărcăm fotografia…</p>}</figure>
}
const historyLabels: Record<string, string> = { USER_SUSPENDED: 'Cont suspendat', USER_REACTIVATED: 'Cont reactivat', USER_SUSPENSION_EXPIRED: 'Suspendare expirată automat', REPORT_WARNING: 'Avertisment', REPORT_BOOK_HIDDEN: 'Anunț ascuns', REPORT_TEMPORARY_SUSPENSION: 'Suspendare temporară' }
export function ModerationHistory({ userId }: { userId: number }) {
  const history = useModerationHistory(userId)
  return <section className="report-card"><h2>Istoric de moderare · utilizator #{userId}</h2>
    {history.isPending && <p role="status">Încărcăm istoricul…</p>}
    {history.isError && <p role="alert">{reportError(history.error)} <button onClick={() => void history.refetch()}>Reîncearcă</button></p>}
    {history.data && <><p>Cont {history.data.enabled ? 'activ' : 'suspendat'}{history.data.suspendedUntil && ` până la ${supportDate(history.data.suspendedUntil)}`}.</p>
      {history.data.suspensionReason && <p>{history.data.suspensionReason}</p>}
      {!history.data.entries.length && <p>Nu există sancțiuni sau reactivări înregistrate.</p>}
      <ol className="report-timeline">{history.data.entries.map((entry, index) => <li key={index}><strong>{historyLabels[entry.action] ?? entry.action}</strong><p>{entry.reason}</p><small>{supportDate(entry.createdAt)}</small></li>)}</ol>
      <small>Ultimele 100 de acțiuni. Raportările respinse nu sunt abateri.</small></>}
    <p><Link to={`/admin/reports?targetUserId=${userId}`}>Vezi raportările acestui utilizator</Link></p>
  </section>
}
function DecisionForm({ report }: { report: ContentReport }) {
  const [decision, setDecision] = useState<ReportDecision>('DISMISS')
  const [note, setNote] = useState('')
  const [days, setDays] = useState(7)
  const [confirmed, setConfirmed] = useState(false)
  const { user } = useAuth()
  const mutation = useReportAction(report.id)
  return <section className="report-card"><h2>Decizie de moderare</h2>
    <p>{report.assignedToId ? `Responsabil: administrator #${report.assignedToId}.` : 'Raportarea nu a fost preluată.'}</p>
    {report.assignedToId !== user?.id && <button disabled={mutation.isPending} onClick={() => mutation.mutate('claim')}>{report.assignedToId ? 'Preia de la responsabilul curent' : 'Preia raportarea'}</button>}
    <form className="report-form" onSubmit={e => { e.preventDefault(); if (confirmed && note.trim()) mutation.mutate({ decision, note: note.trim(), ...(decision === 'SUSPEND' ? { suspensionDays: days } : {}) }) }}>
      <label>Acțiune<select value={decision} disabled={mutation.isPending} onChange={e => { setDecision(e.target.value as ReportDecision); setConfirmed(false) }}>{Object.entries(reportDecisions).filter(([key]) => key !== 'HIDE_BOOK' || report.bookId).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
      {decision === 'SUSPEND' && <><label>Durata suspendării (zile)<input type="number" min={1} max={90} required value={days} disabled={mutation.isPending} onChange={e => setDays(Number(e.target.value))} /></label><p>Accesul și sesiunile sunt blocate, iar anunțurile vizibile sunt ascunse. Reactivarea automată restaurează doar anunțurile ascunse din cauza suspendării. Un cont deja suspendat necesită verificare separată în Utilizatori.</p></>}
      <label>Motivul deciziei<textarea rows={4} required maxLength={500} value={note} disabled={mutation.isPending} onChange={e => setNote(e.target.value)} /></label>
      <small>Motivul este vizibil autorului raportării și, la aplicarea unei măsuri, utilizatorului vizat. Nu include identitatea raportorului sau informații interne.</small>
      <label className="report-confirm"><input type="checkbox" required checked={confirmed} disabled={mutation.isPending} onChange={e => setConfirmed(e.target.checked)} /><span>Am verificat dovezile și istoricul și confirm această decizie.</span></label>
      {mutation.isError && <p role="alert" className="report-error">{reportError(mutation.error)}</p>}
      <button className="report-primary" disabled={mutation.isPending || !confirmed || !note.trim()}>{mutation.isPending ? 'Se salvează…' : 'Confirmă decizia'}</button>
    </form>
  </section>
}
export function ReportDetailsPage({ admin = false }: { admin?: boolean }) {
  const { reportId } = useParams()
  const id = Number(reportId)
  const query = useReport(admin, id)
  const report = query.data?.report
  return <main className={`reports-page${admin ? ' reports-page--admin' : ''}`}>
    <Link to={admin ? '/admin/reports' : '/reports'}>← {admin ? 'Coada de moderare' : 'Raportările mele'}</Link>
    {(!Number.isSafeInteger(id) || id <= 0) ? <p role="alert">Identificator de raportare invalid.</p> : query.isPending && <p role="status">Încărcăm raportarea…</p>}
    {query.isError && <p role="alert">{reportError(query.error)} <button onClick={() => void query.refetch()}>Reîncearcă</button></p>}
    {report && <><header><span className="home-kicker">{admin ? 'Verificare conținut' : 'Raportare înregistrată'}</span><h1>Raportare #{report.id}</h1><p>{report.targetLabel}</p><span className={`report-status report-status--${report.status.toLowerCase()}`}>{reportStatuses[report.status]}</span></header>
      <div className="report-detail-grid"><div className="report-column"><section className="report-card"><h2>{reportReasons[report.reason]}</h2><p className="report-text">{report.description || 'Fără descriere suplimentară.'}</p><small>{supportDate(report.createdAt)}</small>
        <p>{report.targetType === 'BOOK' ? (report.bookId ? `Anunț #${report.bookId}` : 'Anunț șters; raportarea și dovezile sunt păstrate.') : `Utilizator #${report.targetUserId}`}</p>
        {report.bookId && <Link to={`/books/${report.bookId}`}>Vezi anunțul (dacă mai este public)</Link>}
        {admin && <p><Link to="/admin/users">Gestionare utilizatori</Link> · Utilizator vizat #{report.targetUserId} · Autor raportare #{report.reporterId}</p>}
      </section>
      <section className="report-card"><h2>Dovezi private</h2>{!query.data?.evidence.length && <p>Nu au fost atașate fotografii.</p>}<div className="report-evidence">{query.data?.evidence.map((item, index) => <EvidenceImage reportId={id} id={item.id} index={index} key={item.id} />)}</div></section>
      {report.decision && <section className="report-card" role="status"><h2>Raportare soluționată</h2><p>{report.decision === 'DISMISS' ? 'Raportarea a fost respinsă.' : 'Au fost luate măsuri de moderare.'}</p><p className="report-text">{report.decisionNote}</p>{report.suspendedUntil && <p>Termenul suspendării: {supportDate(report.suspendedUntil)}.</p>}</section>}
      {admin && <section className="report-card"><h2>Istoricul raportării</h2><ol className="report-timeline">{query.data?.events.map(event => <li key={event.id}><strong>{event.action === 'CREATED' ? 'Raportare creată' : event.action === 'IN_PROGRESS' ? 'Preluare' : reportDecisions[event.action as ReportDecision] ?? event.action}</strong><p>{event.note}</p><small>{supportDate(event.createdAt)} · Autor #{event.actorId}</small></li>)}</ol></section>}
      </div>{admin && <aside className="report-column">{(report.status === 'NEW' || report.status === 'IN_PROGRESS') && <DecisionForm report={report} key={report.id} />}<ModerationHistory userId={report.targetUserId} /></aside>}</div>
    </>}
  </main>
}
