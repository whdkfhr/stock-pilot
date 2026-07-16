import { api } from './client'
import type { Recommendation } from '@/types'

export const recommendationsApi = {
  get() {
    return api.get<Recommendation>('/recommendations')
  },
}
