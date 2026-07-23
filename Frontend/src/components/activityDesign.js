export const activityDesign = {
  CONTENT_CREATED: {
    title: 'התוכן נוצר',
    description: 'נוצר פריט תוכן חדש והתווספה לו גרסה ראשונה.',
    icon: '+',
  },
  CONTENT_UPDATED: {
    title: 'התוכן עודכן',
    description: 'פרטי התוכן עודכנו ונשמרה גרסה חדשה.',
    icon: '✎',
  },
  SENT_FOR_APPROVAL: {
    title: 'התוכן נשלח לאישור',
    description: 'התוכן מוכן לבדיקה והועבר לאישור הלקוח.',
    icon: '↗',
  },
  APPROVED: {
    title: 'התוכן אושר',
    description: 'הלקוח אישר את התוכן להמשך התהליך.',
    icon: '✓',
  },
  REJECTED: {
    title: 'התוכן נדחה',
    description: 'התוכן הוחזר לעדכון לאחר בדיקת הלקוח.',
    icon: '×',
  },
  SCHEDULED: {
    title: 'מועד הפרסום עודכן',
    description: 'נקבע או עודכן מועד הפרסום המתוכנן.',
    icon: '◷',
  },
  PUBLISHED: {
    title: 'התוכן פורסם',
    description: 'התוכן סומן כפרסום שהושלם.',
    icon: '●',
  },
}

export function getActivityDesign(type) {
  return activityDesign[type] || {
    title: 'פעילות בתוכן',
    description: 'בוצע עדכון בפריט התוכן.',
    icon: '•',
  }
}

export function formatRelativeActivityTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'מועד לא זמין'
  const differenceMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000))
  if (differenceMinutes < 1) return 'עכשיו'
  if (differenceMinutes < 60) return `לפני ${differenceMinutes} דקות`
  const differenceHours = Math.floor(differenceMinutes / 60)
  if (differenceHours < 24) return `לפני ${differenceHours} שעות`
  return date.toLocaleString('he-IL', { dateStyle: 'short', timeStyle: 'short' })
}
