import { useCallback, useEffect, useRef, useState } from 'react'
import { errorText } from './format'

export type Loader<T> = {
  data: T | null
  error: string | null
  loading: boolean
  reload: () => void
}

type State<T> = {
  key: string | null
  data: T | null
  error: string | null
}

export function useLoader<T>(load: () => Promise<T>, deps: unknown[]): Loader<T> {
  const loadRef = useRef(load)
  const [version, setVersion] = useState(0)
  const [state, setState] = useState<State<T>>({ key: null, data: null, error: null })
  const key = JSON.stringify([...deps, version])

  useEffect(() => {
    loadRef.current = load
  })

  useEffect(() => {
    let active = true
    loadRef
      .current()
      .then((data) => {
        if (active) setState({ key, data, error: null })
      })
      .catch((err: unknown) => {
        if (active) {
          setState((previous) => ({ key, data: previous.data, error: errorText(err, 'Daten konnten nicht geladen werden.') }))
        }
      })
    return () => {
      active = false
    }
  }, [key])

  const reload = useCallback(() => setVersion((value) => value + 1), [])

  return { data: state.data, error: state.error, loading: state.key !== key, reload }
}
