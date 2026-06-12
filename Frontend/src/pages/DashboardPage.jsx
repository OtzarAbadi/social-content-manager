import { useCallback, useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import PageShell from '../components/PageShell.jsx'

const api = axios.create({
  baseURL: 'http://localhost:8081',
  withCredentials: true,
})

const statusOptions = [
  { value: 'DRAFT', label: 'טיוטה' },
  { value: 'WAITING_APPROVAL', label: 'ממתין לאישור' },
  { value: 'APPROVED', label: 'מאושר' },
  { value: 'REJECTED', label: 'נדחה' },
  { value: 'PUBLISHED', label: 'פורסם' },
]

const contentTypeOptions = [
  { value: 'IMAGE', label: 'תמונה' },
  { value: 'VIDEO', label: 'וידאו' },
  { value: 'TEXT', label: 'טקסט' },
]

const emptyClientForm = {
  businessName: '',
  fullName: '',
  email: '',
  username: '',
  password: '',
  phone: '',
  adminId: 1,
}

const emptyContentForm = {
  clientId: '',
  title: '',
  description: '',
  file_url: '',
  content_type: 'IMAGE',
  status: 'DRAFT',
  plannedPublishDate: '',
}

const emptyCommentForm = {
  contentId: '',
  userId: '',
  commentText: '',
}

const statusLabelByValue = statusOptions.reduce((labels, status) => {
  labels[status.value] = status.label
  return labels
}, {})

const typeLabelByValue = contentTypeOptions.reduce((labels, type) => {
  labels[type.value] = type.label
  return labels
}, {})

function toInputDateTime(value) {
  if (!value) {
    return ''
  }

  return value.slice(0, 16)
}

function getContentId(content) {
  return content.content_id ?? content.contentId
}

function getClientId(client) {
  return client.client_id ?? client.clientId
}

function getClientInitial(client) {
  return client.business_name?.trim()?.charAt(0) || '#'
}

function getProfileInitials(profile) {
  const name = profile.fullName || profile.username || 'משתמש'
  const parts = name.trim().split(/\s+/).filter(Boolean)

  if (parts.length === 0) {
    return 'מ'
  }

  return parts.slice(0, 2).map((part) => part.charAt(0)).join('')
}

function DashboardPage({ activeRoute, routes, onNavigate }) {
  const [activePanel, setActivePanel] = useState('contents')
  const [profile, setProfile] = useState({
    id: '',
    clientId: '',
    fullName: '',
    username: '',
    email: '',
    role: '',
  })

  const [clients, setClients] = useState([])
  const [contents, setContents] = useState([])
  const [comments, setComments] = useState([])

  const [clientForm, setClientForm] = useState(emptyClientForm)
  const [contentForm, setContentForm] = useState(emptyContentForm)
  const [commentForm, setCommentForm] = useState(emptyCommentForm)
  const [showCreateForm, setShowCreateForm] = useState({
    clients: false,
    contents: false,
  })

  const [clientLookupId, setClientLookupId] = useState('')
  const [contentFilter, setContentFilter] = useState({
    contentId: '',
    clientId: '',
    status: '',
  })
  const [commentsContentId, setCommentsContentId] = useState('')
  const [filteredResults, setFilteredResults] = useState({
    clients: false,
    contents: false,
    comments: false,
  })
  const [resultsHidden, setResultsHidden] = useState({
    clients: false,
    contents: false,
    comments: false,
  })

  const [editingClientId, setEditingClientId] = useState(null)
  const [editingContentId, setEditingContentId] = useState(null)
  const [clientDraft, setClientDraft] = useState(null)
  const [contentDraft, setContentDraft] = useState(null)

  const [loading, setLoading] = useState({
    clients: true,
    contents: true,
    comments: true,
    profile: true,
  })
  const [saving, setSaving] = useState({
    client: false,
    content: false,
    comment: false,
  })
  const [errors, setErrors] = useState({
    clients: '',
    contents: '',
    comments: '',
    profile: '',
  })
  const [notice, setNotice] = useState('')

  const clientById = useMemo(() => {
    return new Map(clients.map((client) => [Number(getClientId(client)), client]))
  }, [clients])

  const waitingApprovalCount = useMemo(() => {
    return contents.filter((content) => content.status === 'WAITING_APPROVAL').length
  }, [contents])

  const isClient = profile.role === 'CLIENT'
  const isAdmin = !isClient
  const visiblePanel = isClient && activePanel === 'clients' ? 'contents' : activePanel

  const resetResultView = useCallback((section) => {
    setFilteredResults((current) => ({ ...current, [section]: false }))
    setResultsHidden((current) => ({ ...current, [section]: false }))
  }, [])

  const showFilteredResults = useCallback((section) => {
    setFilteredResults((current) => ({ ...current, [section]: true }))
    setResultsHidden((current) => ({ ...current, [section]: false }))
  }, [])

  function toggleResults(section) {
    setResultsHidden((current) => ({ ...current, [section]: !current[section] }))
  }

  function toggleCreateForm(section) {
    setShowCreateForm((current) => ({ ...current, [section]: !current[section] }))
  }

  const loadProfile = useCallback(async () => {
    await Promise.resolve()
    setLoading((current) => ({ ...current, profile: true }))
    setErrors((current) => ({ ...current, profile: '' }))

    try {
      const response = await api.get('/users/me')
      setProfile(response.data)
      setCommentForm((current) => ({
        ...current,
        userId: current.userId || String(response.data.id ?? ''),
      }))
    } catch {
      setErrors((current) => ({
        ...current,
        profile: 'לא הצלחנו לטעון את פרטי המשתמש',
      }))
    } finally {
      setLoading((current) => ({ ...current, profile: false }))
    }
  }, [])

  const loadClients = useCallback(async () => {
    await Promise.resolve()
    resetResultView('clients')
    setLoading((current) => ({ ...current, clients: true }))
    setErrors((current) => ({ ...current, clients: '' }))

    try {
      const response = await api.get('/clients')
      setClients(response.data)
      return response.data
    } catch {
      setErrors((current) => ({
        ...current,
        clients: 'לא הצלחנו לטעון את הלקוחות',
      }))
      return []
    } finally {
      setLoading((current) => ({ ...current, clients: false }))
    }
  }, [resetResultView])

  const loadContents = useCallback(async () => {
    await Promise.resolve()
    resetResultView('contents')
    setLoading((current) => ({ ...current, contents: true }))
    setErrors((current) => ({ ...current, contents: '' }))

    try {
      const response = await api.get('/contents')
      setContents(response.data)
      return response.data
    } catch {
      setErrors((current) => ({
        ...current,
        contents: 'לא הצלחנו לטעון את התכנים',
      }))
      return []
    } finally {
      setLoading((current) => ({ ...current, contents: false }))
    }
  }, [resetResultView])

  const loadComments = useCallback(async () => {
    await Promise.resolve()
    resetResultView('comments')
    setLoading((current) => ({ ...current, comments: true }))
    setErrors((current) => ({ ...current, comments: '' }))

    try {
      const response = await api.get('/comments')
      setComments(response.data)
      return response.data
    } catch {
      setErrors((current) => ({
        ...current,
        comments: 'לא הצלחנו לטעון את התגובות',
      }))
      return []
    } finally {
      setLoading((current) => ({ ...current, comments: false }))
    }
  }, [resetResultView])

  useEffect(() => {
    let isMounted = true

    Promise.resolve().then(() => {
      if (!isMounted) {
        return
      }

      loadProfile()
      loadClients()
      loadContents()
      loadComments()
    })

    return () => {
      isMounted = false
    }
  }, [loadClients, loadComments, loadContents, loadProfile])

  function showNotice(message) {
    setNotice(message)
  }

  function handleClientFormChange(event) {
    const { name, value } = event.target

    setClientForm((current) => ({
      ...current,
      [name]: name === 'adminId' ? Number(value) : value,
    }))
  }

  async function handleCreateClient(event) {
    event.preventDefault()
    setSaving((current) => ({ ...current, client: true }))
    setErrors((current) => ({ ...current, clients: '' }))

    try {
      await api.post('/clients', clientForm)
      setClientForm(emptyClientForm)
      setShowCreateForm((current) => ({ ...current, clients: false }))
      await loadClients()
      showNotice('הלקוח נוצר בהצלחה')
    } catch {
      setErrors((current) => ({
        ...current,
        clients: 'לא הצלחנו ליצור את הלקוח',
      }))
    } finally {
      setSaving((current) => ({ ...current, client: false }))
    }
  }

  async function handleFindClientById() {
    if (!clientLookupId) {
      return
    }

    setLoading((current) => ({ ...current, clients: true }))
    setErrors((current) => ({ ...current, clients: '' }))

    try {
      const response = await api.get(`/clients/${clientLookupId}`)
      setClients([response.data])
      showFilteredResults('clients')
      showNotice(`נטען לקוח #${clientLookupId}`)
    } catch {
      setClients([])
      showFilteredResults('clients')
      setErrors((current) => ({
        ...current,
        clients: 'לא נמצא לקוח עם המזהה הזה',
      }))
    } finally {
      setLoading((current) => ({ ...current, clients: false }))
    }
  }

  function startClientEdit(client) {
    setEditingClientId(getClientId(client))
    setClientDraft({
      userId: client.user_id ?? '',
      adminId: client.admin_id ?? '',
      businessName: client.business_name ?? '',
      phone: client.phone ?? '',
    })
  }

  function handleClientDraftChange(event) {
    const { name, value } = event.target

    setClientDraft((current) => ({
      ...current,
      [name]: value,
    }))
  }

  async function handleUpdateClient(clientId) {
    const payload = {
      businessName: clientDraft.businessName,
      phone: clientDraft.phone,
    }

    if (clientDraft.userId !== '') {
      payload.userId = Number(clientDraft.userId)
    }

    if (clientDraft.adminId !== '') {
      payload.adminId = Number(clientDraft.adminId)
    }

    setErrors((current) => ({ ...current, clients: '' }))

    try {
      await api.put(`/clients/${clientId}`, payload)
      setEditingClientId(null)
      setClientDraft(null)
      await loadClients()
      showNotice(`לקוח #${clientId} עודכן`)
    } catch {
      setErrors((current) => ({
        ...current,
        clients: 'לא הצלחנו לעדכן את הלקוח',
      }))
    }
  }

  async function handleDeleteClient(clientId) {
    if (!window.confirm(`למחוק את לקוח #${clientId}?`)) {
      return
    }

    setErrors((current) => ({ ...current, clients: '' }))

    try {
      await api.delete(`/clients/${clientId}`)
      await loadClients()
      await loadContents()
      showNotice(`לקוח #${clientId} נמחק`)
    } catch {
      setErrors((current) => ({
        ...current,
        clients: 'לא הצלחנו למחוק את הלקוח',
      }))
    }
  }

  function handleContentFormChange(event) {
    const { name, value } = event.target

    setContentForm((current) => ({
      ...current,
      [name]: value,
    }))
  }

  async function handleCreateContent(event) {
    event.preventDefault()
    setSaving((current) => ({ ...current, content: true }))
    setErrors((current) => ({ ...current, contents: '' }))

    const payload = {
      clientId: Number(contentForm.clientId),
      title: contentForm.title,
      description: contentForm.description,
      file_url: contentForm.file_url,
      content_type: contentForm.content_type,
    }

    if (contentForm.plannedPublishDate) {
      payload.plannedPublishDate = contentForm.plannedPublishDate
    }

    try {
      await api.post('/contents', payload)
      setContentForm(emptyContentForm)
      setShowCreateForm((current) => ({ ...current, contents: false }))
      await loadContents()
      showNotice('התוכן נוצר בהצלחה')
    } catch {
      setErrors((current) => ({
        ...current,
        contents: 'לא הצלחנו ליצור את התוכן. בדקי שנבחר לקוח קיים',
      }))
    } finally {
      setSaving((current) => ({ ...current, content: false }))
    }
  }

  function handleContentFilterChange(event) {
    const { name, value } = event.target

    setContentFilter((current) => ({
      ...current,
      [name]: value,
    }))
  }

  async function handleFindContentById() {
    if (!contentFilter.contentId) {
      return
    }

    setLoading((current) => ({ ...current, contents: true }))
    setErrors((current) => ({ ...current, contents: '' }))

    try {
      const response = await api.get(`/contents/${contentFilter.contentId}`)
      setContents([response.data])
      showFilteredResults('contents')
      showNotice(`נטען תוכן #${contentFilter.contentId}`)
    } catch {
      setContents([])
      showFilteredResults('contents')
      setErrors((current) => ({
        ...current,
        contents: 'לא נמצא תוכן עם המזהה הזה',
      }))
    } finally {
      setLoading((current) => ({ ...current, contents: false }))
    }
  }

  async function handleLoadContentsByClient(clientId = contentFilter.clientId) {
    if (!clientId) {
      return
    }

    setLoading((current) => ({ ...current, contents: true }))
    setErrors((current) => ({ ...current, contents: '' }))

    try {
      const response = await api.get(`/contents/client/${clientId}`)
      setContents(response.data)
      setContentFilter((current) => ({ ...current, clientId: String(clientId) }))
      setActivePanel('contents')
      showFilteredResults('contents')
      showNotice(`נטענו תכנים של לקוח #${clientId}`)
    } catch {
      setContents([])
      showFilteredResults('contents')
      setErrors((current) => ({
        ...current,
        contents: 'לא נמצא לקוח עם המזהה הזה',
      }))
    } finally {
      setLoading((current) => ({ ...current, contents: false }))
    }
  }

  async function handleLoadContentsByStatus(status = contentFilter.status) {
    if (!status) {
      return
    }

    setLoading((current) => ({ ...current, contents: true }))
    setErrors((current) => ({ ...current, contents: '' }))

    try {
      const response = await api.get(`/contents/status/${status}`)
      setContents(response.data)
      setContentFilter((current) => ({ ...current, status }))
      setActivePanel('contents')
      showFilteredResults('contents')
      showNotice(`נטענו תכנים בסטטוס ${statusLabelByValue[status]}`)
    } catch {
      showFilteredResults('contents')
      setErrors((current) => ({
        ...current,
        contents: 'לא הצלחנו לטעון תכנים לפי סטטוס',
      }))
    } finally {
      setLoading((current) => ({ ...current, contents: false }))
    }
  }

  function startContentEdit(content) {
    setEditingContentId(getContentId(content))
    setContentDraft({
      clientId: content.clientId ?? content.client_id ?? '',
      title: content.title ?? '',
      description: content.description ?? '',
      file_url: content.file_url ?? '',
      content_type: content.content_type ?? 'IMAGE',
      plannedPublishDate: toInputDateTime(content.plannedPublishDate),
    })
  }

  function handleContentDraftChange(event) {
    const { name, value } = event.target

    setContentDraft((current) => ({
      ...current,
      [name]: value,
    }))
  }

  async function handleUpdateContent(contentId) {
    const payload = {
      clientId: Number(contentDraft.clientId),
      title: contentDraft.title,
      description: contentDraft.description,
      file_url: contentDraft.file_url,
      content_type: contentDraft.content_type,
    }

    if (contentDraft.plannedPublishDate) {
      payload.plannedPublishDate = contentDraft.plannedPublishDate
    }

    setErrors((current) => ({ ...current, contents: '' }))

    try {
      await api.put(`/contents/${contentId}`, payload)
      setEditingContentId(null)
      setContentDraft(null)
      await loadContents()
      showNotice(`תוכן #${contentId} עודכן`)
    } catch {
      setErrors((current) => ({
        ...current,
        contents: 'לא הצלחנו לעדכן את התוכן',
      }))
    }
  }

  async function handleDeleteContent(contentId) {
    if (!window.confirm(`למחוק את תוכן #${contentId}?`)) {
      return
    }

    setErrors((current) => ({ ...current, contents: '' }))

    try {
      await api.delete(`/contents/${contentId}`)
      await loadContents()
      await loadComments()
      showNotice(`תוכן #${contentId} נמחק`)
    } catch {
      setErrors((current) => ({
        ...current,
        contents: 'לא הצלחנו למחוק את התוכן',
      }))
    }
  }

  async function handleUpdateStatus(contentId, status) {
    const statusEndpointByValue = {
      WAITING_APPROVAL: 'send-for-approval',
      APPROVED: 'approve',
      REJECTED: 'reject',
      PUBLISHED: 'publish',
    }
    const endpoint = statusEndpointByValue[status]

    if (!endpoint) {
      return
    }

    setErrors((current) => ({ ...current, contents: '' }))

    try {
      await api.put(`/contents/${contentId}/${endpoint}`)
      await loadContents()
      showNotice(`סטטוס תוכן #${contentId} עודכן ל${statusLabelByValue[status]}`)
    } catch {
      setErrors((current) => ({
        ...current,
        contents: 'לא ניתן לבצע את מעבר הסטטוס הזה',
      }))
    }
  }

  function getStatusActions(status) {
    if (isAdmin && status === 'DRAFT') {
      return [{ value: 'WAITING_APPROVAL', label: 'שליחה לאישור' }]
    }

    if (isClient && status === 'WAITING_APPROVAL') {
      return [
        { value: 'APPROVED', label: 'אישור' },
        { value: 'REJECTED', label: 'דחייה' },
      ]
    }

    if (isAdmin && status === 'APPROVED') {
      return [{ value: 'PUBLISHED', label: 'פרסום' }]
    }

    return []
  }

  function handleCommentFormChange(event) {
    const { name, value } = event.target

    setCommentForm((current) => ({
      ...current,
      [name]: value,
    }))
  }

  async function handleLoadCommentsByContent(contentId = commentsContentId) {
    if (!contentId) {
      return
    }

    setLoading((current) => ({ ...current, comments: true }))
    setErrors((current) => ({ ...current, comments: '' }))

    try {
      const response = await api.get('/comments/by-content', {
        params: { contentId },
      })
      setComments(response.data)
      setCommentsContentId(String(contentId))
      setCommentForm((current) => ({
        ...current,
        contentId: String(contentId),
      }))
      setActivePanel('comments')
      showFilteredResults('comments')
      showNotice(`נטענו תגובות לתוכן #${contentId}`)
    } catch {
      showFilteredResults('comments')
      setErrors((current) => ({
        ...current,
        comments: 'לא הצלחנו לטעון תגובות לתוכן',
      }))
    } finally {
      setLoading((current) => ({ ...current, comments: false }))
    }
  }

  async function handleCreateComment(event) {
    event.preventDefault()
    setSaving((current) => ({ ...current, comment: true }))
    setErrors((current) => ({ ...current, comments: '' }))

    try {
      await api.post('/comments', {
        contentId: Number(commentForm.contentId),
        commentText: commentForm.commentText,
      })

      setCommentForm((current) => ({
        ...current,
        commentText: '',
      }))

      if (commentForm.contentId) {
        await handleLoadCommentsByContent(commentForm.contentId)
      } else {
        await loadComments()
      }

      showNotice('התגובה נוספה')
    } catch {
      setErrors((current) => ({
        ...current,
        comments: 'לא הצלחנו לשמור את התגובה',
      }))
    } finally {
      setSaving((current) => ({ ...current, comment: false }))
    }
  }

  function getClientName(clientId) {
    const client = clientById.get(Number(clientId))
    return client?.business_name || `לקוח #${clientId}`
  }

  return (
    <PageShell activeRoute={activeRoute} routes={routes} onNavigate={onNavigate}>
      <section className="dashboard-layout">
        <aside className="manager-panel dashboard-summary">
          <div className="manager-photo" aria-hidden="true">
            {getProfileInitials(profile)}
          </div>

          <p className="eyebrow">ניהול תוכן ולקוחות</p>
          <h2>{profile.fullName || 'משתמש מחובר'}</h2>
          <p>{profile.username || 'שם משתמש'}</p>
          <p>{profile.email || 'אימייל'}</p>
          {profile.role && (
            <span className="role-pill">
              {isAdmin ? 'מנהל' : 'לקוח'}
            </span>
          )}
          {errors.profile && <p className="inline-error">{errors.profile}</p>}

          {isAdmin && (
            <div className="summary-metrics">
              <div>
                <strong>{clients.length}</strong>
                <span>לקוחות</span>
              </div>
              <div>
                <strong>{contents.length}</strong>
                <span>תכנים</span>
              </div>
              <div>
                <strong>{waitingApprovalCount}</strong>
                <span>ממתינים</span>
              </div>
              <div>
                <strong>{comments.length}</strong>
                <span>תגובות</span>
              </div>
            </div>
          )}

          {isClient && (
            <div className="summary-metrics client-summary-metrics">
              <div>
                <strong>{comments.length}</strong>
                <span>תגובות</span>
              </div>
            </div>
          )}
        </aside>

        <section className="workspace-panel">
          <div className="workspace-tabs" aria-label="אזורי ניהול">
            {isAdmin && (
              <button
                className={visiblePanel === 'clients' ? 'active' : ''}
                type="button"
                onClick={() => setActivePanel('clients')}
              >
                לקוחות
              </button>
            )}
            <button
              className={visiblePanel === 'contents' ? 'active' : ''}
              type="button"
              onClick={() => setActivePanel('contents')}
            >
              תכנים
            </button>
            <button
              className={visiblePanel === 'comments' ? 'active' : ''}
              type="button"
              onClick={() => setActivePanel('comments')}
            >
              תגובות
            </button>
          </div>

          {notice && (
            <div className="notice-bar">
              <span>{notice}</span>
              <button type="button" onClick={() => setNotice('')}>
                סגירה
              </button>
            </div>
          )}

          {isAdmin && visiblePanel === 'clients' && (
            <section className="management-section" aria-labelledby="clients-title">
              <div className="management-header">
                <div>
                  <p className="eyebrow">לקוחות</p>
                  <h2 id="clients-title">ניהול לקוחות</h2>
                </div>
                <button type="button" className="secondary-button" onClick={loadClients}>
                  כל הלקוחות
                </button>
              </div>

              <div className="tool-row filter-grid">
                <div className="filter-control">
                  <label>
                    מזהה לקוח
                    <input
                      min="1"
                      type="number"
                      value={clientLookupId}
                      onChange={(event) => setClientLookupId(event.target.value)}
                    />
                  </label>
                  <button type="button" className="secondary-button" onClick={handleFindClientById}>
                    חיפוש לפי מזהה לקוח
                  </button>
                </div>
              </div>

              {loading.clients && <p className="entity-state">טוען לקוחות...</p>}
              {errors.clients && <p className="entity-state entity-state-error">{errors.clients}</p>}
              {!loading.clients && !errors.clients && clients.length === 0 && (
                <p className="entity-state">אין לקוחות להצגה</p>
              )}

              {filteredResults.clients && !loading.clients && !errors.clients && clients.length > 0 && (
                <div className="result-actions">
                  <button type="button" className="ghost-button" onClick={() => toggleResults('clients')}>
                    {resultsHidden.clients ? 'הצגת תוצאות' : 'הסתרת תוצאות'}
                  </button>
                </div>
              )}

              {!resultsHidden.clients && (
                <div className="entity-list">
                  {clients.map((client) => {
                    const clientId = getClientId(client)
                    const isEditing = editingClientId === clientId

                    return (
                      <article className="entity-card" key={clientId}>
                        <div className="entity-mark" aria-hidden="true">
                          {getClientInitial(client)}
                        </div>
                        <div className="entity-details">
                          <div className="entity-title-row">
                            <h3>{client.business_name}</h3>
                            <span className="channel-pill">לקוח #{clientId}</span>
                          </div>

                          {isEditing ? (
                            <div className="inline-edit-grid">
                              <label>
                                שם העסק
                                <input
                                  name="businessName"
                                  value={clientDraft.businessName}
                                  onChange={handleClientDraftChange}
                                />
                              </label>
                              <label>
                                טלפון
                                <input
                                  name="phone"
                                  value={clientDraft.phone}
                                  onChange={handleClientDraftChange}
                                />
                              </label>
                              <label>
                                User ID
                                <input
                                  min="1"
                                  name="userId"
                                  type="number"
                                  value={clientDraft.userId}
                                  onChange={handleClientDraftChange}
                                />
                              </label>
                              <label>
                                Admin ID
                                <input
                                  min="1"
                                  name="adminId"
                                  type="number"
                                  value={clientDraft.adminId}
                                  onChange={handleClientDraftChange}
                                />
                              </label>
                            </div>
                          ) : (
                            <div className="metadata-row">
                              <span>User ID: {client.user_id ?? '-'}</span>
                              <span>Admin ID: {client.admin_id ?? '-'}</span>
                              <span className="phone-number">טלפון: {client.phone || '-'}</span>
                            </div>
                          )}
                        </div>

                        <div className="entity-actions">
                          {isEditing ? (
                            <>
                              <button
                                type="button"
                                className="primary-button small-button"
                                onClick={() => handleUpdateClient(clientId)}
                              >
                                שמירה
                              </button>
                              <button
                                type="button"
                                className="ghost-button small-button"
                                onClick={() => {
                                  setEditingClientId(null)
                                  setClientDraft(null)
                                }}
                              >
                                ביטול
                              </button>
                            </>
                          ) : (
                            <>
                              <button
                                type="button"
                                className="secondary-button small-button"
                                onClick={() => startClientEdit(client)}
                              >
                                עריכה
                              </button>
                              <button
                                type="button"
                                className="secondary-button small-button"
                                onClick={() => handleLoadContentsByClient(clientId)}
                              >
                                תכנים
                              </button>
                              <button
                                type="button"
                                className="danger-button small-button"
                                onClick={() => handleDeleteClient(clientId)}
                              >
                                מחיקה
                              </button>
                            </>
                          )}
                        </div>
                      </article>
                    )
                  })}
                </div>
              )}

              <div className="create-toggle-bar">
                <button type="button" className="primary-button" onClick={() => toggleCreateForm('clients')}>
                  {showCreateForm.clients ? 'סגירת יצירת לקוח' : 'יצירת לקוח חדש'}
                </button>
              </div>

              {showCreateForm.clients && (
                <form className="entity-form" onSubmit={handleCreateClient}>
                  <h3>יצירת לקוח</h3>
                  <div className="form-grid">
                    <label>
                      שם העסק
                      <input
                        name="businessName"
                        value={clientForm.businessName}
                        onChange={handleClientFormChange}
                        required
                      />
                    </label>
                    <label>
                      שם מלא
                      <input
                        name="fullName"
                        value={clientForm.fullName}
                        onChange={handleClientFormChange}
                      />
                    </label>
                    <label>
                      אימייל
                      <input
                        name="email"
                        type="email"
                        value={clientForm.email}
                        onChange={handleClientFormChange}
                        required
                      />
                    </label>
                    <label>
                      שם משתמש
                      <input
                        name="username"
                        value={clientForm.username}
                        onChange={handleClientFormChange}
                        required
                      />
                    </label>
                    <label>
                      סיסמה
                      <input
                        name="password"
                        type="password"
                        value={clientForm.password}
                        onChange={handleClientFormChange}
                        required
                      />
                    </label>
                    <label>
                      טלפון
                      <input
                        name="phone"
                        value={clientForm.phone}
                        onChange={handleClientFormChange}
                      />
                    </label>
                    <label>
                      מזהה מנהל
                      <input
                        min="1"
                        name="adminId"
                        type="number"
                        value={clientForm.adminId}
                        onChange={handleClientFormChange}
                      />
                    </label>
                  </div>
                  <button className="primary-button" type="submit" disabled={saving.client}>
                    {saving.client ? 'שומר...' : 'שמירת לקוח'}
                  </button>
                </form>
              )}
            </section>
          )}

          {visiblePanel === 'contents' && (
            <section className="management-section" aria-labelledby="contents-title">
              <div className="management-header">
                <div>
                  <p className="eyebrow">תכנים</p>
                  <h2 id="contents-title">ניהול תכנים</h2>
                </div>
                <button type="button" className="secondary-button" onClick={loadContents}>
                  כל התכנים
                </button>
              </div>

              <div className="tool-row tool-row-wide filter-grid">
                <div className="filter-control">
                  <label>
                    מזהה תוכן
                    <input
                      min="1"
                      name="contentId"
                      type="number"
                      value={contentFilter.contentId}
                      onChange={handleContentFilterChange}
                    />
                  </label>
                  <button type="button" className="secondary-button" onClick={handleFindContentById}>
                    חיפוש לפי מזהה תוכן
                  </button>
                </div>
                {isAdmin && (
                  <div className="filter-control">
                    <label>
                      לקוח
                      <select
                        name="clientId"
                        value={contentFilter.clientId}
                        onChange={handleContentFilterChange}
                      >
                        <option value="">בחירת לקוח</option>
                        {clients.map((client) => (
                          <option value={getClientId(client)} key={getClientId(client)}>
                            {client.business_name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => handleLoadContentsByClient()}
                    >
                      חיפוש לפי לקוח
                    </button>
                  </div>
                )}
                <div className="filter-control">
                  <label>
                    סטטוס
                    <select
                      name="status"
                      value={contentFilter.status}
                      onChange={handleContentFilterChange}
                    >
                      <option value="">בחירת סטטוס</option>
                      {statusOptions.map((status) => (
                        <option value={status.value} key={status.value}>
                          {status.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleLoadContentsByStatus()}
                  >
                    חיפוש לפי סטטוס
                  </button>
                </div>
              </div>

              {loading.contents && <p className="entity-state">טוען תכנים...</p>}
              {errors.contents && <p className="entity-state entity-state-error">{errors.contents}</p>}
              {!loading.contents && !errors.contents && contents.length === 0 && (
                <p className="entity-state">אין תכנים להצגה</p>
              )}

              {filteredResults.contents && !loading.contents && !errors.contents && contents.length > 0 && (
                <div className="result-actions">
                  <button type="button" className="ghost-button" onClick={() => toggleResults('contents')}>
                    {resultsHidden.contents ? 'הצגת תוצאות' : 'הסתרת תוצאות'}
                  </button>
                </div>
              )}

              {!resultsHidden.contents && (
                <div className="entity-list">
                  {contents.map((content) => {
                    const contentId = getContentId(content)
                    const contentClientId = content.clientId ?? content.client_id
                    const isEditing = editingContentId === contentId
                    const statusActions = getStatusActions(content.status)

                    return (
                      <article className="entity-card content-card" key={contentId}>
                        <div className={`status-rail status-${content.status || 'DRAFT'}`} />
                        <div className="entity-details">
                          <div className="entity-title-row">
                            <h3>{content.title}</h3>
                            <span className={`status-pill status-${content.status || 'DRAFT'}`}>
                              {statusLabelByValue[content.status] || content.status || 'טיוטה'}
                            </span>
                          </div>

                          {isEditing ? (
                            <div className="inline-edit-grid">
                              <label>
                                לקוח
                                <select
                                  name="clientId"
                                  value={contentDraft.clientId}
                                  onChange={handleContentDraftChange}
                                >
                                  {clients.map((client) => (
                                    <option value={getClientId(client)} key={getClientId(client)}>
                                      {client.business_name}
                                    </option>
                                  ))}
                                </select>
                              </label>
                              <label>
                                כותרת
                                <input
                                  name="title"
                                  value={contentDraft.title}
                                  onChange={handleContentDraftChange}
                                />
                              </label>
                              <label>
                                סוג
                                <select
                                  name="content_type"
                                  value={contentDraft.content_type}
                                  onChange={handleContentDraftChange}
                                >
                                  {contentTypeOptions.map((type) => (
                                    <option value={type.value} key={type.value}>
                                      {type.label}
                                    </option>
                                  ))}
                                </select>
                              </label>
                              <label>
                                תאריך פרסום
                                <input
                                  name="plannedPublishDate"
                                  type="datetime-local"
                                  value={contentDraft.plannedPublishDate}
                                  onChange={handleContentDraftChange}
                                />
                              </label>
                              <label>
                                קישור
                                <input
                                  name="file_url"
                                  value={contentDraft.file_url}
                                  onChange={handleContentDraftChange}
                                />
                              </label>
                              <label className="wide-field">
                                תיאור
                                <textarea
                                  name="description"
                                  value={contentDraft.description}
                                  onChange={handleContentDraftChange}
                                />
                              </label>
                            </div>
                          ) : (
                            <>
                              <p>{content.description || 'אין תיאור'}</p>
                              <div className="metadata-row">
                                <span>{getClientName(contentClientId)}</span>
                                <span>{typeLabelByValue[content.content_type] || content.content_type}</span>
                                <span>תוכן #{contentId}</span>
                                <span>
                                  פרסום:{' '}
                                  {content.plannedPublishDate
                                    ? toInputDateTime(content.plannedPublishDate).replace('T', ' ')
                                    : '-'}
                                </span>
                              </div>
                              {content.file_url && (
                                <a className="file-link" href={content.file_url} target="_blank" rel="noreferrer">
                                  קובץ מצורף
                                </a>
                              )}
                            </>
                          )}
                        </div>

                        <div className="entity-actions">
                          {isEditing ? (
                            <>
                              <button
                                type="button"
                                className="primary-button small-button"
                                onClick={() => handleUpdateContent(contentId)}
                              >
                                שמירה
                              </button>
                              <button
                                type="button"
                                className="ghost-button small-button"
                                onClick={() => {
                                  setEditingContentId(null)
                                  setContentDraft(null)
                                }}
                              >
                                ביטול
                              </button>
                            </>
                          ) : (
                            <>
                              {isAdmin && (
                                <button
                                  type="button"
                                  className="secondary-button small-button"
                                  onClick={() => startContentEdit(content)}
                                >
                                  עריכה
                                </button>
                              )}
                              {statusActions.map((action) => (
                                <button
                                  type="button"
                                  className="secondary-button small-button"
                                  key={action.value}
                                  onClick={() => handleUpdateStatus(contentId, action.value)}
                                >
                                  {action.label}
                                </button>
                              ))}
                              <button
                                type="button"
                                className="secondary-button small-button"
                                onClick={() => handleLoadCommentsByContent(contentId)}
                              >
                                תגובות
                              </button>
                              {isAdmin && (
                                <button
                                  type="button"
                                  className="danger-button small-button"
                                  onClick={() => handleDeleteContent(contentId)}
                                >
                                  מחיקה
                                </button>
                              )}
                            </>
                          )}
                        </div>
                      </article>
                    )
                  })}
                </div>
              )}

              {isAdmin && (
                <>
                  <div className="create-toggle-bar">
                    <button type="button" className="primary-button" onClick={() => toggleCreateForm('contents')}>
                      {showCreateForm.contents ? 'סגירת יצירת תוכן' : 'יצירת תוכן חדש'}
                    </button>
                  </div>

                  {showCreateForm.contents && (
                    <form className="entity-form" onSubmit={handleCreateContent}>
                      <h3>יצירת תוכן</h3>
                      <div className="form-grid">
                        <label>
                          לקוח
                          <select
                            name="clientId"
                            value={contentForm.clientId}
                            onChange={handleContentFormChange}
                            required
                          >
                            <option value="">בחירת לקוח</option>
                            {clients.map((client) => (
                              <option value={getClientId(client)} key={getClientId(client)}>
                                {client.business_name}
                              </option>
                            ))}
                          </select>
                        </label>
                        <label>
                          כותרת
                          <input
                            name="title"
                            value={contentForm.title}
                            onChange={handleContentFormChange}
                            required
                          />
                        </label>
                        <label>
                          סוג תוכן
                          <select
                            name="content_type"
                            value={contentForm.content_type}
                            onChange={handleContentFormChange}
                          >
                            {contentTypeOptions.map((type) => (
                              <option value={type.value} key={type.value}>
                                {type.label}
                              </option>
                            ))}
                          </select>
                        </label>
                        <label>
                          תאריך פרסום מתוכנן
                          <input
                            name="plannedPublishDate"
                            type="datetime-local"
                            value={contentForm.plannedPublishDate}
                            onChange={handleContentFormChange}
                          />
                        </label>
                        <label>
                          קישור לקובץ
                          <input
                            name="file_url"
                            value={contentForm.file_url}
                            onChange={handleContentFormChange}
                          />
                        </label>
                        <label className="wide-field">
                          תיאור
                          <textarea
                            name="description"
                            value={contentForm.description}
                            onChange={handleContentFormChange}
                          />
                        </label>
                      </div>
                      <button className="primary-button" type="submit" disabled={saving.content}>
                        {saving.content ? 'שומר...' : 'שמירת תוכן'}
                      </button>
                    </form>
                  )}
                </>
              )}
            </section>
          )}

          {visiblePanel === 'comments' && (
            <section className="management-section" aria-labelledby="comments-title">
              <div className="management-header">
                <div>
                  <p className="eyebrow">תגובות</p>
                  <h2 id="comments-title">ניהול תגובות</h2>
                </div>
                <button type="button" className="secondary-button" onClick={loadComments}>
                  כל התגובות
                </button>
              </div>

              <div className="tool-row filter-grid">
                <div className="filter-control">
                  <label>
                    תוכן
                    <select
                      value={commentsContentId}
                      onChange={(event) => setCommentsContentId(event.target.value)}
                    >
                      <option value="">בחירת תוכן</option>
                      {contents.map((content) => (
                        <option value={getContentId(content)} key={getContentId(content)}>
                          {content.title}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleLoadCommentsByContent()}
                  >
                    הצגת תגובות לתוכן
                  </button>
                </div>
              </div>

              <form className="entity-form" onSubmit={handleCreateComment}>
                <h3>הוספת תגובה</h3>
                <div className="form-grid">
                  <label>
                    תוכן
                    <select
                      name="contentId"
                      value={commentForm.contentId}
                      onChange={handleCommentFormChange}
                      required
                    >
                      <option value="">בחירת תוכן</option>
                      {contents.map((content) => (
                        <option value={getContentId(content)} key={getContentId(content)}>
                          {content.title}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="wide-field">
                    תגובה
                    <textarea
                      name="commentText"
                      value={commentForm.commentText}
                      onChange={handleCommentFormChange}
                      required
                    />
                  </label>
                </div>
                <button className="primary-button" type="submit" disabled={saving.comment}>
                  {saving.comment ? 'שומר...' : 'שמירת תגובה'}
                </button>
              </form>

              {loading.comments && <p className="entity-state">טוען תגובות...</p>}
              {errors.comments && <p className="entity-state entity-state-error">{errors.comments}</p>}
              {!loading.comments && !errors.comments && comments.length === 0 && (
                <p className="entity-state">אין תגובות להצגה</p>
              )}

              {filteredResults.comments && !loading.comments && !errors.comments && comments.length > 0 && (
                <div className="result-actions">
                  <button type="button" className="ghost-button" onClick={() => toggleResults('comments')}>
                    {resultsHidden.comments ? 'הצגת תוצאות' : 'הסתרת תוצאות'}
                  </button>
                </div>
              )}

              {!resultsHidden.comments && (
                <div className="comment-list">
                  {comments.map((comment) => (
                    <article className="comment-item" key={comment.commentId}>
                      <div>
                        <h3>תגובה #{comment.commentId}</h3>
                        <p>{comment.commentText}</p>
                      </div>
                      <div className="metadata-row">
                        <span>תוכן #{comment.contentId}</span>
                        <span>User #{comment.userId}</span>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          )}
        </section>
      </section>
    </PageShell>
  )
}

export default DashboardPage
