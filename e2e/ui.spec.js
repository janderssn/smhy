const { test, expect } = require("@playwright/test");
const fs = require("fs");
const path = require("path");

const proof = path.join(__dirname, "proof");
fs.mkdirSync(proof, { recursive: true });

async function assertRealDashboard(page) {
  const dash = await page.evaluate(async () => {
    const response = await fetch("dashboard.json");
    if (!response.ok) throw new Error(String(response.status));
    return response.json();
  });
  expect(dash.schema).toBe(1);
  expect(dash.activeSource).toBe("yr");
  expect(dash.days).toHaveLength(10);
  expect(dash.sites.length).toBeGreaterThan(5);
  expect(dash.defaultFavorites).toEqual(expect.arrayContaining(["areskutan", "alleberg", "kaseberga"]));
  const coming = (dash.sources || []).filter((source) => source.status !== "live").map((source) => source.id);
  expect(coming).toEqual(expect.arrayContaining(["smhi", "open-meteo", "holfuy"]));
  return dash;
}

test("week view at this viewport uses the generated forecast", async ({ page }, testInfo) => {
  await page.goto("/index.html");
  await expect(page.locator("#heroTitle")).toHaveText(/\d+\s+\S+/, { timeout: 20_000 });
  await assertRealDashboard(page);

  const text = await page.locator("body").innerText();
  expect(text).toContain("kommande");
  expect(text).toContain("yr.no");
  expect(text).not.toContain("Sammanvägt");
  expect(text).not.toContain("Svag nordostlig");
  expect(text).toMatch(/PLATSER/);
  await expect(page.locator("#scrub .dchip")).toHaveCount(10);

  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);

  const shot = path.join(proof, `${testInfo.project.name}-overview.png`);
  await page.screenshot({ path: shot });

  if (testInfo.project.name === "desktop-1440") {
    await page.locator("[data-metric=wind]").click();
    await expect(page.locator("#legend")).toContainText("Medelvind");
    await page.screenshot({ path: path.join(proof, "desktop-1440-wind.png") });
    await page.keyboard.press("Control+KeyK");
    await page.locator("#cmdkIn").fill("åre");
    await expect(page.locator("#cmdkList")).toContainText("Åreskutan");
    await page.screenshot({ path: path.join(proof, "desktop-1440-search.png") });
    await page.keyboard.press("Escape");
  }

  await page.locator("#rows .row").first().click();
  if (testInfo.project.name === "mobile-390") {
    await expect(page.locator("#inspector")).toHaveClass(/open/);
  }
  await expect(page.locator("#inspector")).toContainText("kommande");
  await expect(page.locator("#inspector")).not.toContainText("Sammanvägt");
  await page.screenshot({ path: path.join(proof, `${testInfo.project.name}-detail.png`) });
});

test("weekend, favourites and region pages", async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== "desktop-1440", "page variants are checked once at 1440");

  await page.goto("/helg.html");
  await expect(page.locator("#heroTitle")).not.toHaveText("", { timeout: 20_000 });
  await expect(page.locator('[data-view="helg"]')).toHaveAttribute("aria-current", "true");
  const helgDay = await page.locator("#heroDay").innerText();
  expect(helgDay).toMatch(/lördag|söndag/);

  await page.goto("/favoriter.html");
  await expect(page.locator("#mx")).toContainText("Åreskutan");
  await expect(page.locator("#mx")).toContainText("Ålleberg");
  await expect(page.locator("#mx")).toContainText("Kåseberga");

  await page.goto("/stockholm.html");
  await expect(page.locator("#mx")).toContainText("Granholmstoppen");
  await expect(page.locator("#mx")).not.toContainText("Ålleberg");
});
