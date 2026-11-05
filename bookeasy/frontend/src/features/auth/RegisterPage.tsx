import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { api, errorBody, errorMessage } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { slugify } from '../../lib/format'
import { CATEGORY_LABEL } from '../../lib/status'
import type { TokenResponse } from '../../lib/types'
import { Button, Input, Select } from '../../components/ui'
import { AuthLayout } from './AuthLayout'

const schema = z.object({
  businessName: z.string().trim().min(2, 'Vui lòng nhập tên cơ sở').max(150),
  slug: z.string().regex(/^[a-z0-9](?:[a-z0-9-]{1,58}[a-z0-9])$/, 'Chỉ chữ thường không dấu, số, dấu gạch ngang (3–60 ký tự)'),
  category: z.enum(['SPA', 'SALON', 'GYM', 'CLINIC', 'OTHER']),
  fullName: z.string().trim().min(2, 'Vui lòng nhập họ tên').max(100),
  email: z.string().trim().email('Email không hợp lệ'),
  phone: z.string().max(20).optional(),
  password: z.string().regex(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/, 'Tối thiểu 8 ký tự, gồm chữ và số'),
})
type Form = z.infer<typeof schema>

export default function RegisterPage() {
  const { acceptTokens } = useAuth()
  const navigate = useNavigate()
  const [slugTouched, setSlugTouched] = useState(false)
  const form = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: { businessName: '', slug: '', category: 'SPA', fullName: '', email: '', phone: '', password: '' },
  })
  const m = useMutation({
    mutationFn: async (v: Form) => (await api.post<TokenResponse>('/auth/register', v)).data,
    onSuccess: (t) => {
      acceptTokens(t)
      navigate('/app/settings?welcome=1', { replace: true })
    },
    onError: (e) => {
      const b = errorBody(e)
      if (b?.code === 'SLUG_TAKEN') form.setError('slug', { message: b.message })
      if (b?.code === 'EMAIL_TAKEN') form.setError('email', { message: b.message })
      b?.fieldErrors && Object.entries(b.fieldErrors).forEach(([k, msg]) => form.setError(k as keyof Form, { message: msg }))
    },
  })
  const { errors } = form.formState
  const slug = form.watch('slug')

  return (
    <AuthLayout title="Đăng ký cơ sở" subtitle={<>Đã có tài khoản? <Link to="/login" className="font-medium text-brand underline-offset-2 hover:underline">Đăng nhập</Link></>}>
      <form noValidate onSubmit={form.handleSubmit((v) => m.mutate(v))} className="flex flex-col gap-4">
        <Input
          label="Tên cơ sở"
          placeholder="Ví dụ: Hoa Sen Spa"
          error={errors.businessName?.message}
          {...form.register('businessName', {
            onChange: (e) => !slugTouched && form.setValue('slug', slugify(e.target.value)),
          })}
        />
        <Input
          label="Đường dẫn trang đặt lịch"
          hint={<>Khách đặt lịch tại <span className="font-mono">/b/{slug || 'ten-co-so'}</span></>}
          error={errors.slug?.message}
          {...form.register('slug', { onChange: () => setSlugTouched(true) })}
        />
        <Select label="Loại hình" {...form.register('category')}>
          {Object.entries(CATEGORY_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </Select>
        <Input label="Họ tên chủ cơ sở" autoComplete="name" error={errors.fullName?.message} {...form.register('fullName')} />
        <Input label="Email đăng nhập" type="email" autoComplete="email" error={errors.email?.message} {...form.register('email')} />
        <Input label="Số điện thoại (không bắt buộc)" type="tel" autoComplete="tel" {...form.register('phone')} />
        <Input label="Mật khẩu" type="password" autoComplete="new-password" error={errors.password?.message} {...form.register('password')} />
        {m.isError && !errorBody(m.error)?.fieldErrors && !['SLUG_TAKEN', 'EMAIL_TAKEN'].includes(errorBody(m.error)?.code ?? '') && (
          <p role="alert" className="text-danger">{errorMessage(m.error)}</p>
        )}
        <Button type="submit" loading={m.isPending} size="lg">Tạo cơ sở</Button>
      </form>
    </AuthLayout>
  )
}
