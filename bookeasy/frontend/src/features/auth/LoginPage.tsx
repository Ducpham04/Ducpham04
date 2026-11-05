import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { homeFor, useAuth } from '../../lib/auth'
import { errorMessage } from '../../lib/api'
import { Button, Input } from '../../components/ui'
import { AuthLayout } from './AuthLayout'

const schema = z.object({
  email: z.string().trim().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
})
type Form = z.infer<typeof schema>

const DEMO = [
  { label: 'Chủ cơ sở', email: 'owner@lotusspa.vn' },
  { label: 'Nhân viên', email: 'lan@lotusspa.vn' },
  { label: 'Super admin', email: 'admin@bookeasy.vn' },
]

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from
  const form = useForm<Form>({ resolver: zodResolver(schema), defaultValues: { email: '', password: '' } })
  const m = useMutation({
    mutationFn: (v: Form) => login(v.email, v.password),
    onSuccess: (u) => navigate(from && from !== '/login' ? from : homeFor(u), { replace: true }),
  })
  if (user) return <Navigate to={homeFor(user)} replace />

  return (
    <AuthLayout title="Đăng nhập" subtitle={<>Chưa có tài khoản? <Link to="/register" className="font-medium text-brand underline-offset-2 hover:underline">Đăng ký cơ sở miễn phí</Link></>}>
      <form noValidate onSubmit={form.handleSubmit((v) => m.mutate(v))} className="flex flex-col gap-4">
        <Input label="Email" type="email" autoComplete="email" error={form.formState.errors.email?.message} {...form.register('email')} />
        <Input label="Mật khẩu" type="password" autoComplete="current-password" error={form.formState.errors.password?.message} {...form.register('password')} />
        {m.isError && <p role="alert" className="text-danger">{errorMessage(m.error)}</p>}
        <Button type="submit" loading={m.isPending} size="lg">Đăng nhập</Button>
      </form>
      <div className="mt-8 border-t border-line pt-5">
        <p className="mb-2 text-sm text-ink-2">Tài khoản demo (mật khẩu <span className="font-mono">Demo@123</span>)</p>
        <div className="flex flex-wrap gap-2">
          {DEMO.map((d) => (
            <button
              key={d.email}
              type="button"
              onClick={() => { form.setValue('email', d.email); form.setValue('password', 'Demo@123') }}
              className="rounded-full border border-line-strong px-3 py-1.5 text-sm text-ink-2 hover:border-brand hover:text-ink"
            >
              {d.label}
            </button>
          ))}
        </div>
      </div>
    </AuthLayout>
  )
}
