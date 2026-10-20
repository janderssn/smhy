const { defineConfig } = require("@playwright/test");

module.exports = defineConfig({
  testDir: ".",
  testMatch: /ui\.spec\.js/,
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"]],
  use: {
    baseURL: "http://127.0.0.1:8080",
    trace: "retain-on-failure",
  },
  webServer: {
    command: "python3 -m http.server 8080 --bind 127.0.0.1 --directory /tmp/smhy",
    url: "http://127.0.0.1:8080/index.html",
    reuseExistingServer: !process.env.CI,
    timeout: 20_000,
  },
  projects: [
    { name: "desktop-1440", use: { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 } },
    {
      name: "mobile-390",
      use: { viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true },
    },
  ],
});
