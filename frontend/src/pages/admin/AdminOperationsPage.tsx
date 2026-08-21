import { useState } from 'react'
import { AdminBadge, AdminNotice, AdminPageHeader, AdminPagination, AdminPanel, AdminState, ReasonDialog } from '../../components/admin/AdminUi'
import { useAdminShipmentExceptions, useFailedAdminIntegrations, useFailedAdminTransfers, useRetryAdminIntegration } from '../../features/admin/hooks/useAdmin'
import type { AdminIntegrationEvent } from '../../features/admin/types/admin.types'
import { formatAdminDate, formatAdminMoney, getAdminErrorMessage } from '../../features/admin/utils/adminFormatters'
import { formatShipmentStatus } from '../../features/orders/utils/orderFormatters'

type OperationsTab = 'integrations' | 'transfers' | 'shipments'

export function AdminOperationsPage() {
  const [tab, setTab] = useState<OperationsTab>('integrations')
  const [integrationPage, setIntegrationPage] = useState(0)
  const [transferPage, setTransferPage] = useState(0)
  const [shipmentPage, setShipmentPage] = useState(0)
  const [retryTarget, setRetryTarget] = useState<AdminIntegrationEvent | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const integrations = useFailedAdminIntegrations({ page: integrationPage, size: 25 })
  const transfers = useFailedAdminTransfers({ page: transferPage, size: 25 })
  const shipments = useAdminShipmentExceptions({ page: shipmentPage, size: 25 })
  const retryIntegration = useRetryAdminIntegration()

  const handleRetry = async (reason: string) => {
    if (!retryTarget) return
    setError(null)
    try {
      await retryIntegration.mutateAsync({ eventId: retryTarget.id, reason })
      setNotice('Evenimentul a fost reintrodus în coada de procesare.')
      setRetryTarget(null)
    } catch (mutationError) { setError(getAdminErrorMessage(mutationError)) }
  }

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Sănătatea platformei" title="Operațiuni" description="Monitorizează eșecurile Stripe și Sameday. Intervențiile sunt limitate la reîncercări idempotente." />
      {notice && <AdminNotice tone="success" onClose={() => setNotice(null)}>{notice}</AdminNotice>}
      <div className="admin-operation-summary">
        <button type="button" className={tab === 'integrations' ? 'active' : ''} onClick={() => setTab('integrations')}><span>Integrări eșuate</span><strong>{integrations.data?.totalElements ?? '—'}</strong></button>
        <button type="button" className={tab === 'transfers' ? 'active' : ''} onClick={() => setTab('transfers')}><span>Transferuri eșuate</span><strong>{transfers.data?.totalElements ?? '—'}</strong></button>
        <button type="button" className={tab === 'shipments' ? 'active' : ''} onClick={() => setTab('shipments')}><span>Excepții livrare</span><strong>{shipments.data?.totalElements ?? '—'}</strong></button>
      </div>

      {tab === 'integrations' && <AdminPanel title="Evenimente de integrare" description="După remedierea cauzei, evenimentul poate fi retrimis în coadă.">
        {integrations.isLoading && <AdminState kind="loading" title="Încărcăm evenimentele" message="Verificăm coada de integrări." />}
        {integrations.isError && <AdminState kind="error" title="Coada nu poate fi încărcată" message={getAdminErrorMessage(integrations.error)} onRetry={() => void integrations.refetch()} />}
        {integrations.data?.content.length === 0 && <AdminState kind="empty" title="Nicio integrare eșuată" message="Toate evenimentele sunt procesate normal." />}
        {integrations.data && integrations.data.content.length > 0 && <><div className="admin-operation-list">{integrations.data.content.map((event) => <article key={event.id}><header><span><strong>{event.eventType}</strong><small>{event.aggregateType} #{event.aggregateId}</small></span><AdminBadge tone="danger">Eșuat după {event.attempts} încercări</AdminBadge></header><p>{event.lastError || 'Furnizorul nu a returnat detalii.'}</p><footer><span>Creat {formatAdminDate(event.createdAt)}</span><button type="button" className="admin-button admin-button--compact" onClick={() => { setRetryTarget(event); setError(null) }}>Reîncearcă</button></footer></article>)}</div><AdminPagination page={integrations.data.page} totalPages={integrations.data.totalPages} totalElements={integrations.data.totalElements} onPageChange={setIntegrationPage} /></>}
      </AdminPanel>}

      {tab === 'transfers' && <AdminPanel title="Transferuri către vânzători" description="Pentru retry folosește evenimentul Stripe corespunzător din coada de integrări.">
        {transfers.isLoading && <AdminState kind="loading" title="Încărcăm transferurile" message="Verificăm plățile către vânzători." />}
        {transfers.isError && <AdminState kind="error" title="Transferurile nu pot fi încărcate" message={getAdminErrorMessage(transfers.error)} onRetry={() => void transfers.refetch()} />}
        {transfers.data?.content.length === 0 && <AdminState kind="empty" title="Niciun transfer eșuat" message="Toate plățile către vânzători sunt în regulă." />}
        {transfers.data && transfers.data.content.length > 0 && <><div className="admin-operation-list">{transfers.data.content.map((transfer) => <article key={transfer.id}><header><span><strong>{formatAdminMoney(transfer.amount, transfer.currency)}</strong><small>Transfer #{transfer.id} · Subcomandă #{transfer.sellerOrderId}</small></span><AdminBadge tone="danger">{transfer.status}</AdminBadge></header><p>{transfer.failureReason || 'Fără mesaj de eroare.'}</p><footer><span>Actualizat {formatAdminDate(transfer.updatedAt)}</span><span>Stripe ID: {transfer.providerTransferId || 'necreat'}</span></footer></article>)}</div><AdminPagination page={transfers.data.page} totalPages={transfers.data.totalPages} totalElements={transfers.data.totalElements} onPageChange={setTransferPage} /></>}
      </AdminPanel>}

      {tab === 'shipments' && <AdminPanel title="Livrări cu probleme" description="Colete returnate sau marcate pierdute de furnizor.">
        {shipments.isLoading && <AdminState kind="loading" title="Încărcăm livrările" message="Sincronizăm excepțiile Sameday." />}
        {shipments.isError && <AdminState kind="error" title="Livrările nu pot fi încărcate" message={getAdminErrorMessage(shipments.error)} onRetry={() => void shipments.refetch()} />}
        {shipments.data?.content.length === 0 && <AdminState kind="empty" title="Nicio excepție de livrare" message="Nu sunt colete pierdute sau returnate." />}
        {shipments.data && shipments.data.content.length > 0 && <><div className="admin-operation-list">{shipments.data.content.map((entry) => <article key={entry.id}><header><span><strong>{entry.shipment.easyboxName}</strong><small>Livrare #{entry.id} · Subcomandă #{entry.sellerOrderId}</small></span><AdminBadge tone="danger">{formatShipmentStatus(entry.shipment.status)}</AdminBadge></header><p>{entry.shipment.providerStatus || 'Fără detalii suplimentare de la furnizor.'}</p><footer><span>AWB: {entry.shipment.trackingNumber || '—'}</span><span>Actualizat {formatAdminDate(entry.updatedAt)}</span></footer></article>)}</div><AdminPagination page={shipments.data.page} totalPages={shipments.data.totalPages} totalElements={shipments.data.totalElements} onPageChange={setShipmentPage} /></>}
      </AdminPanel>}

      {retryTarget && <ReasonDialog title="Reîncearcă integrarea" description={`${retryTarget.eventType} va reveni în coada de procesare cu numărul de încercări resetat.`} confirmLabel="Trimite din nou" pending={retryIntegration.isPending} error={error} onClose={() => { setRetryTarget(null); setError(null) }} onConfirm={(reason) => void handleRetry(reason)} />}
    </main>
  )
}
