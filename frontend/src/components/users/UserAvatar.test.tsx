import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { getDemoUserByRole } from '../../mocks/users'
import { UserAvatar } from './UserAvatar'
import { getUserAvatarColorClass } from './userAvatarColor'

describe('UserAvatar', () => {
  it('aynı kullanıcı için aynı, farklı kullanıcılar için kimliğe bağlı renk üretir', () => {
    const employee = getDemoUserByRole('CALISAN')
    const deputy = getDemoUserByRole('BASKAN_YARDIMCISI')

    expect(getUserAvatarColorClass(employee.id)).toBe(getUserAvatarColorClass(employee.id))
    expect(getUserAvatarColorClass(employee.id)).not.toBe(getUserAvatarColorClass(deputy.id))

    render(<UserAvatar user={employee} className="size-10 rounded-full" />)
    expect(screen.getByText('JD')).toHaveClass(...getUserAvatarColorClass(employee.id).split(' '))
  })
})
