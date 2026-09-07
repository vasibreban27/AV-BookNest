import { useDeferredValue, useState } from 'react'
import { ModerationHistory } from '../reports/ReportDetailsPage'
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
import {
  useAdminUser,
  useAdminUsers,
  useReactivateAdminUser,
  useResendAdminVerification,
  useRevokeAdminUserSessions,
  useSuspendAdminUser,
} from '../../features/admin/hooks/useAdmin'
import type { AdminUser } from '../../features/admin/types/admin.types'
import { formatAdminDate, getAdminErrorMessage } from '../../features/admin/utils/adminFormatters'

type UserAction = { type: 'suspend' | 'reactivate' | 'sessions'; user: AdminUser }

export function AdminUsersPage() {
  const [query, setQuery] = useState('')
  const [enabled, setEnabled] = useState('')
  const [page, setPage] = useState(0)
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null)
  const [action, setAction] = useState<UserAction | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const deferredQuery = useDeferredValue(query)
  const filters = { query: deferredQuery, enabled: enabled === '' ? undefined : enabled === 'true', page, size: 25 }
  const users = useAdminUsers(filters)
  const userDetails = useAdminUser(selectedUserId)
  const suspendUser = useSuspendAdminUser()
  const reactivateUser = useReactivateAdminUser()
  const revokeSessions = useRevokeAdminUserSessions()
  const resendVerification = useResendAdminVerification()

  const handleReasonAction = async (reason: string) => {
    if (!action) return
    setActionError(null)
    try {
      if (action.type === 'suspend') await suspendUser.mutateAsync({ userId: action.user.id, reason })
      if (action.type === 'reactivate') await reactivateUser.mutateAsync({ userId: action.user.id, reason })
      if (action.type === 'sessions') await revokeSessions.mutateAsync({ userId: action.user.id, reason })
      setNotice(action.type === 'suspend' ? 'Contul a fost suspendat, sesiunile revocate și anunțurile ascunse.' : action.type === 'reactivate' ? 'Contul și anunțurile ascunse automat au fost reactivate.' : 'Sesiunile active au fost revocate.')
      setAction(null)
    } catch (error) {
      setActionError(getAdminErrorMessage(error))
    }
  }

  const handleResendVerification = async (userId: number) => {
    setActionError(null)
    try {
      await resendVerification.mutateAsync(userId)
      setNotice('Emailul de verificare a fost retrimis.')
    } catch (error) {
      setActionError(getAdminErrorMessage(error))
    }
  }

  const pending = suspendUser.isPending || reactivateUser.isPending || revokeSessions.isPending
  const actionCopy = action && {
    suspend: { title: 'Suspendă utilizatorul', description: 'Accesul va fi blocat imediat, sesiunile vor fi revocate, iar anunțurile vizibile vor fi ascunse.', label: 'Suspendă contul', danger: true },
    reactivate: { title: 'Reactivează utilizatorul', description: 'Contul și anunțurile ascunse automat la suspendare vor redeveni active.', label: 'Reactivează contul', danger: false },
    sessions: { title: 'Revocă toate sesiunile', description: 'Utilizatorul va trebui să se autentifice din nou pe toate dispozitivele.', label: 'Revocă sesiunile', danger: true },
  }[action.type]

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Comunitate" title="Utilizatori" description="Caută conturi, verifică activitatea și intervino fără să modifici roluri sau parole." />
      {notice && <AdminNotice tone="success" onClose={() => setNotice(null)}>{notice}</AdminNotice>}
      <AdminPanel title="Toate conturile" description="Rezultatele sunt paginate direct din baza de date.">
        <div className="admin-filters">
          <label className="admin-field admin-field--search"><span>Caută</span><input type="search" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} placeholder="Nume sau adresă de email" /></label>
          <label className="admin-field"><span>Starea contului</span><select value={enabled} onChange={(event) => { setEnabled(event.target.value); setPage(0) }}><option value="">Toate conturile</option><option value="true">Active</option><option value="false">Suspendate</option></select></label>
        </div>
        {users.isLoading && <AdminState kind="loading" title="Încărcăm utilizatorii" message="Căutăm conturile care corespund filtrelor." />}
        {users.isError && <AdminState kind="error" title="Utilizatorii nu au putut fi încărcați" message={getAdminErrorMessage(users.error)} onRetry={() => void users.refetch()} />}
        {users.data?.content.length === 0 && <AdminState kind="empty" title="Niciun utilizator găsit" message="Schimbă termenul de căutare sau filtrul selectat." />}
        {users.data && users.data.content.length > 0 && (
          <>
            <div className="admin-table-wrap">
              <table className="admin-table"><thead><tr><th>Utilizator</th><th>Rol</th><th>Cont</th><th>Email</th><th>Creat</th><th><span className="sr-only">Acțiuni</span></th></tr></thead>
                <tbody>{users.data.content.map((user) => (
                  <tr key={user.id}>
                    <td data-label="Utilizator"><button className="admin-user-cell" type="button" onClick={() => setSelectedUserId(user.id)}><span>{user.firstName[0]}{user.lastName[0]}</span><span><strong>{user.firstName} {user.lastName}</strong><small>{user.email}</small></span></button></td>
                    <td data-label="Rol"><AdminBadge tone={user.role === 'ADMIN' ? 'info' : 'neutral'}>{user.role}</AdminBadge></td>
                    <td data-label="Cont"><AdminBadge tone={user.enabled ? 'success' : 'danger'}>{user.enabled ? 'Activ' : 'Suspendat'}</AdminBadge></td>
                    <td data-label="Email"><AdminBadge tone={user.emailVerified ? 'success' : 'warning'}>{user.emailVerified ? 'Verificat' : 'Neverificat'}</AdminBadge></td>
                    <td data-label="Creat">{formatAdminDate(user.createdAt)}</td>
                    <td><button className="admin-button admin-button--compact admin-button--secondary" type="button" onClick={() => setSelectedUserId(user.id)}>Detalii</button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            <AdminPagination page={users.data.page} totalPages={users.data.totalPages} totalElements={users.data.totalElements} onPageChange={setPage} />
          </>
        )}
      </AdminPanel>

      {selectedUserId !== null && (
        <AdminModal title="Detalii utilizator" description="Activitate, acces și verificări ale contului." onClose={() => { setSelectedUserId(null); setActionError(null) }} size="large">
          {userDetails.isLoading && <AdminState kind="loading" title="Încărcăm profilul" message="Centralizăm activitatea utilizatorului." />}
          {userDetails.isError && <AdminState kind="error" title="Profilul nu poate fi afișat" message={getAdminErrorMessage(userDetails.error)} />}
          {userDetails.data && (
            <div className="admin-detail">
              <div className="admin-detail__identity"><span>{userDetails.data.user.firstName[0]}{userDetails.data.user.lastName[0]}</span><div><h3>{userDetails.data.user.firstName} {userDetails.data.user.lastName}</h3><p>{userDetails.data.user.email}</p></div><AdminBadge tone={userDetails.data.user.enabled ? 'success' : 'danger'}>{userDetails.data.user.enabled ? 'Activ' : 'Suspendat'}</AdminBadge></div>
              <dl className="admin-detail-grid"><div><dt>Telefon</dt><dd>{userDetails.data.user.phoneNumber || '—'}</dd></div><div><dt>Email</dt><dd>{userDetails.data.user.emailVerified ? 'Verificat' : 'Neverificat'}</dd></div><div><dt>Stripe payouts</dt><dd>{userDetails.data.user.stripePayoutsEnabled ? 'Active' : 'Inactive'}</dd></div><div><dt>Înregistrat</dt><dd>{formatAdminDate(userDetails.data.user.createdAt)}</dd></div></dl>
              <div className="admin-mini-stats"><div><strong>{userDetails.data.listingCount}</strong><span>Anunțuri</span></div><div><strong>{userDetails.data.buyerOrderCount}</strong><span>Comenzi</span></div><div><strong>{userDetails.data.sellerOrderCount}</strong><span>Vânzări</span></div></div>
              {userDetails.data.user.suspensionReason && <div className="admin-callout admin-callout--danger"><strong>Motivul suspendării</strong><p>{userDetails.data.user.suspensionReason}</p><small>{formatAdminDate(userDetails.data.user.suspendedAt)}</small></div>}
              {userDetails.data.user.suspendedUntil && <p>Reactivare automată după {formatAdminDate(userDetails.data.user.suspendedUntil)}</p>}
              <ModerationHistory userId={userDetails.data.user.id} />
              {actionError && <AdminNotice tone="error" onClose={() => setActionError(null)}>{actionError}</AdminNotice>}
              <div className="admin-detail__actions">
                {userDetails.data.user.role !== 'ADMIN' && (userDetails.data.user.enabled ? <button className="admin-button admin-button--danger" type="button" onClick={() => setAction({ type: 'suspend', user: userDetails.data!.user })}>Suspendă</button> : <button className="admin-button" type="button" onClick={() => setAction({ type: 'reactivate', user: userDetails.data!.user })}>Reactivează</button>)}
                <button className="admin-button admin-button--secondary" type="button" onClick={() => setAction({ type: 'sessions', user: userDetails.data!.user })}>Revocă sesiunile</button>
                {!userDetails.data.user.emailVerified && <button className="admin-button admin-button--ghost" type="button" disabled={resendVerification.isPending} onClick={() => void handleResendVerification(userDetails.data!.user.id)}>Retrimite verificarea</button>}
              </div>
            </div>
          )}
        </AdminModal>
      )}

      {action && actionCopy && <ReasonDialog title={actionCopy.title} description={actionCopy.description} confirmLabel={actionCopy.label} danger={actionCopy.danger} pending={pending} error={actionError} onClose={() => { setAction(null); setActionError(null) }} onConfirm={(reason) => void handleReasonAction(reason)} />}
    </main>
  )
}
