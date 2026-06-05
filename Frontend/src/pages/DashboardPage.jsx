import {useEffect, useState} from 'react'
import PageShell from '../components/PageShell.jsx'
import axios from "axios";

const dashboardText = {
  activeClients: 'לקוחות פעילים',
  brandCountSuffix: 'מותגים בטיפול',
  clientListAria: 'רשימת לקוחות',
  createClient: 'יצירת לקוח',
  emailLabel: 'אימייל',
  emptyClients: 'אין לקוחות להצגה כרגע',
  loadClientsError: 'לא הצלחנו לטעון את הלקוחות',
  managerEyebrow: 'יוצר מחובר',
  phoneLabel: 'טלפון',
  saveClient: 'שמירת לקוח',
  savingClient: 'שומר...',
  usernameLabel: 'שם משתמש',
}

const initialDashboardState = {
  userPersonalInformation: {
    fullName: 'אוצר עבדי',
    username: 'אוצר_תוכן',
    email: 'אוצר@סטודיו-פלואו.דוגמה',
    initials: 'אע',
  },
}

function DashboardPage({ activeRoute, routes, onNavigate }) {
  const [dashboardState] = useState(initialDashboardState)
  const { userPersonalInformation } = dashboardState
  const [clients, setClients] = useState([])
  const [isLoadingClients, setIsLoadingClients] = useState(true)
  const [clientsError, setClientsError] = useState('')
  const [isSavingClient, setIsSavingClient] = useState(false)
  const [newClient, setNewClient] = useState({
    businessName: '',
    fullName: '',
    email: '',
    username: '',
    password: '',
    phone: '',
    adminId: 1,
  })
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');

  const loadClients = () => {
    setIsLoadingClients(true)
    setClientsError('')

    axios.get("http://localhost:8081/clients", {
      withCredentials: true,
    }).then(response => {
      setClients(response.data)
    }).catch(() => {
      setClientsError(dashboardText.loadClientsError)
    }).finally(() => {
      setIsLoadingClients(false)
    })
  }



  useEffect(() => {
    axios.get("http://localhost:8081/users/me", {
      withCredentials: true,
    }).then(response => {
      setUsername(response.data.username)
      setName(response.data.fullName);
      setEmail(response.data.email);
    })

    axios.get("http://localhost:8081/clients", {
      withCredentials: true,
    }).then(response => {
      setClients(response.data)
    }).catch(() => {
      setClientsError(dashboardText.loadClientsError)
    }).finally(() => {
      setIsLoadingClients(false)
    })
  }, []);

  const handleClientChange = (event) => {
    const { name, value } = event.target

    setNewClient((client) => ({
      ...client,
      [name]: name === 'adminId' ? Number(value) : value,
    }))
  }

  const handleCreateClient = (event) => {
    event.preventDefault()
    setIsSavingClient(true)
    setClientsError('')

    axios.post("http://localhost:8081/clients", newClient, {
      withCredentials: true,
    }).then(() => {
      setNewClient({
        businessName: '',
        fullName: '',
        email: '',
        username: '',
        password: '',
        phone: '',
        adminId: 1,
      })
      loadClients()
    }).catch(() => {
      setClientsError('לא הצלחנו לשמור את הלקוח')
    }).finally(() => {
      setIsSavingClient(false)
    })
  }


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
            {dashboardText.usernameLabel}: {username}
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
              {clients.length} {dashboardText.brandCountSuffix}
            </h2>
          </div>

          <form className="client-form" onSubmit={handleCreateClient}>
            <h3>{dashboardText.createClient}</h3>
            <div className="client-form-grid">
              <label>
                שם העסק
                <input name="businessName" value={newClient.businessName} onChange={handleClientChange} required />
              </label>
              <label>
                שם מלא
                <input name="fullName" value={newClient.fullName} onChange={handleClientChange} />
              </label>
              <label>
                אימייל
                <input name="email" type="email" value={newClient.email} onChange={handleClientChange} required />
              </label>
              <label>
                שם משתמש
                <input name="username" value={newClient.username} onChange={handleClientChange} required />
              </label>
              <label>
                סיסמה
                <input name="password" type="password" value={newClient.password} onChange={handleClientChange} required />
              </label>
              <label>
                טלפון
                <input name="phone" value={newClient.phone} onChange={handleClientChange} />
              </label>
            </div>
            <button className="login-button" type="submit" disabled={isSavingClient}>
              {isSavingClient ? dashboardText.savingClient : dashboardText.saveClient}
            </button>
          </form>

          {isLoadingClients && <p className="client-state">טוען לקוחות...</p>}
          {clientsError && <p className="client-state client-state-error">{clientsError}</p>}
          {!isLoadingClients && !clientsError && clients.length === 0 && (
            <p className="client-state">{dashboardText.emptyClients}</p>
          )}

          {clients.map((client) => (
            <article className="client-card" key={client.client_id}>
              <div className="client-mark" aria-hidden="true">
                {client.business_name.charAt(0)}
              </div>
              <div>
                <h3>{client.business_name}</h3>
                <p>
                  {dashboardText.phoneLabel}:{' '}
                  <span className="phone-number">{client.phone || '-'}</span>
                </p>
              </div>
              <span className="channel-pill">לקוח #{client.client_id}</span>
            </article>
          ))}
        </section>
      </section>
    </PageShell>
  )
}

export default DashboardPage
