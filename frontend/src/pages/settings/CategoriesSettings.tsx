import { useState, type FormEvent } from 'react'
import { categoriesApi } from '../../api/finance'
import type { Category } from '../../api/types'
import { Modal } from '../../components/Modal'
import { EmptyState, ErrorText, FormActions, PageHeader } from '../../components/ui'
import { errorText } from '../../lib/format'
import { useLoader } from '../../lib/useLoader'

type Editing = { category: Category | null }

export function CategoriesSettings() {
  const categories = useLoader(() => categoriesApi.list(), [])
  const [editing, setEditing] = useState<Editing | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete(category: Category) {
    if (!window.confirm(`Kategorie „${category.name}“ löschen? Buchungen bleiben erhalten, verlieren aber die Kategorie.`)) {
      return
    }
    setError(null)
    try {
      await categoriesApi.remove(category.id)
      categories.reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  return (
    <>
      <PageHeader
        title="Kategorien"
        subtitle="Ordne Einnahmen und Ausgaben Kategorien zu, z. B. Miete, Lebensmittel oder Gehalt."
        actions={
          <button type="button" onClick={() => setEditing({ category: null })}>
            Neue Kategorie
          </button>
        }
      />
      <ErrorText message={error ?? categories.error} />
      {categories.data && categories.data.length === 0 && <EmptyState>Noch keine Kategorien angelegt.</EmptyState>}
      {categories.data && categories.data.length > 0 && (
        <ul className="item-list">
          {categories.data.map((category) => (
            <li key={category.id}>
              <span className="item-title">{category.name}</span>
              <div className="row-actions">
                <button type="button" className="link" onClick={() => setEditing({ category })}>
                  Bearbeiten
                </button>
                <button type="button" className="link danger" onClick={() => handleDelete(category)}>
                  Löschen
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {editing && (
        <CategoryForm
          category={editing.category}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null)
            categories.reload()
          }}
        />
      )}
    </>
  )
}

function CategoryForm({ category, onClose, onSaved }: { category: Category | null; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(category?.name ?? '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!name.trim()) {
      setError('Bitte einen Namen eingeben.')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      if (category) {
        await categoriesApi.update(category.id, { name: name.trim() })
      } else {
        await categoriesApi.create({ name: name.trim() })
      }
      onSaved()
    } catch (err) {
      setError(errorText(err))
      setSubmitting(false)
    }
  }

  return (
    <Modal title={category ? 'Kategorie bearbeiten' : 'Neue Kategorie'} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input value={name} maxLength={255} onChange={(event) => setName(event.target.value)} autoFocus />
        </label>
        <ErrorText message={error} />
        <FormActions submitting={submitting} label="Speichern" onCancel={onClose} />
      </form>
    </Modal>
  )
}
