export type DocumentType = 'PDF' | 'DOCX'

export interface DocumentItem {
  id: string
  name: string
  type: DocumentType
  author: string
  createdAt: string
}