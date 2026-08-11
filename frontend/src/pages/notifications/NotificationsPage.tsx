import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from '../../features/notifications/hooks/useNotifications'

export function NotificationsPage() {
  const { data: notifications, isLoading, isError } = useNotifications()
  const markRead = useMarkNotificationRead()
  const markAllRead = useMarkAllNotificationsRead()
  const items = notifications ?? []
  const hasUnread = items.some((item) => !item.readAt)

  const handleOpen = (notificationId: number, readAt: string | null, actionUrl: string | null) => {
    if (!readAt) markRead.mutate(notificationId)
    if (actionUrl) window.location.assign(actionUrl)
  }

  return (
    <main className="notifications-page">
      <section className="notifications-content">
        <header>
          <div>
            <span className="eyebrow">Contul tău</span>
            <h1>Notificări</h1>
          </div>
          {hasUnread && (
            <button type="button" onClick={() => markAllRead.mutate()} disabled={markAllRead.isPending}>
              Marchează toate ca citite
            </button>
          )}
        </header>

        {isLoading && <p>Încărcăm notificările...</p>}
        {isError && <p>Notificările nu pot fi încărcate momentan.</p>}
        {!isLoading && !isError && items.length === 0 && (
          <div className="notifications-empty">Nu ai primit încă nicio notificare.</div>
        )}
        {!isLoading && !isError && items.length > 0 && (
          <div className="notifications-list">
            {items.map((item) => (
              <button
                className={`notification-item${!item.readAt ? ' notification-item--unread' : ''}`}
                type="button"
                key={item.id}
                onClick={() => handleOpen(item.id, item.readAt, item.actionUrl)}
              >
                <div>
                  <strong>{item.title}</strong>
                  {!item.readAt && <span>Nou</span>}
                </div>
                <p>{item.message}</p>
                <small>{new Date(item.createdAt).toLocaleString('ro-RO')}</small>
              </button>
            ))}
          </div>
        )}
      </section>
    </main>
  )
}
