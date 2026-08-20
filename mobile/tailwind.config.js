const tokens = require('./src/theme/tokens.json');

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  darkMode: 'class',
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      borderRadius: {
        'app-sm': `${tokens.radius.sm}px`,
        'app-md': `${tokens.radius.md}px`,
        'app-lg': `${tokens.radius.lg}px`,
        'app-xl': `${tokens.radius.xl}px`,
        'app-pill': `${tokens.radius.pill}px`,
      },
      colors: {
        brand: tokens.brand,
        app: {
          canvas: tokens.semantic.light.canvas,
          'canvas-dark': tokens.semantic.dark.canvas,
          surface: tokens.semantic.light.surface,
          'surface-dark': tokens.semantic.dark.surface,
          'surface-muted': tokens.semantic.light.surfaceMuted,
          'surface-muted-dark': tokens.semantic.dark.surfaceMuted,
          'surface-strong': tokens.semantic.light.surfaceStrong,
          'surface-strong-dark': tokens.semantic.dark.surfaceStrong,
          border: tokens.semantic.light.border,
          'border-dark': tokens.semantic.dark.border,
          text: tokens.semantic.light.text,
          'text-dark': tokens.semantic.dark.text,
          'text-strong': tokens.semantic.light.textStrong,
          'text-strong-dark': tokens.semantic.dark.textStrong,
          'text-muted': tokens.semantic.light.textMuted,
          'text-muted-dark': tokens.semantic.dark.textMuted,
        },
      },
      fontFamily: {
        inter: ['Inter_400Regular'],
        'inter-medium': ['Inter_500Medium'],
        'inter-semibold': ['Inter_600SemiBold'],
        'inter-bold': ['Inter_700Bold'],
      },
    },
  },
  plugins: [],
};
