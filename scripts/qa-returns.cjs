const path=require('node:path');const fs=require('node:fs/promises');const assert=require('node:assert/strict');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',output=path.resolve(__dirname,'../.local/qa');
(async()=>{
  const fixture=JSON.parse(await fs.readFile(path.join(output,'fulfillment-order.json'),'utf8'));
  assert.equal(fixture.dispatched,true,'Run qa-fulfillment first. This script returns that same demo item.');
  const browser=await chromium.launch({channel:'msedge',headless:true});const customer=await browser.newContext({viewport:{width:390,height:1000}}),admin=await browser.newContext({viewport:{width:1440,height:1050}});
  const page=await customer.newPage(),staff=await admin.newPage(),errors=[];for(const p of [page,staff])p.on('pageerror',e=>errors.push(e.message));
  try{
    await login(page,fixture.email,'BrowserDemo!2026');await login(staff,'admin@luma.local','LumaDemo!2026');
    await page.goto(base+'/don-hang/'+fixture.code);let ticket;
    const existing=page.getByRole('link',{name:/^Phiếu #/}).first();
    if(await existing.count()) {await existing.click();ticket=new URL(page.url()).pathname.split('/').pop();}
    else{
      await page.getByRole('link',{name:'Yêu cầu trả hàng',exact:true}).click();
      await page.locator('input[name="dong[0].soLuong"]').fill('1');await page.getByLabel('Lý do trả hàng',{exact:true}).fill('Kiểm thử trả hàng demo: đổi nhu cầu sử dụng');
      await overflow(page);await page.screenshot({path:path.join(output,'return-request-mobile.png'),fullPage:true});
      await page.getByRole('button',{name:'Gửi yêu cầu trả',exact:true}).click();await page.waitForURL('**/don-hang/tra-hang/*');ticket=new URL(page.url()).pathname.split('/').pop();
    }
    await fs.writeFile(path.join(output,'return-order.json'),JSON.stringify({...fixture,ticket},null,2));
    await staff.goto(base+'/quan-tri/tra-hang');await staff.getByRole('link',{name:'Phiếu #'+ticket,exact:true}).click();
    if(await staff.getByRole('button',{name:'Lưu kết quả duyệt',exact:true}).count()){
      await staff.getByLabel('Phí giao được hoàn (VND)',{exact:true}).fill('30000');await staff.getByLabel('Kết quả duyệt',{exact:true}).fill('Đồng ý nhận hàng và hoàn phí giao cho kịch bản demo');
      await staff.getByRole('button',{name:'Lưu kết quả duyệt',exact:true}).click();assert.match(await staff.locator('main').innerText(),/Đã duyệt nhận hàng/);
    }
    if(await staff.getByRole('button',{name:'Ghi nhận kiện đang về kho',exact:true}).count())await staff.getByRole('button',{name:'Ghi nhận kiện đang về kho',exact:true}).click();
    if(await staff.getByRole('button',{name:'Chốt kiểm hàng & nhập lại phần đủ điều kiện',exact:true}).count()){
      await staff.getByLabel('Số máy thực nhận',{exact:true}).fill('1');await staff.getByLabel('Số máy duyệt hoàn tiền',{exact:true}).fill('1');await staff.getByLabel('Số máy đủ điều kiện nhập lại',{exact:true}).fill('1');await staff.getByLabel('Kết quả kiểm hàng',{exact:true}).fill('Máy demo nguyên vẹn, đủ phụ kiện, đủ điều kiện bán lại');await staff.getByLabel('Đã nhận và kiểm toàn bộ hàng ghi trên phiếu',{exact:true}).check();
      await overflow(staff);await staff.screenshot({path:path.join(output,'return-inspection-desktop.png'),fullPage:true});await staff.setViewportSize({width:360,height:1000});await overflow(staff);await staff.screenshot({path:path.join(output,'return-inspection-mobile.png'),fullPage:true});
      await staff.getByRole('button',{name:'Chốt kiểm hàng & nhập lại phần đủ điều kiện',exact:true}).click();assert.match(await staff.locator('main').innerText(),/Đã chốt kiểm hàng/);
    }
    await staff.setViewportSize({width:360,height:1000});await overflow(staff);await staff.screenshot({path:path.join(output,'return-results-mobile.png'),fullPage:true});
    await staff.goto(base+'/quan-tri/don-hang/'+fixture.code);await staff.getByRole('link',{name:/^Khoản hoàn #/}).first().click();
    if(await staff.getByRole('button',{name:'Ghi nhận hoàn tiền theo chứng từ',exact:true}).count()){
      assert.match(await staff.locator('main').innerText(),/Dữ liệu mô phỏng/);
      for(const [label,value]of [['Tài khoản nhận hoàn','DEMO-12345678'],['Ngân hàng nhận hoàn','NGAN HANG DEMO'],['Tên người nhận hoàn','NGUYEN THI ANH'],['Mã chứng từ / tham chiếu hoàn','QA-REFUND-'+fixture.code]])await staff.getByLabel(label,{exact:true}).fill(value);
      await staff.getByLabel('Chứng từ PDF, PNG hoặc JPEG (tối đa 5 MB)',{exact:true}).setInputFiles(path.join(output,'invoice-browser.pdf'));
      await staff.getByLabel('Đã kiểm tra người nhận, số tiền và chứng từ hoàn',{exact:true}).check();await overflow(staff);await staff.screenshot({path:path.join(output,'refund-form-mobile.png'),fullPage:true});
      await staff.getByRole('button',{name:'Ghi nhận hoàn tiền theo chứng từ',exact:true}).click();assert.match(await staff.locator('main').innerText(),/Đã ghi nhận hoàn tiền theo chứng từ/);
    }
    await page.goto(base+'/don-hang/'+fixture.code);assert.match(await page.locator('main').innerText(),/Đã hoàn tiền/);assert.match(await page.locator('main').innerText(),/Chứng từ hoàn nội bộ/);await overflow(page);await page.screenshot({path:path.join(output,'returned-order-mobile.png'),fullPage:true});
    const [download]=await Promise.all([page.waitForEvent('download'),page.getByRole('link',{name:'Tải chứng từ đối soát hoàn',exact:true}).click()]);assert.match(download.suggestedFilename(),/\.pdf$/);await download.saveAs(path.join(output,'refund-proof-browser.pdf'));assert.deepEqual(await fs.readFile(path.join(output,'refund-proof-browser.pdf')),await fs.readFile(path.join(output,'invoice-browser.pdf')));
    const other=await browser.newContext();const stranger=await other.newPage();await login(stranger,'khach@luma.local','LumaDemo!2026');const denied=await stranger.request.get(base+'/don-hang/tra-hang/'+ticket);assert.equal(denied.status(),404);await other.close();
    assert.deepEqual(errors,[]);console.log('Return/refund browser QA passed: existing completed COD order -> request -> approval/reverse parcel -> inspect/restock -> manual mock refund with private proof -> customer history/download; no actual funds transferred. Returned fixture: '+fixture.code);
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
async function login(page,email,password){await page.goto(base+'/login');await page.getByLabel('Email',{exact:true}).fill(email);await page.getByLabel('Mật khẩu',{exact:true}).fill(password);await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');}
async function overflow(page){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow horizontally');}
