/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  // Tắt preflight để không xung đột với CSS của zmp-ui
  corePlugins: { preflight: false },
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: "var(--color-primary)", dark: "var(--color-primary-dark)", soft: "var(--color-primary-soft)" },
        success: "var(--color-success)",
        warning: "var(--color-warning)",
        danger: "var(--color-danger)",
        ink: { DEFAULT: "var(--color-text)", muted: "var(--color-text-muted)" },
        line: "var(--color-border)",
        page: "var(--color-bg)",
      },
      borderRadius: { card: "12px" },
      boxShadow: { card: "0 1px 2px rgba(16,24,40,.06)", sheet: "0 8px 24px rgba(16,24,40,.12)" },
    },
  },
  plugins: [],
};
