import { describe, expect, it } from 'vitest'
import { emptyContentFilters, filterContents } from './contentFilters.js'

const clients = new Map([[1, { business_name: 'Otzar social' }], [2, { business_name: 'Second' }]])
const items = [
  { contentId: 101, clientId: 1, title: 'Approved image', description: 'Campaign', status: 'APPROVED', contentType: 'IMAGE' },
  { contentId: 102, clientId: 1, title: 'Waiting video', description: 'Launch', status: 'WAITING_APPROVAL', contentType: 'VIDEO' },
  { contentId: 103, clientId: 2, title: 'Other image', description: 'Campaign', status: 'APPROVED', contentType: 'IMAGE' },
]

describe('content management combined filters', () => {
  const apply = (changes) => filterContents(items, { ...emptyContentFilters, ...changes }, clients)

  it('filters by client and status independently', () => {
    expect(apply({ clientId: '1' }).map((item) => item.contentId)).toEqual([101, 102])
    expect(apply({ status: 'APPROVED' }).map((item) => item.contentId)).toEqual([101, 103])
  })

  it('combines client, status, and content type with AND semantics', () => {
    expect(apply({ clientId: '1', status: 'APPROVED', contentType: 'IMAGE' }).map((item) => item.contentId)).toEqual([101])
  })

  it('changes or clears one filter without altering the others', () => {
    const selected = { ...emptyContentFilters, clientId: '1', status: 'APPROVED', contentType: 'IMAGE' }
    expect(filterContents(items, { ...selected, status: 'WAITING_APPROVAL' }, clients).map((item) => item.contentId)).toEqual([])
    expect(filterContents(items, { ...selected, status: '' }, clients).map((item) => item.contentId)).toEqual([101])
    expect(selected.clientId).toBe('1')
    expect(selected.contentType).toBe('IMAGE')
  })

  it('searches human-readable fields and clear-all restores defaults', () => {
    expect(apply({ search: 'otzar' }).map((item) => item.contentId)).toEqual([101, 102])
    expect(filterContents(items, emptyContentFilters, clients)).toEqual(items)
  })

  it('has no user-facing content ID filter state', () => {
    expect(emptyContentFilters).not.toHaveProperty('contentId')
  })
})
