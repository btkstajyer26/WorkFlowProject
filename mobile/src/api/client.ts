const BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'https://api.workflowproject.com';

export const apiClient = {
  get: async <T>(endpoint: string, options?: RequestInit): Promise<T> => {
    const res = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      method: 'GET',
      headers: { Accept: 'application/json', ...options?.headers },
    });
    if (!res.ok) throw new Error(`GET ${endpoint} başarısız: ${res.status}`);
    return res.json();
  },
  post: async <T>(endpoint: string, data?: any, options?: RequestInit): Promise<T> => {
    const isFormData = data instanceof FormData;
    const res = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      method: 'POST',
      body: isFormData ? data : JSON.stringify(data),
      headers: {
        ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
        ...options?.headers,
      },
    });
    if (!res.ok) throw new Error(`POST ${endpoint} başarısız: ${res.status}`);
    return res.json();
  },
  delete: async <T>(endpoint: string, options?: RequestInit): Promise<T> => {
    const res = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      method: 'DELETE',
      headers: { ...options?.headers },
    });
    if (!res.ok) throw new Error(`DELETE ${endpoint} başarısız: ${res.status}`);
    return res.json();
  },
};