import { useQuery } from '@tanstack/react-query'
import { api } from '../../lib/api'
import type { Branch, Business, Category, Service, Staff } from '../../lib/types'

export const useBusiness = () => useQuery({ queryKey: ['business'], queryFn: async () => (await api.get<Business>('/business')).data })
export const useBranches = () => useQuery({ queryKey: ['branches'], queryFn: async () => (await api.get<Branch[]>('/branches')).data })
export const useStaffList = () => useQuery({ queryKey: ['staff'], queryFn: async () => (await api.get<Staff[]>('/staff')).data })
export const useServices = () => useQuery({ queryKey: ['services'], queryFn: async () => (await api.get<Service[]>('/services')).data })
export const useCategories = () => useQuery({ queryKey: ['categories'], queryFn: async () => (await api.get<Category[]>('/service-categories')).data })
