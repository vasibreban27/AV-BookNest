import { useEffect, type FormEvent, type ReactNode } from 'react'

export function AdminPageHeader({ eyebrow, title, description, actions }: { eyebrow: string; title: string; description: string; actions?: ReactNode }) {
  return (
    <header className="admin-page-header">
      <div><span>{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>
      {actions && <div className="admin-page-header__actions">{actions}</div>}
    </header>
  )
}

export function AdminPanel({ title, description, actions, children, className = '' }: { title: string; description?: string; actions?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`admin-panel ${className}`}>
      <header className="admin-panel__header">
        <div><h2>{title}</h2>{description && <p>{description}</p>}</div>
        {actions}
      </header>
      {children}
    </section>
  )
}

export function AdminState({ kind, title, message, onRetry }: { kind: 'loading' | 'error' | 'empty'; title: string; message: string; onRetry?: () => void }) {
  return (
    <div className={`admin-state admin-state--${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {kind === 'loading' ? <span className="spinner" /> : <span className="admin-state__icon" aria-hidden="true">{kind === 'error' ? '!' : '○'}</span>}
      <div><strong>{title}</strong><p>{message}</p></div>
      {onRetry && <button type="button" className="admin-button admin-button--secondary" onClick={onRetry}>Reîncearcă</button>}
    </div>
  )
}

export function AdminBadge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }) {
  return <span className={`admin-badge admin-badge--${tone}`}>{children}</span>
}

export function AdminPagination({ page, totalPages, totalElements, onPageChange }: { page: number; totalPages: number; totalElements: number; onPageChange: (page: number) => void }) {
  if (totalPages <= 1) return <div className="admin-pagination admin-pagination--single"><span>{totalElements} rezultate</span></div>
  return (
    <nav className="admin-pagination" aria-label="Paginare">
      <span>{totalElements} rezultate · Pagina {page + 1} din {totalPages}</span>
      <div>
        <button type="button" onClick={() => onPageChange(page - 1)} disabled={page === 0}>Anterior</button>
        <button type="button" onClick={() => onPageChange(page + 1)} disabled={page + 1 >= totalPages}>Următor</button>
      </div>
    </nav>
  )
}

export function AdminNotice({ tone, children, onClose }: { tone: 'success' | 'error'; children: ReactNode; onClose?: () => void }) {
  return <div className={`admin-notice admin-notice--${tone}`} role={tone === 'error' ? 'alert' : 'status'}><span>{children}</span>{onClose && <button type="button" onClick={onClose} aria-label="Închide mesajul">×</button>}</div>
}

export function AdminModal({ title, description, children, onClose, size = 'medium' }: { title: string; description?: string; children: ReactNode; onClose: () => void; size?: 'small' | 'medium' | 'large' }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const handleKey = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    document.addEventListener('keydown', handleKey)
    return () => { document.body.style.overflow = previousOverflow; document.removeEventListener('keydown', handleKey) }
  }, [onClose])

  return (
    <div className="admin-modal" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target) onClose() }}>
      <section className={`admin-modal__dialog admin-modal__dialog--${size}`} role="dialog" aria-modal="true" aria-labelledby="admin-modal-title">
        <header><div><h2 id="admin-modal-title">{title}</h2>{description && <p>{description}</p>}</div><button type="button" onClick={onClose} aria-label="Închide">×</button></header>
        <div className="admin-modal__body">{children}</div>
      </section>
    </div>
  )
}

export function ReasonDialog({ title, description, confirmLabel, danger = false, pending, error, onClose, onConfirm }: { title: string; description: string; confirmLabel: string; danger?: boolean; pending: boolean; error?: string | null; onClose: () => void; onConfirm: (reason: string) => void }) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const reason = String(form.get('reason') ?? '').trim()
    if (reason) onConfirm(reason)
  }
  return (
    <AdminModal title={title} description={description} onClose={onClose} size="small">
      <form className="admin-form" onSubmit={handleSubmit}>
        <label><span>Motivul acțiunii</span><textarea name="reason" required maxLength={500} rows={4} placeholder="Notează motivul pentru jurnalul de audit..." autoFocus /></label>
        {error && <AdminNotice tone="error">{error}</AdminNotice>}
        <div className="admin-form__actions"><button type="button" className="admin-button admin-button--ghost" onClick={onClose}>Renunță</button><button type="submit" className={`admin-button${danger ? ' admin-button--danger' : ''}`} disabled={pending}>{pending ? 'Se procesează...' : confirmLabel}</button></div>
      </form>
    </AdminModal>
  )
}
