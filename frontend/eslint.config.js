import js from "@eslint/js";
import globals from "globals";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";

export default [
  // Bỏ qua các thư mục và file không cần lint
  {
    ignores: [
      "**/node_modules/**",
      "**/dist/**",
      "**/dist_old_locked/**", // thư mục build cũ đã khóa (dev-only)
      "**/build/**",
      "**/coverage/**",
      "**/*.min.js",
      "**/vite.config.js", // nếu muốn bỏ qua file config
    ],
  },
  // Cấu hình cho JS/JSX
  js.configs.recommended,
  {
    files: ["**/*.{js,jsx}"],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        ecmaFeatures: { jsx: true },
        sourceType: "module",
      },
    },
    plugins: {
      react,
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      ...react.configs.recommended.rules,
      ...react.configs["jsx-runtime"].rules,
      ...reactHooks.configs.recommended.rules,
      "react-refresh/only-export-components": "warn",
      "react/prop-types": "off",
    },
    settings: {
      react: { version: "detect" },
    },
  },
];
