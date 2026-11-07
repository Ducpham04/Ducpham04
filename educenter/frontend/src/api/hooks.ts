import { useQuery } from "@tanstack/react-query";
import { api } from "./client";
import type { Course, Room, TeacherOption, FeePlan, Tenant } from "./types";

export const useTeachers = () => useQuery({ queryKey: ["teachers"], queryFn: () => api<TeacherOption[]>("/users/teachers"), staleTime: 300_000 });
export const useRooms = () => useQuery({ queryKey: ["rooms"], queryFn: () => api<Room[]>("/rooms"), staleTime: 300_000 });
export const useCourses = () => useQuery({ queryKey: ["courses"], queryFn: () => api<Course[]>("/courses"), staleTime: 300_000 });
export const useFeePlans = (enabled = true) => useQuery({ queryKey: ["fee-plans"], queryFn: () => api<FeePlan[]>("/fee-plans"), enabled, staleTime: 120_000 });
export const useCenter = () => useQuery({ queryKey: ["center"], queryFn: () => api<Tenant>("/center"), staleTime: 300_000 });
export const useBanks = () => useQuery({ queryKey: ["banks"], queryFn: () => api<Record<string, string>>("/banks"), staleTime: Infinity });
