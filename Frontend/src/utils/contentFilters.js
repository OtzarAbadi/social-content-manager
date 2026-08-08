export const emptyContentFilters = {
  clientId: '',
  status: '',
  contentType: '',
  search: '',
}

export function filterContents(items, filters, clientById = new Map()) {
  const query = filters.search.trim().toLocaleLowerCase('he-IL')
  return items.filter((content) => {
    const clientId = content.clientId ?? content.client_id
    const contentType = content.contentType ?? content.content_type
    const clientName = clientById.get(Number(clientId))?.business_name
    return (!filters.clientId || Number(clientId) === Number(filters.clientId))
      && (!filters.status || content.status === filters.status)
      && (!filters.contentType || contentType === filters.contentType)
      && (!query || [content.title, content.description, clientName, content.status, contentType]
        .some((value) => String(value || '').toLocaleLowerCase('he-IL').includes(query)))
  })
}
