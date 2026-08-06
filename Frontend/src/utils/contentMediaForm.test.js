import { describe, expect, it } from 'vitest'
import { appendMediaFiles, validateMediaSelection } from './contentMediaForm.js'

const image = (name = 'image.jpg') => new File(['image'], name, { type: 'image/jpeg' })
const video = (name = 'video.mp4') => new File(['video'], name, { type: 'video/mp4' })

describe('content media form', () => {
  it('rejects video in IMAGE mode', () => expect(validateMediaSelection('IMAGE', [video()])).toBeTruthy())
  it('rejects image in VIDEO mode', () => expect(validateMediaSelection('VIDEO', [image()])).toBeTruthy())
  it('accepts an ordered image and video in mixed mode', () => expect(validateMediaSelection('MIXED', [image(), video()])).toBe(''))
  it('rejects image-only mixed media', () => expect(validateMediaSelection('MIXED', [image()])).toBeTruthy())
  it('rejects video-only mixed media', () => expect(validateMediaSelection('MIXED', [video()])).toBeTruthy())
  it('appends every file under the repeated files field in order', () => {
    const entries = []
    appendMediaFiles({ append: (name, value) => entries.push([name, value.name]) }, [image('first.jpg'), video('second.mp4'), image('third.png')])
    expect(entries).toEqual([['files', 'first.jpg'], ['files', 'second.mp4'], ['files', 'third.png']])
  })
})
