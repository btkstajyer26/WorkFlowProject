import AsyncStorage from '@react-native-async-storage/async-storage';
import { useColorScheme as useNativeWindColorScheme } from 'nativewind';
import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { Appearance, type ColorSchemeName } from 'react-native';

import {
  getThemeColors,
  type AppColorScheme,
  type ThemePreference,
} from './theme';

const THEME_STORAGE_KEY = '@ebys/theme-preference';

type ThemeContextValue = {
  colors: ReturnType<typeof getThemeColors>;
  isReady: boolean;
  preference: ThemePreference;
  resolvedTheme: AppColorScheme;
  setPreference: (preference: ThemePreference) => Promise<void>;
  toggleTheme: () => Promise<void>;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

function isThemePreference(value: string | null): value is ThemePreference {
  return value === 'light' || value === 'dark' || value === 'system';
}

function resolveColorScheme(
  selectedScheme: ColorSchemeName | null | undefined,
  systemScheme: ColorSchemeName | null | undefined,
): AppColorScheme {
  if (selectedScheme === 'light' || selectedScheme === 'dark') {
    return selectedScheme;
  }

  return systemScheme === 'dark' ? 'dark' : 'light';
}

function resolvePreference(preference: ThemePreference): AppColorScheme {
  return preference === 'system'
    ? resolveColorScheme(undefined, Appearance.getColorScheme())
    : preference;
}

export function ThemeProvider({ children }: PropsWithChildren) {
  const { colorScheme, setColorScheme } = useNativeWindColorScheme();
  const [preference, setStoredPreference] = useState<ThemePreference>('system');
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function restorePreference() {
      try {
        const storedPreference = await AsyncStorage.getItem(THEME_STORAGE_KEY);

        if (!isMounted) return;

        const nextPreference = isThemePreference(storedPreference)
          ? storedPreference
          : 'system';
        setStoredPreference(nextPreference);
        setColorScheme(resolvePreference(nextPreference));
      } catch {
        if (isMounted) setColorScheme(resolvePreference('system'));
      } finally {
        if (isMounted) setIsReady(true);
      }
    }

    void restorePreference();

    return () => {
      isMounted = false;
    };
  }, [setColorScheme]);

  useEffect(() => {
    if (preference !== 'system') return;

    const subscription = Appearance.addChangeListener(({ colorScheme }) => {
      setColorScheme(colorScheme === 'dark' ? 'dark' : 'light');
    });

    return () => subscription.remove();
  }, [preference, setColorScheme]);

  const systemColorScheme = Appearance.getColorScheme();
  const resolvedTheme = resolveColorScheme(colorScheme, systemColorScheme);

  const setPreference = useCallback(
    async (nextPreference: ThemePreference) => {
      setStoredPreference(nextPreference);
      setColorScheme(resolvePreference(nextPreference));

      try {
        await AsyncStorage.setItem(THEME_STORAGE_KEY, nextPreference);
      } catch {
        // Tema yine mevcut oturumda uygulanır; depolama hatası uygulamayı durdurmaz.
      }
    },
    [setColorScheme],
  );

  const toggleTheme = useCallback(async () => {
    await setPreference(resolvedTheme === 'dark' ? 'light' : 'dark');
  }, [resolvedTheme, setPreference]);

  const value = useMemo<ThemeContextValue>(
    () => ({
      colors: getThemeColors(resolvedTheme),
      isReady,
      preference,
      resolvedTheme,
      setPreference,
      toggleTheme,
    }),
    [isReady, preference, resolvedTheme, setPreference, toggleTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useAppTheme() {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error('useAppTheme, ThemeProvider içinde kullanılmalıdır.');
  }

  return context;
}
