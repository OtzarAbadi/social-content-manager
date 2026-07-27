import { useState } from 'react'
import Cookies from 'js-cookie'
import { Eye, EyeOff, LockKeyhole, User } from 'lucide-react'
import PageShell from '../components/PageShell.jsx'
import api from '../services/api.js'
import { APP_INITIAL, APP_NAME } from '../config/appConfig.js'

function LoginPage({ activeRoute, routes, onNavigate, isAuthenticated, onAuthenticated, onLogout }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const isLoginDisabled = !username.trim() || !password.trim() || loading

  async function handleLogin(event) {
    event.preventDefault()
    if (isLoginDisabled) return
    setLoading(true)
    setErrorMessage('')
    try {
      const response = await api.post('/users/login', { username, password })
      if (!response.data.success) {
        setErrorMessage('שם המשתמש או הסיסמה אינם נכונים')
        return
      }
      Cookies.set('token', response.data.token, {
        expires: 7,
        secure: window.location.protocol === 'https:',
        sameSite: 'strict',
      })
      onAuthenticated()
    } catch {
      setErrorMessage('לא הצלחנו להתחבר כרגע. נסו שוב בעוד רגע.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <PageShell activeRoute={activeRoute} routes={routes} onNavigate={onNavigate} isAuthenticated={isAuthenticated} onLogout={onLogout}>
      <section className="login-page" aria-labelledby="login-title">
        <div className="login-panel">
          <div className="login-card">
            <div className="login-brand"><span className="brand-mark">{APP_INITIAL}</span><strong>{APP_NAME}</strong></div>
            <div className="login-intro">
              <h3>התוכן שלכם<br />מתוכנן חכם.</h3>
              <p>ניהול, אישור ותזמון של כל התוכן החברתי במקום אחד.</p>
            </div>
            <p className="eyebrow">ברוכים הבאים</p>
            <h2 id="login-title">כניסה לחשבון</h2>
            <p className="login-note">הזינו את פרטי החשבון כדי להמשיך למרחב העבודה.</p>
            <form className="field-stack" onSubmit={handleLogin}>
              <label>שם משתמש<span className="input-with-icon"><i aria-hidden="true"><User size={20} /></i><input autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="שם המשתמש שלך" required /></span></label>
              <label>סיסמה<span className="input-with-icon"><i aria-hidden="true"><LockKeyhole size={20} /></i><input autoComplete="current-password" type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="הסיסמה שלך" required /><button type="button" onClick={() => setShowPassword((current) => !current)} aria-label={showPassword ? 'הסתרת סיסמה' : 'הצגת סיסמה'}>{showPassword ? <EyeOff size={20} /> : <Eye size={20} />}</button></span></label>
              {errorMessage && <p className="login-error" role="alert">{errorMessage}</p>}
              <button className="login-button" type="submit" disabled={isLoginDisabled}>{loading ? <><span className="button-spinner" /> מתחבר...</> : 'כניסה למערכת'}</button>
            </form>
          </div>
        </div>
      </section>
    </PageShell>
  )
}

export default LoginPage
