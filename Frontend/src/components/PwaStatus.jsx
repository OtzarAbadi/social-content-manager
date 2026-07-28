import { useEffect, useState } from 'react'
import { Download, RefreshCw, WifiOff, X } from 'lucide-react'
import { useRegisterSW } from 'virtual:pwa-register/react'

const INSTALL_DISMISSED_KEY = 'sscm-pwa-install-dismissed'
const OFFLINE_MESSAGE = 'אין חיבור לאינטרנט. יש להתחבר מחדש כדי לבצע פעולה זו.'

function isStandalone() {
  return window.matchMedia?.('(display-mode: standalone)').matches
    || window.navigator.standalone === true
}

function isIos() {
  return /iPad|iPhone|iPod/.test(window.navigator.userAgent)
}

function isAndroid() {
  return /Android/i.test(window.navigator.userAgent)
}

function PwaStatus() {
  const [installPrompt, setInstallPrompt] = useState(null)
  const [installDismissed, setInstallDismissed] = useState(
    () => window.localStorage.getItem(INSTALL_DISMISSED_KEY) === 'true',
  )
  const [online, setOnline] = useState(() => window.navigator.onLine)
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    updateServiceWorker,
  } = useRegisterSW()

  useEffect(() => {
    const handleInstallPrompt = (event) => {
      event.preventDefault()
      if (!installDismissed && !isStandalone()) setInstallPrompt(event)
    }
    const handleInstalled = () => setInstallPrompt(null)
    const handleOnline = () => setOnline(true)
    const handleOffline = () => setOnline(false)

    window.addEventListener('beforeinstallprompt', handleInstallPrompt)
    window.addEventListener('appinstalled', handleInstalled)
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    return () => {
      window.removeEventListener('beforeinstallprompt', handleInstallPrompt)
      window.removeEventListener('appinstalled', handleInstalled)
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [installDismissed])

  async function install() {
    if (!installPrompt) return
    await installPrompt.prompt()
    await installPrompt.userChoice
    setInstallPrompt(null)
  }

  function dismissInstall() {
    window.localStorage.setItem(INSTALL_DISMISSED_KEY, 'true')
    setInstallDismissed(true)
    setInstallPrompt(null)
  }

  const canShowGuidance = !isStandalone() && !installDismissed
  const installMode = !canShowGuidance
    ? null
    : installPrompt
      ? 'automatic'
      : isIos()
        ? 'ios'
        : isAndroid()
          ? 'android-manual'
          : null

  return (
    <aside className="pwa-status-stack" aria-live="polite">
      {!online && (
        <div className="pwa-status-banner pwa-offline" role="status">
          <WifiOff size={20} aria-hidden="true" />
          <span>{OFFLINE_MESSAGE}</span>
        </div>
      )}

      {needRefresh && (
        <div className="pwa-status-banner pwa-update" role="status">
          <RefreshCw size={20} aria-hidden="true" />
          <span>גרסה חדשה זמינה</span>
          <button type="button" onClick={() => updateServiceWorker(true)}>עדכון עכשיו</button>
          <button
            type="button"
            className="pwa-close"
            aria-label="סגירת הודעת העדכון"
            onClick={() => setNeedRefresh(false)}
          >
            <X size={18} />
          </button>
        </div>
      )}

      {installMode && (
        <div className="pwa-status-banner pwa-install" role="status">
          <Download size={20} aria-hidden="true" />
          <div className="pwa-install-copy">
            <strong>התקנת SocialContent</strong>
            {installMode === 'automatic' && (
              <span>התקינו את המערכת כאפליקציה במכשיר לקבלת גישה מהירה ונוחה.</span>
            )}
            {installMode === 'android-manual' && (
              <span>
                ב־Chrome לחצו על תפריט שלוש הנקודות ולאחר מכן בחרו:
                <br />״התקנת האפליקציה״ או ״הוספה למסך הבית״
              </span>
            )}
            {installMode === 'ios' && (
              <span>
                ב־Safari לחצו על כפתור השיתוף ולאחר מכן על:
                <br />״הוספה למסך הבית״
              </span>
            )}
          </div>
          {installMode === 'automatic' && (
            <button type="button" onClick={install}>התקנת האפליקציה</button>
          )}
          <button type="button" className="pwa-close" aria-label="לא עכשיו" onClick={dismissInstall}>
            <X size={18} />
          </button>
        </div>
      )}
    </aside>
  )
}

export default PwaStatus
