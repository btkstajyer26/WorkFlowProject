import type { GetAllRecordsData, GetAllRecordsParams } from './generated/data-contracts'
import { apiHttpClient } from './client'

export type RecordListQuery = Omit<GetAllRecordsParams, 'pageable'> & {
  page?: number
  size?: number
  sort?: string[]
}

/**
 * Spring Pageable gerçekte page/size/sort query parametreleriyle çalışır.
 * Mevcut OpenAPI çıktısı bunları hatalı biçimde tek bir `pageable` nesnesi
 * olarak gösterdiği için bu uyumluluk adaptörü yalnızca liste isteğini düzeltir.
 */
export function listRecords({ page = 0, size = 10, sort, ...filters }: RecordListQuery = {}) {
  return apiHttpClient.request<GetAllRecordsData>({
    path: '/api/records',
    method: 'GET',
    query: { ...filters, page, size, sort },
    secure: true,
  })
}
