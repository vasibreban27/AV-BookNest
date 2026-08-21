import { useState } from 'react'
import { AdminBadge, AdminPageHeader, AdminPagination, AdminPanel, AdminState } from '../../components/admin/AdminUi'
import { useAdminAuditLogs } from '../../features/admin/hooks/useAdmin'
import { formatAdminDate, formatAuditAction, getAdminErrorMessage } from '../../features/admin/utils/adminFormatters'

export function AdminAuditPage() {
  const [page, setPage] = useState(0)
  const logs = useAdminAuditLogs({ page, size: 50 })

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Trasabilitate" title="Jurnal de audit" description="Registru cronologic, numai pentru citire, al tuturor acțiunilor administrative importante." />
      <AdminPanel title="Activitate administrativă" description="Cele mai recente acțiuni sunt afișate primele.">
        {logs.isLoading && <AdminState kind="loading" title="Încărcăm jurnalul" message="Pregătim istoricul administrativ." />}
        {logs.isError && <AdminState kind="error" title="Jurnalul nu poate fi încărcat" message={getAdminErrorMessage(logs.error)} onRetry={() => void logs.refetch()} />}
        {logs.data?.content.length === 0 && <AdminState kind="empty" title="Jurnalul este gol" message="Primele acțiuni administrative vor apărea aici." />}
        {logs.data && logs.data.content.length > 0 && <><div className="admin-audit-list">{logs.data.content.map((log) => <article key={log.id}>
          <span className="admin-audit-list__line" aria-hidden="true" />
          <div className="admin-audit-list__mark" aria-hidden="true">{log.action.slice(0, 2)}</div>
          <div className="admin-audit-list__content"><header><strong>{formatAuditAction(log.action)}</strong><time>{formatAdminDate(log.createdAt)}</time></header><p><b>{log.administratorEmail}</b> a operat asupra <AdminBadge>{log.targetType} {log.targetId ? `#${log.targetId}` : ''}</AdminBadge></p>{log.reason && <blockquote>{log.reason}</blockquote>}{log.details && <code>{log.details}</code>}</div>
        </article>)}</div><AdminPagination page={logs.data.page} totalPages={logs.data.totalPages} totalElements={logs.data.totalElements} onPageChange={setPage} /></>}
      </AdminPanel>
    </main>
  )
}
