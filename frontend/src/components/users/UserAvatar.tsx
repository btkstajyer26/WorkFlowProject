import type { AuthUser } from '../../types/auth'
import { getUserAvatarColorClass } from './userAvatarColor'

type AvatarUser = Pick<AuthUser, 'id' | 'firstName' | 'lastName'>

export function UserAvatar({ user, className = '' }: { user: AvatarUser; className?: string }) {
  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toLocaleUpperCase('tr-TR')

  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center font-extrabold ${getUserAvatarColorClass(user.id)} ${className}`}
      aria-hidden="true"
    >
      {initials}
    </span>
  )
}
