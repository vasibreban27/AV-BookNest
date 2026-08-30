import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useSupportAction, useSupportAdministrators, useSupportTicket } from '../../features/support/hooks'
import { supportStatuses, type AdminTicket, type SupportAction, type SupportStatus } from '../../features/support/types'
import { referenceId, supportDate, supportError, unwrapTicket } from '../../features/support/utils'
import '../../styles/pages/support/support.css'

export function SupportTicketPage({ admin = false }: { admin?: boolean }) {
  const { ticketId } = useParams()
  const id = referenceId(ticketId ?? null)
  if (!id) return <main className="support-page"><p role="alert">Referința solicitării nu este validă.</p><Link to={admin ? '/admin/support' : '/support'}>Înapoi la solicitări</Link></main>
  return <TicketConversation key={`${admin}-${id}`} id={id} admin={admin} />
}

function TicketConversation({ id, admin }: { id: number; admin: boolean }) {
  const [page, setPage] = useState(0)
  const [body, setBody] = useState('')
  const [notice, setNotice] = useState('')
  const { ticket: query, messages } = useSupportTicket(id, admin, page)
  const mutation = useSupportAction(id, admin)
  const entry = query.data
  const ticket = entry ? unwrapTicket(entry) : undefined
  const execute = async (action: SupportAction) => {
    setNotice('')
    try {
      await mutation.mutateAsync(action)
      if (action.kind === 'reply') { setBody(''); setPage(0) }
      setNotice(action.kind === 'reply' ? 'Răspunsul a fost salvat în conversație.' : action.kind === 'retry' ? 'Emailul a fost pus în coada de trimitere.' : 'Solicitarea a fost actualizată.')
      return true
    } catch { return false }
  }
  const submit = (event: FormEvent) => { event.preventDefault(); if (body.trim()) void execute({ kind: 'reply', body: body.trim() }) }

  return <main className={`support-page${admin ? ' support-page--admin' : ''}`}>
    <Link className="support-back" to={admin ? '/admin/support' : '/support'}>← Înapoi la solicitări</Link>
    {query.isPending && <p role="status">Se încarcă solicitarea…</p>}
    {query.isError && <div className="support-panel" role="alert"><p>{supportError(query.error)}</p><button className="support-button" onClick={() => void query.refetch()}>Reîncearcă</button></div>}
    {ticket && !query.isError && <>
      <header className="support-heading"><div><span className="support-eyebrow">{ticket.reference}</span><h1>{ticket.subject}</h1><p>Deschisă {supportDate(ticket.createdAt)}</p></div><span className={`support-badge support-badge--${ticket.status.toLowerCase()}`}>{supportStatuses[ticket.status]}</span></header>
      <div className="support-context">
        {ticket.orderId && (admin ? <Link to="/admin/orders">Comandă #{ticket.orderId}</Link> : <span>Comandă #{ticket.orderId}</span>)}
        {ticket.bookId && <span>Anunț #{ticket.bookId}</span>}
        {ticket.status === 'RESOLVED' && <p>Un răspuns nou redeschide automat solicitarea.</p>}
      </div>
      {notice && <p className="support-notice" role="status">{notice}</p>}
      {mutation.isError && <p className="support-error" role="alert">{supportError(mutation.error)}</p>}
      <div className={`support-conversation-layout${admin ? ' support-conversation-layout--admin' : ''}`}>
        <section className="support-panel support-conversation" aria-label="Conversație cu suportul">
          <h2>Conversație</h2>
          {messages.isPending && <p role="status">Se încarcă mesajele…</p>}
          {messages.isError && <div role="alert"><p>{supportError(messages.error)}</p><button className="support-button support-button--secondary" onClick={() => void messages.refetch()}>Reîncarcă mesajele</button></div>}
          {messages.data && messages.data.totalPages > 1 && <nav className="support-pagination" aria-label="Paginare conversație"><button disabled={!messages.data.hasNext} onClick={() => setPage(page + 1)}>Mesaje mai vechi</button><span>{page === 0 ? 'Cele mai recente' : `Istoric · pagina ${page + 1}`}</span><button disabled={page === 0} onClick={() => setPage(page - 1)}>Mesaje mai noi</button></nav>}
          <div className="support-messages">{!messages.isError && [...(messages.data?.content ?? [])].reverse().map(row => {
            const message = 'message' in row ? row.message : row
            const delivery = 'message' in row ? row : null
            return <article className={`support-message support-message--${message.kind.toLowerCase()}`} key={message.id}>
              <header><strong>{message.authorName}</strong><time dateTime={message.createdAt}>{supportDate(message.createdAt)}</time></header>
              <p>{message.body}</p>
              {admin && delivery?.emailStatus && <footer className="support-email-state"><span>Email {message.kind === 'REQUESTER' ? 'către echipă' : 'către solicitant'}: {({ PENDING: 'în așteptare', SENT: 'trimis', FAILED: 'livrare eșuată', DISABLED: 'trimitere dezactivată' })[delivery.emailStatus]}{delivery.emailAttempts > 0 ? ` · ${delivery.emailAttempts} încercări` : ''}</span>
                {(delivery.emailStatus === 'FAILED' || delivery.emailStatus === 'DISABLED') && <><small>{delivery.emailStatus === 'DISABLED' ? 'Activează MAIL_ENABLED și configurează SMTP pe backend.' : delivery.emailAttempts < 5 ? 'Se va reîncerca automat. Poți relansa și manual.' : 'Încercările automate s-au epuizat. Verifică SMTP și reîncearcă.'}</small><button type="button" disabled={mutation.isPending} onClick={() => void execute({ kind: 'retry', messageId: message.id })}>Reîncearcă emailul</button></>}
              </footer>}
            </article>
          })}</div>
          {ticket.canReply ? <form className="support-reply" onSubmit={submit}>
            <label htmlFor="support-reply">{admin ? 'Răspuns către solicitant' : 'Adaugă un răspuns'}</label>
            <textarea id="support-reply" rows={5} maxLength={4000} required value={body} disabled={mutation.isPending} onChange={e => setBody(e.target.value)} aria-describedby="support-reply-help" />
            <small id="support-reply-help">Nu include parole, date de card sau coduri de autentificare. {body.length}/4000</small>
            <button className="support-button" type="submit" disabled={mutation.isPending || !body.trim()}>{mutation.isPending ? 'Se salvează…' : 'Trimite răspunsul'}</button>
          </form> : <div className="support-closed"><strong>Solicitarea este închisă.</strong><p>{admin ? 'Redeschide solicitarea pentru a continua conversația.' : 'Pentru o problemă nouă, deschide o altă solicitare.'}</p>{!admin && <Link to="/contact">Solicitare nouă</Link>}</div>}
        </section>
        {admin && entry && 'ticket' in entry && <AdminTicketTools key={`${ticket.status}-${entry.assignedToId}`} entry={entry} pending={mutation.isPending} execute={execute} />}
      </div>
    </>}
  </main>
}

function AdminTicketTools({ entry, pending, execute }: { entry: AdminTicket; pending: boolean; execute: (action: SupportAction) => Promise<boolean> }) {
  const administrators = useSupportAdministrators(true)
  const [status, setStatus] = useState<SupportStatus>(entry.ticket.status === 'NEW' ? 'IN_PROGRESS' : entry.ticket.status)
  const [assignee, setAssignee] = useState(entry.assignedToId?.toString() ?? '')
  const [statusReason, setStatusReason] = useState('')
  const [assignmentReason, setAssignmentReason] = useState('')
  return <aside className="support-panel support-tools">
    <h2>Gestionare solicitare</h2><dl><dt>Solicitant</dt><dd>{entry.contactName}</dd><dt>Email</dt><dd><a href={`mailto:${entry.contactEmail}`}>{entry.contactEmail}</a></dd><dt>Cont</dt><dd>{entry.requesterId ? `Utilizator #${entry.requesterId}` : 'Vizitator · răspuns pe email'}</dd><dt>Responsabil</dt><dd>{entry.assignedToName ?? 'Nerepartizată'}</dd></dl>
    {entry.requesterId === null && <p className="support-hint">Răspunsurile vizitatorului pe email se preiau manual din căsuța de suport; nu sunt importate automat în conversație.</p>}
    <form onSubmit={async e => { e.preventDefault(); if (await execute({ kind: 'status', status, reason: statusReason.trim() })) setStatusReason('') }}>
      <label>Stare nouă<select value={status} onChange={e => setStatus(e.target.value as SupportStatus)} disabled={pending}>{Object.entries(supportStatuses).filter(([value]) => value !== 'NEW' && (entry.ticket.status !== 'CLOSED' || value === 'IN_PROGRESS' || value === 'CLOSED')).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label>Motivul schimbării stării<textarea required rows={2} maxLength={500} value={statusReason} onChange={e => setStatusReason(e.target.value)} disabled={pending} /></label>
      <small>Motiv intern, păstrat în jurnalul de audit.</small><button className="support-button" disabled={pending || !statusReason.trim() || status === entry.ticket.status}>Actualizează starea</button>
    </form>
    <form onSubmit={async e => { e.preventDefault(); if (await execute({ kind: 'assignment', administratorId: assignee ? Number(assignee) : null, reason: assignmentReason.trim() })) setAssignmentReason('') }}>
      <label>Repartizează către<select value={assignee} disabled={pending || administrators.isPending || administrators.isError} onChange={e => setAssignee(e.target.value)}><option value="">Nerepartizată</option>{administrators.data?.map(person => <option key={person.id} value={person.id}>{person.name}</option>)}</select></label>
      {administrators.isError && <div role="alert">Lista administratorilor nu este disponibilă. <button type="button" onClick={() => void administrators.refetch()}>Reîncearcă</button></div>}
      <label>Motivul repartizării<textarea required rows={2} maxLength={500} value={assignmentReason} onChange={e => setAssignmentReason(e.target.value)} disabled={pending} /></label>
      <button className="support-button support-button--secondary" disabled={pending || administrators.isPending || administrators.isError || !assignmentReason.trim() || assignee === (entry.assignedToId?.toString() ?? '')}>Salvează repartizarea</button>
    </form>
  </aside>
}
