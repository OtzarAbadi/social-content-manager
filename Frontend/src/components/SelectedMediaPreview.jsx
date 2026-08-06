import { useEffect, useMemo } from 'react'
import MediaPreview from './MediaPreview.jsx'

function SelectedMediaPreview({ file, alt }) {
  const url=useMemo(()=>URL.createObjectURL(file),[file])
  useEffect(()=>()=>URL.revokeObjectURL(url),[url])
  return <MediaPreview path={url} type={file.type.startsWith('video/')?'VIDEO':'IMAGE'} alt={alt}/>
}
export default SelectedMediaPreview
