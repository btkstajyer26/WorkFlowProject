const avatarColorClasses = [
  'bg-rose-700 text-white',
  'bg-amber-400 text-amber-950',
  'bg-emerald-700 text-white',
  'bg-cyan-700 text-white',
  'bg-blue-600 text-white',
  'bg-indigo-600 text-white',
  'bg-violet-600 text-white',
  'bg-fuchsia-700 text-white',
] as const

export function getUserAvatarColorClass(userId: string) {
  let hash = 0
  for (const character of userId) hash = (hash * 31 + character.charCodeAt(0)) >>> 0
  return avatarColorClasses[hash % avatarColorClasses.length]
}
