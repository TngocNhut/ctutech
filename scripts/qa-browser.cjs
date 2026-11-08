// Local-only browser QA; uses the isolated Playwright install and Microsoft Edge.
const path = require('node:path');
const fs = require('node:fs/promises');
const assert = require('node:assert/strict');
const { chromium } = require(path.resolve(__dirname, '../.local/browser/node_modules/playwright'));

(async () => {
  const output = path.resolve(__dirname, '../.local/qa');
  await fs.mkdir(output, { recursive: true });
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const failures = [];
  try {
    for (const width of [1440, 390, 360]) {
      const context = await browser.newContext({ viewport: { width, height: 1000 } });
      const page = await context.newPage();
      page.on('pageerror', err => failures.push(err.message));
      for (const [name, route] of [['home', '/'], ['detail', '/san-pham/iphone-13?sku=1'], ['login', '/login'], ['register', '/dang-ky']]) {
        const response = await page.goto('http://127.0.0.1:8082' + route);
        assert.equal(response.status(), 200, route);
        await assertNoOverflow(page, `${name}-${width}`);
        await page.screenshot({ path: path.join(output, `${name}-${width}.png`), fullPage: true });
      }
      await context.close();
    }
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const page = await context.newPage();
    await page.goto('http://127.0.0.1:8082/login');
    await page.getByLabel('Email', { exact: true }).fill('admin@luma.local');
    await page.getByLabel('Mật khẩu', { exact: true }).fill(process.env.QA_DEMO_PASSWORD || 'LumaDemo!2026');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await page.waitForURL('**/tai-khoan');
    for (const [name, route] of [['profile', '/tai-khoan'], ['admin', '/quan-tri/san-pham'], ['sku-edit', '/quan-tri/bien-the/1']]) {
      assert.equal((await page.goto('http://127.0.0.1:8082' + route)).status(), 200);
      await assertNoOverflow(page, name);
      await page.screenshot({ path: path.join(output, name + '.png'), fullPage: true });
    }
    await page.setViewportSize({ width: 360, height: 900 });
    await page.goto('http://127.0.0.1:8082/quan-tri/san-pham');
    await assertNoOverflow(page, 'admin-mobile');
    await page.screenshot({ path: path.join(output, 'admin-mobile.png'), fullPage: true });
    // Logout so browser QA does not leave an active demo account session behind.
    await page.goto('http://127.0.0.1:8082/tai-khoan');
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
    assert.deepEqual(failures, []);
    await context.close();
    console.log('Browser QA passed: store/detail/auth at 1440/390/360px; admin login/profile/SKU editor, mobile table and logout. Screenshots: .local/qa');
  } finally { await browser.close(); }
})().catch(err => { console.error(err); process.exitCode = 1; });

async function assertNoOverflow(page, label) {
  const size = await page.evaluate(() => ({ scroll: document.documentElement.scrollWidth, client: document.documentElement.clientWidth }));
  assert.ok(size.scroll <= size.client + 1, `${label} horizontal overflow: ${JSON.stringify(size)}`);
}
