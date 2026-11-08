const path = require('node:path');
const fs = require('node:fs/promises');
const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');
const { chromium } = require(path.resolve(__dirname, '../.local/browser/node_modules/playwright'));
const base = process.env.LUMA_BASE_URL || 'http://127.0.0.1:8082';

(async () => {
  const root = path.resolve(__dirname, '..');
  const output = path.join(root, '.local/qa');
  await fs.mkdir(output, { recursive: true });
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1050 } });
  const page = await context.newPage();
  const email = `browser-${randomUUID()}@example.test`;
  const password = 'BrowserDemo!2026';
  const conflict = process.argv.includes('--payos-conflict');
  const mock = process.argv.includes('--payos-mock') || conflict;
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  let code;
  try {
    await page.goto(base + '/san-pham/iphone-13?sku=1');
    await page.getByRole('button', { name: 'Thêm vào giỏ hàng', exact: true }).click();
    await page.waitForURL('**/gio-hang');
    assert.match(await page.locator('main').innerText(), /iPhone 13/);
    await page.goto(base + '/dang-ky');
    await page.getByLabel('Họ', { exact: true }).fill('Khách');
    await page.getByLabel('Tên', { exact: true }).fill('Kiểm thử');
    await page.getByLabel('Ngày sinh', { exact: true }).fill('2000-01-01');
    await page.getByLabel('Email', { exact: true }).fill(email);
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
    await page.getByLabel('Nhập lại mật khẩu', { exact: true }).fill(password);
    await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
    await page.waitForURL('**/login?registered');
    await page.goto(base + '/login');
    await page.getByLabel('Email', { exact: true }).fill(email);
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await page.waitForURL('**/tai-khoan');
    await page.goto(base + '/gio-hang');
    assert.match(await page.locator('main').innerText(), /iPhone 13/);
    await page.goto(base + '/tai-khoan/dia-chi');
    await page.getByRole('button', { name: 'Thêm địa chỉ', exact: true }).click();
    await fillAddress(page);
    await page.getByLabel('Đặt làm địa chỉ mặc định', { exact: true }).check();
    await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click();
    assert.match(await page.locator('main').innerText(), /Đã lưu địa chỉ/);
    await page.goto(base + '/dat-hang');
    await page.locator('[data-saved-address]').first().click();
    assert.equal(await page.getByLabel('Số nhà, đường, căn hộ', { exact: true }).inputValue(), '123 Đường thử nghiệm');
    await page.getByRole('button', { name: 'Chọn điểm trên Google Maps', exact: true }).click();
    assert.match(await page.locator('[data-map-status]').innerText(), /chưa sẵn sàng/);
    await page.getByLabel('Mã giảm giá', { exact: true }).fill('LUMA100');
    if (mock) await page.getByLabel('PayOS mô phỏng · không chuyển tiền', { exact: true }).check();
    await page.getByRole('button', { name: 'Kiểm tra ưu đãi', exact: true }).click();
    assert.match(await page.locator('main').innerText(), /Tổng sau ưu đãi vừa kiểm tra/);
    await noOverflow(page, 'checkout desktop');
    await page.screenshot({ path: path.join(output, 'checkout-desktop.png'), fullPage: true });
    await page.setViewportSize({ width: 390, height: 1000 });
    await noOverflow(page, 'checkout mobile');
    await page.screenshot({ path: path.join(output, 'checkout-mobile.png'), fullPage: true });
    const submitted = await page.locator('form.checkout-grid').evaluate(form => Object.fromEntries(new FormData(form)));
    await page.getByRole('button', { name: 'Đặt hàng', exact: true }).click();
    await page.waitForURL(mock ? '**/thanh-toan/LM-*' : '**/don-hang/LM-*');
    code = new URL(page.url()).pathname.split('/').pop();
    if (mock) {
      assert.match(await page.locator('main').innerText(), /Chế độ mô phỏng/);
      const due = BigInt(await page.getByLabel('Số tiền mô phỏng (VND)', { exact: true }).inputValue());
      const originalNonce=await page.locator('input[name="nonce"]').inputValue();
      await page.getByLabel('Số tiền mô phỏng (VND)', { exact: true }).fill('1000');
      await page.getByRole('button', { name: 'Ghi khoản thu mô phỏng', exact: true }).click();
      assert.match(await page.locator('main').innerText(), /Đã thu một phần/);
      if(conflict){
        const csrf=await page.locator('input[name="_csrf"]').first().inputValue();
        assert.equal((await page.request.post(`${base}/thanh-toan/${code}/mo-phong`,{form:{_csrf:csrf,nonce:originalNonce,soTien:'1001'},maxRedirects:0})).status(),302);
        await page.reload();assert.match(await page.locator('main').innerText(),/Đang đối soát/);
        await checkReconciliation(browser,code,output,errors);
      }else{
        await page.getByLabel('Số tiền mô phỏng (VND)', { exact: true }).fill(String(due - 1000n));
        await page.getByRole('button', { name: 'Ghi khoản thu mô phỏng', exact: true }).click();
        assert.match(await page.locator('main').innerText(), /Đã xác minh đủ tiền/);
      }
      await page.screenshot({ path: path.join(output, 'payos-mock-mobile.png'), fullPage: true });
      await page.getByRole('link', { name: 'Về chi tiết đơn hàng', exact: true }).click();
    }
    const text = await page.locator('main').innerText();
    assert.match(text, conflict ? /Đang đối soát/ : mock ? /Đã thu tiền/ : /Chờ thu tiền/); assert.match(text, /100\.000/);
    await noOverflow(page, 'order mobile');
    await page.screenshot({ path: path.join(output, mock ? 'payos-order-mobile.png' : 'cod-order-mobile.png'), fullPage: true });
    const replay = await page.request.post(base + '/dat-hang', { form: submitted, maxRedirects: 0 });
    assert.equal(replay.status(), 302); assert.equal(new URL(replay.headers().location, page.url()).pathname, (mock ? '/thanh-toan/' : '/don-hang/') + code);
    await page.getByRole('button', { name: 'Hủy trước khi bàn giao', exact: true }).click();
    assert.match(await page.locator('main').innerText(), /Đã hủy/);
    await page.goto(base + '/don-hang');
    assert.match(await page.locator('main').innerText(), /Đã hủy/);
    await page.goto(base + '/tai-khoan/thong-bao');assert.ok(await page.locator('[data-notification-id]').count()>=2);assert.ok((await page.locator('main').innerText()).includes(code));if(mock)assert.match(await page.locator('main').innerText(),/Mô phỏng ·/);
    assert.deepEqual(errors, []);
    console.log((conflict ? 'PayOS conflict/reconciliation browser QA: ' : mock ? 'PayOS mock browser QA: ' : '') + 'Browser checkout QA passed: guest cart -> email registration -> login/merge -> saved address -> Maps unavailable fallback -> COD/voucher -> idempotent replay -> cancel/history; desktop/mobile layouts.');
  } finally {
    // Clean up any reservation even when a later rendering assertion fails.
    if (code) {
      try {
        await page.goto(base + '/tai-khoan');
        const csrf = await page.locator('input[name="_csrf"]').first().inputValue();
        await page.request.post(`${base}/don-hang/${code}/huy`, { form: { _csrf: csrf }, maxRedirects: 0 });
      } catch {}
    }
    try { await page.goto(base + '/tai-khoan'); await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); } catch {}
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });

