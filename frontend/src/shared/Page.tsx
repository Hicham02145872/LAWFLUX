import type { PropsWithChildren } from 'react'

export function Page({ title, children }: PropsWithChildren<{ title: string }>) {
  return (
    <section className="page" aria-labelledby="page-title">
      <h1 id="page-title">{title}</h1>
      {children}
    </section>
  )
}

export function PendingPage({ title }: { title: string }) {
  return <Page title={title}><p>Écran à développer.</p></Page>
}
