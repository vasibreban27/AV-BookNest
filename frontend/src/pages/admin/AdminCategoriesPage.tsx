import { useState, type FormEvent } from 'react'
import { AdminBadge, AdminModal, AdminNotice, AdminPageHeader, AdminPanel, AdminState, ReasonDialog } from '../../components/admin/AdminUi'
import { useAdminCategories, useCreateAdminCategory, useSetAdminCategoryActive, useUpdateAdminCategory } from '../../features/admin/hooks/useAdmin'
import type { AdminCategory } from '../../features/admin/types/admin.types'
import { formatAdminDate, getAdminErrorMessage } from '../../features/admin/utils/adminFormatters'

type CategoryAction = { category: AdminCategory; active: boolean }

export function AdminCategoriesPage() {
  const categories = useAdminCategories()
  const createCategory = useCreateAdminCategory()
  const updateCategory = useUpdateAdminCategory()
  const setActive = useSetAdminCategoryActive()
  const [editor, setEditor] = useState<'create' | AdminCategory | null>(null)
  const [activeAction, setActiveAction] = useState<CategoryAction | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const handleSave = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!editor) return
    const form = new FormData(event.currentTarget)
    const name = String(form.get('name')).trim()
    const description = String(form.get('description')).trim()
    setError(null)
    try {
      if (editor === 'create') await createCategory.mutateAsync({ name, slug: String(form.get('slug')).trim(), description })
      else await updateCategory.mutateAsync({ categoryId: editor.id, payload: { name, description } })
      setNotice(editor === 'create' ? 'Categoria a fost creată.' : 'Categoria a fost actualizată.')
      setEditor(null)
    } catch (mutationError) { setError(getAdminErrorMessage(mutationError)) }
  }

  const handleActive = async (reason: string) => {
    if (!activeAction) return
    setError(null)
    try {
      await setActive.mutateAsync({ categoryId: activeAction.category.id, active: activeAction.active, reason })
      setNotice(activeAction.active ? 'Categoria a fost activată.' : 'Categoria și anunțurile sale au fost retrase din catalogul public.')
      setActiveAction(null)
    } catch (mutationError) { setError(getAdminErrorMessage(mutationError)) }
  }

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Taxonomie" title="Categorii" description="Păstrează catalogul simplu și coerent. Categoriile dezactivate nu mai apar public și nu acceptă anunțuri noi." actions={<button type="button" className="admin-button" onClick={() => { setEditor('create'); setError(null) }}>+ Categorie nouă</button>} />
      {notice && <AdminNotice tone="success" onClose={() => setNotice(null)}>{notice}</AdminNotice>}
      <AdminPanel title="Structura catalogului" description={`${categories.data?.length ?? 0} categorii configurate`}>
        {categories.isLoading && <AdminState kind="loading" title="Încărcăm categoriile" message="Pregătim structura catalogului." />}
        {categories.isError && <AdminState kind="error" title="Categoriile nu pot fi încărcate" message={getAdminErrorMessage(categories.error)} onRetry={() => void categories.refetch()} />}
        {categories.data?.length === 0 && <AdminState kind="empty" title="Nicio categorie" message="Creează prima categorie pentru catalog." />}
        {categories.data && categories.data.length > 0 && <div className="admin-category-grid">{categories.data.map((category) => <article className={`admin-category-card${category.active ? '' : ' admin-category-card--inactive'}`} key={category.id}>
          <header><span aria-hidden="true">{category.name.slice(0, 2).toUpperCase()}</span><AdminBadge tone={category.active ? 'success' : 'neutral'}>{category.active ? 'Activă' : 'Inactivă'}</AdminBadge></header>
          <h3>{category.name}</h3><code>/{category.slug}</code><p>{category.description || 'Fără descriere.'}</p><small>Creată {formatAdminDate(category.createdAt)}</small>
          <footer><button type="button" className="admin-button admin-button--compact admin-button--secondary" onClick={() => { setEditor(category); setError(null) }}>Editează</button><button type="button" className={`admin-button admin-button--compact ${category.active ? 'admin-button--ghost-danger' : 'admin-button--ghost'}`} onClick={() => { setActiveAction({ category, active: !category.active }); setError(null) }}>{category.active ? 'Dezactivează' : 'Activează'}</button></footer>
        </article>)}</div>}
      </AdminPanel>

      {editor && <AdminModal title={editor === 'create' ? 'Categorie nouă' : 'Editează categoria'} description={editor === 'create' ? 'Slug-ul va fi folosit în URL și nu va mai putea fi modificat ulterior.' : `Slug permanent: /${editor.slug}`} onClose={() => { setEditor(null); setError(null) }} size="small"><form className="admin-form" onSubmit={(event) => void handleSave(event)}><label><span>Nume</span><input name="name" required maxLength={100} defaultValue={editor === 'create' ? '' : editor.name} autoFocus /></label>{editor === 'create' && <label><span>Slug</span><input name="slug" required maxLength={120} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" placeholder="literatura-romana" /></label>}<label><span>Descriere</span><textarea name="description" maxLength={500} rows={4} defaultValue={editor === 'create' ? '' : editor.description ?? ''} /></label>{error && <AdminNotice tone="error">{error}</AdminNotice>}<div className="admin-form__actions"><button type="button" className="admin-button admin-button--ghost" onClick={() => setEditor(null)}>Renunță</button><button type="submit" className="admin-button" disabled={createCategory.isPending || updateCategory.isPending}>{createCategory.isPending || updateCategory.isPending ? 'Se salvează...' : 'Salvează categoria'}</button></div></form></AdminModal>}
      {activeAction && <ReasonDialog title={activeAction.active ? 'Activează categoria' : 'Dezactivează categoria'} description={activeAction.active ? 'Categoria și anunțurile eligibile vor redeveni vizibile în catalog.' : 'Categoria va dispărea din catalogul public, iar anunțurile existente nu vor mai fi afișate.'} confirmLabel={activeAction.active ? 'Activează' : 'Dezactivează'} danger={!activeAction.active} pending={setActive.isPending} error={error} onClose={() => { setActiveAction(null); setError(null) }} onConfirm={(reason) => void handleActive(reason)} />}
    </main>
  )
}
