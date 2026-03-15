import { apiRequest } from '../api'
import { CoordinationState, CoordinationSummaryDto } from './dashboard'

export interface PaginatedResponse<T> {
  content: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  isFirst: boolean
  isLast: boolean
  hasNext: boolean
  hasPrevious: boolean
}

export const coordinationsAPI = {
  getHistory: async (
    username: string, 
    page: number = 0, 
    size: number = 20, 
    status?: string
  ): Promise<PaginatedResponse<CoordinationSummaryDto>> => {
    let url = `/api/coordinations?username=${encodeURIComponent(username)}&page=${page}&size=${size}`
    if (status && status !== 'ALL') {
      url += `&status=${encodeURIComponent(status)}`
    }
    return apiRequest<PaginatedResponse<CoordinationSummaryDto>>(url)
  },

  getDetail: async (id: string, username: string): Promise<any> => {
    return apiRequest<any>(`/api/coordinations/${encodeURIComponent(id)}?username=${encodeURIComponent(username)}`)
  }
}
