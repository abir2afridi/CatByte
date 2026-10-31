export default [
  { ignores: ['dist/', 'build/', 'src-tauri/target/', '*.config.*'] },
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: { jsx: true },
        project: './tsconfig.json',
      },
    },
    settings: {
      react: { version: '18.3' },
    },
    plugins: {
      '@typescript-eslint': await import('@typescript-eslint/eslint-plugin'),
      'react-hooks': await import('eslint-plugin-react-hooks'),
      'react-refresh': await import('eslint-plugin-react-refresh'),
    },
    rules: {
      ...await import('@typescript-eslint/eslint-plugin').then((p) => p.configs.recommended.rules),
      ...await import('eslint-plugin-react-hooks').then((p) => p.configs.recommended.rules),
      'react-refresh/only-export-components': 'warn',
      'react-refresh/export-hero': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    },
  },
]