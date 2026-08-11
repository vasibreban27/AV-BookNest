import { Link } from 'react-router-dom'
import {
  useMarkNotificationRead,
  useNotifications,
} from '../../features/notifications/hooks/useNotifications'
import { BellIcon } from '../common/icons/AppIcons'

type NavbarNotificationsProps = {
  onNavigate: () => void
}

export function NavbarNotifications({ onNavigate }: NavbarNotificationsProps) {
  const { data: notifications, isLoading, isError } = useNotifications()
  const markRead = useMarkNotificationRead()
  const items = notifications ?? []
  const unreadCount = items.filter((item) => !item.readAt).length

  const openNotification = (notificationId: number, readAt: string | null) => {
    if (!readAt) markRead.mutate(notificationId)
    onNavigate()
  }

  return (
    <div className="navbar-collection navbar-notifications">
      <Link
        className="navbar-collection__trigger"
        to="/notifications"
        onClick={onNavigate}
        aria-label={`Notificări, ${unreadCount} necitite`}
      >
        <BellIcon />
        <span className="navbar-collection__trigger-label">
          <strong>Notificări</strong>
          <small>{unreadCount} necitite</small>
        </span>
        {unreadCount > 0 && (
          <span className="navbar-collection__badge">{unreadCount > 9 ? '9+' : unreadCount}</span>
        )}
      </Link>

      <div className="navbar-collection__preview notification-preview">
        <div className="navbar-collection__heading">
          <strong>Notificări</strong>
          <span>{unreadCount} necitite</span>
        </div>

        {isLoading && <p>Încărcăm notificările...</p>}
        {isError && <p>Notificările nu pot fi încărcate momentan.</p>}
        {!isLoading && !isError && items.length === 0 && <p>Nu ai notificări noi.</p>}
        {!isLoading && !isError && items.length > 0 && (
          <ul>
            {items.slice(0, 4).map((item) => (
              <li key={item.id} className={!item.readAt ? 'notification-preview__unread' : undefined}>
                <Link
                  to={item.actionUrl ?? '/notifications'}
                  onClick={() => openNotification(item.id, item.readAt)}
                >
                  <strong>{item.title}</strong>
                  <span>{item.message}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}

        <Link className="navbar-collection__view" to="/notifications" onClick={onNavigate}>
          Vezi toate notificările
        </Link>
      </div>
    </div>
  )
}