async function fillAddress(page) {
  await page.getByLabel('Người nhận', { exact: true }).fill('Khách kiểm thử trình duyệt');
  await page.getByLabel('Điện thoại nhận hàng', { exact: true }).fill('0901234567');
  await page.waitForFunction(() => document.querySelector('#address-city').options.length > 1);
  await page.getByLabel('Tỉnh / Thành phố', { exact: true }).selectOption({label:'Hồ Chí Minh'});
  await page.getByLabel('Phường / Xã', { exact: true }).selectOption({index:1});
  await page.getByLabel('Số nhà, đường, căn hộ', { exact: true }).fill('123 Đường thử nghiệm');
}
async function noOverflow(page, label) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  assert.ok(overflow <= 1, label + ' overflows by ' + overflow);
}
async function checkReconciliation(browser,code,output,errors){
  const admin=await browser.newPage({viewport:{width:1440,height:1050}});admin.on('pageerror',e=>errors.push(e.message));
  try{
    await admin.goto(base + '/login');await admin.getByLabel('Email',{exact:true}).fill('admin@luma.local');await admin.getByLabel('Mật khẩu',{exact:true}).fill('LumaDemo!2026');await admin.getByRole('button',{name:'Đăng nhập',exact:true}).click();await admin.waitForURL('**/tai-khoan');
    assert.equal((await admin.goto(base + '/quan-tri/doi-soat?q='+code)).status(),200);
    assert.match(await admin.locator('main').innerText(),/PAYOS · Mô phỏng/);await admin.getByRole('link',{name:'Xem đối soát',exact:true}).click();assert.match(await admin.locator('main').innerText(),/THAM_CHIEU_XUNG_DOT/);
    assert.match(await admin.locator('main').innerText(),/Mã khoản thu: [0-9]+/);assert.doesNotMatch(await admin.locator('main').innerText(),/e\.MaGiaoDich/);
    const receipts=admin.locator('section').filter({has:admin.getByRole('heading',{name:'Khoản thu đã ghi sổ',exact:true})});assert.equal(await receipts.locator('tbody tr').count(),1);
    assert.match(await receipts.innerText(),/1\.000/);await noOverflow(admin,'reconciliation desktop');await admin.screenshot({path:path.join(output,'reconciliation-conflict-desktop.png'),fullPage:true});
    await admin.getByRole('button',{name:'Kiểm tra trạng thái PayOS',exact:true}).click();assert.match(await admin.locator('main').innerText(),/THAM_CHIEU_XUNG_DOT/);assert.equal(await receipts.locator('tbody tr').count(),1);
    await admin.setViewportSize({width:360,height:1000});await noOverflow(admin,'reconciliation mobile');await admin.screenshot({path:path.join(output,'reconciliation-conflict-mobile.png'),fullPage:true});
    await admin.goto(base + '/quan-tri/doi-soat/su-kien-chua-gan');assert.match(await admin.locator('main').innerText(),/Sự kiện chưa gắn phiên/);await noOverflow(admin,'unmatched inbox mobile');
  }finally{await admin.close();}
}
