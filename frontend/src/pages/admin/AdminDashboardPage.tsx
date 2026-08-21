import { Link } from 'react-router-dom'
import { AdminPageHeader, AdminPanel, AdminState } from '../../components/admin/AdminUi'
import { useAdminDashboard } from '../../features/admin/hooks/useAdmin'
import { formatAdminMoney, formatAdminNumber } from '../../features/admin/utils/adminFormatters'

export function AdminDashboardPage() {
  const dashboard = useAdminDashboard()

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Centru de control" title="Bun venit în administrare" description="Situația platformei, acțiunile urgente și indicatorii comerciali într-un singur loc." actions={<Link className="admin-button admin-button--secondary" to="/">Vezi magazinul</Link>} />
      {dashboard.isLoading && <AdminState kind="loading" title="Pregătim situația platformei" message="Calculăm indicatorii și cozile operaționale." />}
      {dashboard.isError && <AdminState kind="error" title="Datele nu au putut fi încărcate" message="Verifică legătura cu backend-ul și încearcă din nou." onRetry={() => void dashboard.refetch()} />}
      {dashboard.data && (
        <>
          <section className="admin-stats" aria-label="Indicatori principali">
            <article className="admin-stat admin-stat--forest"><span>Utilizatori noi</span><strong>{formatAdminNumber(dashboard.data.newUsersLast7Days)}</strong><small>în ultimele 7 zile</small></article>
            <article className="admin-stat"><span>Anunțuri active</span><strong>{formatAdminNumber(dashboard.data.activeListings)}</strong><small>vizibile în catalog</small></article>
            <article className="admin-stat"><span>Comenzi plătite</span><strong>{formatAdminNumber(dashboard.data.paidOrders)}</strong><small>în flux sau finalizate</small></article>
            <article className={`admin-stat${dashboard.data.openIssues > 0 ? ' admin-stat--alert' : ''}`}><span>Dispute deschise</span><strong>{formatAdminNumber(dashboard.data.openIssues)}</strong><small>necesită o decizie</small></article>
          </section>

          <div className="admin-dashboard-grid">
            <AdminPanel title="Necesită atenție" description="Cozi operaționale ordonate după impact.">
              <div className="admin-queue-list">
                <Link to="/admin/issues"><span className="admin-queue-list__mark">DS</span><span><strong>Dispute deschise</strong><small>Analizează reclamațiile cumpărătorilor</small></span><b>{dashboard.data.openIssues}</b></Link>
                <Link to="/admin/operations"><span className="admin-queue-list__mark">IN</span><span><strong>Integrări eșuate</strong><small>Stripe sau Sameday necesită retry</small></span><b>{dashboard.data.failedIntegrations}</b></Link>
                <Link to="/admin/operations"><span className="admin-queue-list__mark">TR</span><span><strong>Transferuri eșuate</strong><small>Plăți către vânzători blocate</small></span><b>{dashboard.data.failedTransfers}</b></Link>
                <Link to="/admin/operations"><span className="admin-queue-list__mark">LV</span><span><strong>Excepții livrare</strong><small>Colete pierdute sau returnate</small></span><b>{dashboard.data.shipmentExceptions}</b></Link>
              </div>
            </AdminPanel>

            <AdminPanel title="Imagine comercială" description="Valori nete după rambursările procesate.">
              <div className="admin-finance-card">
                <span>Volum net tranzacționat</span><strong>{formatAdminMoney(dashboard.data.netGmv)}</strong>
                <div><span>Comision activ</span><b>{formatAdminMoney(dashboard.data.activeCommission)}</b></div>
              </div>
              <p className="admin-panel__note">Indicatorii financiari sunt informativi. Operațiunile Stripe rămân sursa finală de adevăr.</p>
            </AdminPanel>
          </div>

          <AdminPanel title="Scurtături" description="Cele mai frecvente acțiuni administrative.">
            <div className="admin-shortcuts">
              <Link to="/admin/users"><strong>Verifică utilizatorii</strong><span>Suspendări, sesiuni și verificare email →</span></Link>
              <Link to="/admin/books"><strong>Moderează catalogul</strong><span>Ascunde sau restaurează anunțuri →</span></Link>
              <Link to="/admin/categories"><strong>Organizează categoriile</strong><span>Creează, editează și dezactivează →</span></Link>
            </div>
          </AdminPanel>
        </>
      )}
    </main>
  )
}
