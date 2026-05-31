import {useEffect, useState} from 'react'
import PageShell from '../components/PageShell.jsx'
import axios from "axios";

const dashboardText = {
  activeClients: 'לקוחות פעילים',
  brandCountSuffix: 'מותגים בטיפול',
  clientListAria: 'רשימת לקוחות',
  emailLabel: 'אימייל',
  managerEyebrow: 'יוצר מחובר',
  phoneLabel: 'טלפון',
  usernameLabel: 'שם משתמש',
}

const initialDashboardState = {
  userPersonalInformation: {
    fullName: 'אוצר עבדי',
    username: 'אוצר_תוכן',
    email: 'אוצר@סטודיו-פלואו.דוגמה',
    initials: 'אע',
  },
  systemsInformation: {
    systems: [
      {
        id: 1,
        name: 'בנק הפועלים',
        phone: '050-112-3088',
        channel: 'אינסטגרם',
      },
      {
        id: 2,
        name: 'שופרסל',
        phone: '052-431-9044',
        channel: 'טיקטוק',
      },
      {
        id: 3,
        name: 'אל על',
        phone: '054-886-2270',
        channel: 'לינקדאין',
      },
    ],
  },
}

function DashboardPage({ activeRoute, routes, onNavigate }) {
  const [dashboardState] = useState(initialDashboardState)
  const { systemsInformation, userPersonalInformation } = dashboardState
  const { systems } = systemsInformation
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')


  useEffect(() => {
    axios.get("http://localhost:8081/users/me", {
      withCredentials: true,
    }).then(response => {
      setName(response.data.userModel.fullName)
      setEmail(response.data.userModel.email)
    })
  }, []);


  return (
    <PageShell
      activeRoute={activeRoute}
      routes={routes}
      onNavigate={onNavigate}
    >
      <section className="page-grid" aria-labelledby="clients-title">
        <aside className="manager-panel">
          <div className="manager-photo" aria-hidden="true">
            {userPersonalInformation.initials}
          </div>
          <p className="eyebrow">{dashboardText.managerEyebrow}</p>
          <h2 id="clients-title">{name}</h2>
          <p>
            {dashboardText.usernameLabel}: {userPersonalInformation.username}
          </p>
          <p>
            {dashboardText.emailLabel}: {email}
          </p>

          <div className="content-board" aria-hidden="true">
            {[1, 2, 3, 4].map((tile) => (
              <span key={tile}></span>
            ))}
          </div>
        </aside>

        <section className="client-list" aria-label={dashboardText.clientListAria}>
          <div className="section-heading">
            <p className="eyebrow">{dashboardText.activeClients}</p>
            <h2>
              {systems.length} {dashboardText.brandCountSuffix}
            </h2>
          </div>

          {systems.map((system) => (
            <article className="client-card" key={system.id}>
              <div className="client-mark" aria-hidden="true">
                {system.name.charAt(0)}
              </div>
              <div>
                <h3>{system.name}</h3>
                <p>
                  {dashboardText.phoneLabel}:{' '}
                  <span className="phone-number">{system.phone}</span>
                </p>
              </div>
              <span className="channel-pill">{system.channel}</span>
            </article>
          ))}
        </section>
      </section>
    </PageShell>
  )
}

export default DashboardPage
