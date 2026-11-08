const path = require('node:path');
const fs = require('node:fs/promises');
const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');
const { chromium } = require(path.resolve(__dirname, '../.local/browser/node_modules/playwright'));
const base = 'http://127.0.0.1:8082';
const output = path.resolve(__dirname, '../.local/qa');

(async () => {
  await fs.mkdir(output, { recursive: true });
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const customer = await browser.newContext({ viewport: { width: 390, height: 1000 } });
  const admin = await browser.newContext({ viewport: { width: 1440, height: 1050 } });
  const page = await customer.newPage(), staff = await admin.newPage();
  const errors = [];for (const p of [page, staff]) p.on('pageerror', e => errors.push(e.message));
  let code, dispatched = false;
  try {
    // A dedicated browser account keeps this fixture separate from the user's demo cart.
    const email = `delivery-${randomUUID()}@example.test`, password = 'BrowserDemo!2026';
    await page.goto(base + '/dang-ky');
    for (const [label, value] of [['Họ tên','Nguyễn Thị Ánh'],['Email',email],['Mật khẩu',password],['Nhập lại mật khẩu',password]]) await page.getByLabel(label, { exact: true }).fill(value);
    await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();await page.waitForURL('**/login?registered');
    const mailDir = path.resolve(__dirname, '../.local/mail');let link;
    for (const file of await fs.readdir(mailDir)) {
      const text = await fs.readFile(path.join(mailDir, file), 'utf8');
      if (text.includes('Đến: ' + email + '\n')) link = text.match(/http:\/\/localhost:8082\/xac-minh-email\?token=[A-Za-z0-9_-]+/)?.[0];
    }
    assert.ok(link);await page.goto(link.replace('localhost','127.0.0.1'));await page.getByRole('button', { name:'Xác nhận email',exact:true }).click();
    await login(page, email, password);await login(staff, 'admin@luma.local', 'LumaDemo!2026');
    await page.goto(base + '/san-pham/iphone-13?sku=1');await page.getByRole('button', { name:'Thêm vào giỏ hàng',exact:true }).click();await page.waitForURL('**/gio-hang');
    await page.goto(base + '/dat-hang');
    for (const [label,value] of [['Người nhận','Nguyễn Thị Ánh'],['Điện thoại nhận hàng','0901234567'],['Tỉnh / Thành phố','Thành phố Hồ Chí Minh'],['Phường / Xã','Phường Bến Nghé'],['Số nhà, đường, căn hộ','123 Nguyễn Huệ']]) await page.getByLabel(label,{exact:true}).fill(value);
    await page.getByRole('button',{name:'Đặt hàng',exact:true}).click();await page.waitForURL('**/don-hang/LM-*');code = new URL(page.url()).pathname.split('/').pop();
    await fs.writeFile(path.join(output, 'fulfillment-order.json'), JSON.stringify({code,email,dispatched:false},null,2));
    await staff.goto(base + '/quan-tri/don-hang');assert.match(await staff.locator('main').innerText(), new RegExp(code));
    await staff.getByRole('link',{name:code,exact:true}).click();
    await staff.getByLabel('Đã kiểm tra địa chỉ giao nhận và điều kiện của đơn',{exact:true}).check();
    await staff.getByRole('button',{name:'Xác nhận đơn hàng',exact:true}).click();assert.match(await staff.locator('main').innerText(), /Đã xác nhận đơn/);
    await staff.getByLabel('Cước gốc thực tế (để trống khi chưa biết)',{exact:true}).fill('26000');
    await staff.getByRole('button',{name:'Tạo vận đơn',exact:true}).click();assert.match(await staff.locator('main').innerText(), /Đã tạo vận đơn/);
    await overflow(staff);await staff.screenshot({path:path.join(output,'admin-shipping-desktop.png'),fullPage:true});
    await staff.setViewportSize({width:360,height:1000});await overflow(staff);await staff.screenshot({path:path.join(output,'admin-shipping-mobile.png'),fullPage:true});
    await update(staff,'BAN_GIAO','Đã bàn giao kiện cho đội giao nội bộ');dispatched = true;
    await fs.writeFile(path.join(output, 'fulfillment-order.json'), JSON.stringify({code,email,dispatched:true},null,2));
    await update(staff,'GIAO_THAT_BAI','Khách hẹn giao lại trong ngày');
    await staff.goto(base + '/quan-tri/giao-nhan');assert.match(await staff.locator('main').innerText(), /Khách hẹn giao lại/);
    const task = staff.locator('article').filter({has:staff.getByRole('link',{name:'LV-'+code,exact:true})});
    await task.getByRole('button',{name:'Nhận việc',exact:true}).click();assert.match(await staff.locator('main').innerText(), /Đã nhận việc/);
    await staff.goto(base + '/quan-tri/don-hang/' + code);await update(staff,'GIAO_LAI','Đội giao nhận đã liên hệ lại khách');await update(staff,'DA_GIAO','Khách đã nhận đủ một kiện');
    assert.match(await staff.locator('main').innerText(), /Chờ thu tiền/);
    await page.reload();assert.match(await page.locator('main').innerText(), /Hàng đã giao.*kiểm tra khoản thu COD/);assert.equal(await page.getByRole('link',{name:'Tải hóa đơn PDF',exact:true}).count(),0);
    await staff.getByLabel('Mã chứng từ thu COD',{exact:true}).fill('QA-COD-'+code);await staff.getByLabel('Đã kiểm tra chứng từ và số tiền COD',{exact:true}).check();
    await staff.getByRole('button',{name:'Ghi thu COD & hoàn tất đơn',exact:true}).click();assert.match(await staff.locator('main').innerText(), /Đã ghi thu COD mô phỏng/);
    await page.reload();assert.match(await page.locator('main').innerText(), /Hoàn thành/);await overflow(page);await page.screenshot({path:path.join(output,'delivered-order-mobile.png'),fullPage:true});
    const [download] = await Promise.all([page.waitForEvent('download'),page.getByRole('link',{name:'Tải hóa đơn PDF',exact:true}).click()]);
    await download.saveAs(path.join(output,'invoice-browser.pdf'));const bytes = await fs.readFile(path.join(output,'invoice-browser.pdf'));assert.equal(bytes.subarray(0,5).toString(),'%PDF-');
    const foreign = await browser.newContext();const other = await foreign.newPage();await login(other,'khach@luma.local','LumaDemo!2026');const denied = await other.request.get(base+'/don-hang/'+code+'/hoa-don.pdf');assert.equal(denied.status(),404);await foreign.close();
    assert.deepEqual(errors,[]);console.log('Fulfillment browser QA passed: COD order -> admin confirm/one parcel -> handoff -> failed delivery/work/retry -> delivered without collecting -> separate COD receipt -> immutable PDF download; owner access and 1440/390/360px. Order: '+code);
  } finally {
    // Before dispatch, ordinary cancellation safely releases the reservation. A shipped fixture remains as demo history.
    if (code && !dispatched) {
      try { await page.goto(base+'/tai-khoan');const csrf=await page.locator('input[name="_csrf"]').first().inputValue();await page.request.post(base+'/don-hang/'+code+'/huy',{form:{_csrf:csrf},maxRedirects:0}); } catch {}
    }
    await browser.close();
  }
})().catch(e=>{console.error(e);process.exitCode=1;});

async function login(page,email,password){await page.goto(base+'/login');await page.getByLabel('Email',{exact:true}).fill(email);await page.getByLabel('Mật khẩu',{exact:true}).fill(password);await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');}
async function update(page,action,note){await page.getByLabel('Thao tác giao nhận',{exact:true}).selectOption(action);await page.getByLabel('Ghi chú giao nhận (khách có thể xem)',{exact:true}).fill(note);await page.getByRole('button',{name:'Lưu giao nhận',exact:true}).click();assert.match(await page.locator('main').innerText(), /Đã cập nhật giao nhận/);}
async function overflow(page){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow horizontally');}
