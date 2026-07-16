import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Footer } from '../components/footer/Footer'
import { Navbar } from '../components/navbar/Navbar'

export function AppLayout() {
  const location = useLocation()

  useEffect(() => {
    if (location.hash) {
      requestAnimationFrame(() => {
        document
          .getElementById(location.hash.slice(1))
          ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      })
      return
    }

    window.scrollTo({ top: 0, behavior: 'instant' })
  }, [location.hash, location.pathname])

  return (
    <div className="app-layout">
      <Navbar />
      <Outlet />
      <Footer />
    </div>
  )
}
