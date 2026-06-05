/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,vue}'],
  theme: {
    extend: {
      colors: {
        brand: '#1A2A4F',
        background: '#FFF2EF',
        rose: '#F7A5A5',
        apricot: '#FFDBB6',
        cream: '#FFF2EF',
        ink: '#1A2A4F',
      },
      boxShadow: {
        soft: '0 24px 80px rgba(26, 42, 79, 0.12)',
      },
    },
  },
  plugins: [],
};
