import { http, HttpResponse } from 'msw'
import { apiBaseUrl } from '../../../api/config'
import { getAuthenticatedMockUser } from '../auth'
import { mockApiCategories } from '../db'
import { unauthorizedResponse } from '../responses'

export const categoryHandlers = [
  http.get(`${apiBaseUrl}/api/categories`, ({ request }) => {
    if (!getAuthenticatedMockUser(request)) return unauthorizedResponse()
    return HttpResponse.json(mockApiCategories)
  }),
]
