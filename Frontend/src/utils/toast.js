export function showToast(message, type = 'success') {
  window.dispatchEvent(new CustomEvent('sscm:toast', { detail: { message, type } }))
}
