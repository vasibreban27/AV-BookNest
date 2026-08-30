import { useDeferredValue, useState } from 'react'
import { Link } from 'react-router-dom'
import { useSupportAdministrators, useSupportList } from '../../features/support/hooks'
import { supportStatuses, type SupportStatus } from '../../features/support/types'
import { supportDate, supportError } from '../../features/support/utils'
import '../../styles/pages/support/support.css'

export function SupportPage({ admin = false }: { admin?: boolean }) {
  const [status, setStatus] = useState<SupportStatus | ''>('')
  const [search, setSearch] = useState('')
  const [assignment, setAssignment] = useState('')
  const [page, setPage] = useState(0)
  const q = useDeferredValue(search)
  const tickets = useSupportList(admin, { status, q, assignment, page })
  const administrators = useSupportAdministrators(admin)
  return <main className={`support-page${admin ? ' support-page--admin' : ''}`}>
    <header className="support-heading"><div><span className="support-eyebrow">{admin ? 'BookNest · Administrare' : 'Suntem aici pentru tine'}</span>
      <h1>{admin ? 'Suport clienți' : 'Solicitările mele'}</h1>
      <p>{admin ? 'Gestionează conversațiile, repartizează solicitările și urmărește rezolvarea lor.' : 'Toate conversațiile cu echipa BookNest, într-un singur loc.'}</p></div>
      {!admin && <Link className="support-button" to="/contact">Solicitare nouă</Link>}
    </header>
    <section className="support-panel" aria-label="Solicitări de suport">
      <div className="support-filters">
        {admin && <label>Caută solicitări<input type="search" maxLength={200} placeholder="Referință, subiect, nume sau email" value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} /></label>}
        <label>Stare<select value={status} onChange={e => { setStatus(e.target.value as SupportStatus | ''); setPage(0) }}><option value="">Toate stările</option>{Object.entries(supportStatuses).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        {admin && <label>Responsabil<select value={assignment} onChange={e => { setAssignment(e.target.value); setPage(0) }}><option value="">Toți administratorii</option><option value="unassigned">Nerepartizate</option>{administrators.data?.map(person => <option key={person.id} value={person.id}>{person.name}</option>)}</select></label>}
      </div>
      {tickets.isPending && <p role="status">Se încarcă solicitările…</p>}
      {tickets.isError && <div role="alert"><p>{supportError(tickets.error)}</p><button className="support-button support-button--secondary" onClick={() => void tickets.refetch()}>Reîncearcă</button></div>}
      {!tickets.isError && tickets.data?.content.length === 0 && <div className="support-empty"><h2>Nicio solicitare găsită</h2><p>{admin ? 'Nu există solicitări pentru filtrele selectate.' : 'Dacă ai nevoie de ajutor, trimite o solicitare nouă. Mesajele trimise fără autentificare nu apar în cont.'}</p></div>}
      <div className="support-ticket-list">{!tickets.isError && tickets.data?.content.map(entry => {
        const ticket = 'ticket' in entry ? entry.ticket : entry
        return <Link className="support-ticket-card" key={ticket.id} to={`${admin ? '/admin' : ''}/support/${ticket.id}`}>
          <div className="support-ticket-card__top"><small>{ticket.reference}</small><span className={`support-badge support-badge--${ticket.status.toLowerCase()}`}>{supportStatuses[ticket.status]}</span></div>
          <h2>{ticket.subject}</h2>
          {'ticket' in entry && <p>{entry.contactName} · {entry.assignedToName ?? 'Nerepartizată'}{entry.requesterId === null ? ' · Vizitator' : ''}</p>}
          <div className="support-ticket-card__bottom"><span>Actualizată {supportDate(ticket.updatedAt)}</span><strong>Vezi conversația →</strong></div>
        </Link>
      })}</div>
      {tickets.data && tickets.data.totalPages > 1 && <nav className="support-pagination" aria-label="Paginare solicitări"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Înapoi</button><span>Pagina {page + 1} din {tickets.data.totalPages}</span><button disabled={!tickets.data.hasNext} onClick={() => setPage(page + 1)}>Înainte</button></nav>}
    </section>
  </main>
}
