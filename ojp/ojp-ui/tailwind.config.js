/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                brand: {
                    primary: '#1677ff',
                    accent: '#5b8def',
                },
            },
        },
    },
    corePlugins: {
        preflight: true,
    },
    plugins: [],
}
