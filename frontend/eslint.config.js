import js from '@eslint/js';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import configPrettier from 'eslint-config-prettier';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist', 'dev-dist', 'coverage'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      // La variante `flat` es la que entiende la configuracion plana de ESLint.
      reactHooks.configs.flat['recommended-latest'],
      reactRefresh.configs.vite,
      // Debe ir al final: apaga las reglas de estilo que ya resuelve Prettier.
      configPrettier,
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
    },
    rules: {
      // `any` desactiva el sistema de tipos. Para un dato externo sin forma
      // conocida se usa `unknown` y se valida antes de usarlo.
      '@typescript-eslint/no-explicit-any': 'error',
      // Una variable sin uso suele ser codigo muerto; el prefijo `_` marca la
      // excepcion deliberada (por ejemplo, un parametro que exige una firma).
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
    },
  }
);
