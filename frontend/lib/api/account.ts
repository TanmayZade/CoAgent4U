import { apiRequest } from '../api'

export const accountAPI = {
  exportDataUrl: (username: string): string => {
    return `/api/account/export?username=${encodeURIComponent(username)}`
  },

  deleteAccount: async (username: string): Promise<void> => {
    await apiRequest(`/api/account?username=${encodeURIComponent(username)}`, {
      method: 'DELETE'
    })
  }
}
