export default [
  {
    files: ["src/**/*.js"],
    rules: {
      "no-unused-vars": "error",
      "no-undef": "error",
    },
    languageOptions: {
      globals: {
        window: "readonly",
        fetch: "readonly",
      },
    },
  },
];
