import tokens from './tokens.json';

export type AppColorScheme = keyof typeof tokens.semantic;
export type ThemePreference = AppColorScheme | 'system';

export const appTokens = tokens;

export function getThemeColors(scheme: AppColorScheme) {
  return tokens.semantic[scheme];
}
