const path=require('node:path');
const fs=require('node:fs/promises');
const assert=require('node:assert/strict');
const {randomUUID}=require('node:crypto');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',out=path.resolve(__dirname,'../.local/qa');

(async()=>{
  // Reuse the hidden product fixture; never change prices/availability of a seed product.
  const fixture=JSON.parse(await fs.readFile(path.join(out,'product-admin-fixture.json'),'utf8'));
  const {product,sku,slug}=fixture;
  const browser=await chromium.launch({channel:'msedge',headless:true});
  const admin=await browser.newPage({viewport:{width:1440,height:1050}});
  const customer=await browser.newPage({viewport:{width:390,height:1000}});
  const suffix=randomUUID().replaceAll('-','').slice(0,12).toUpperCase();
  const campaignCode='QA-C-'+suffix,coupon='QA-V-'+suffix,campaignName='Ưu đãi kiểm thử '+suffix;
  const email='promo-'+suffix.toLowerCase()+'@example.test',errors=[];
  for(const p of [admin,customer])p.on('pageerror',e=>errors.push(e.message));
  let campaign,voucher,order;
  try{
    await login(admin,'admin@luma.local','LumaDemo!2026');
    await update(admin,`/quan-tri/model/${product}/sku/${sku}`,f=>{f.kinhDoanh='true';f['giaTon.giaBan']='10000003';f['giaTon.giaNiemYet']='12000000';f['giaTon.soLuongTon']='2';f['giaTon.lyDo']='QA ưu đãi và gộp SKU ngừng bán';});
    await update(admin,`/quan-tri/model/${product}`,f=>{f.hienThi='true';});
    await customer.goto(base+'/san-pham/'+slug+'?sku='+sku);
    await customer.getByRole('button',{name:'Thêm vào giỏ hàng',exact:true}).click();
    await customer.waitForURL('**/gio-hang');
    await admin.goto(base+'/quan-tri/model/'+product);
    await admin.locator('tr').filter({has:admin.getByText(fixture.skuCode,{exact:true})}).getByRole('button',{name:'Ngừng bán SKU',exact:true}).click();
    await registerVerify(customer,email);
    await login(customer,email,'PromotionDemo!2026');
    assert.match(await customer.locator('[data-cart-merge-notice]').innerText(),/kiểm tra/i);
    await customer.goto(base+'/gio-hang');
    assert.match(await customer.locator('main').innerText(),/Cấu hình đã hết bán/);
    assert.equal(await customer.getByRole('link',{name:'Tiếp tục đặt hàng',exact:true}).count(),0);
    await customer.locator('.cart-line').getByRole('button',{name:'Xóa',exact:true}).click();
    assert.match(await customer.locator('main').innerText(),/Giỏ hàng đang trống/);
    await update(admin,`/quan-tri/model/${product}/sku/${sku}`,f=>{f.kinhDoanh='true';f['giaTon.lyDo']='QA mở lại SKU riêng để kiểm thử giá';});
    await admin.goto(base+'/quan-tri/khuyen-mai/moi');
    await admin.getByLabel('Ký hiệu chiến dịch',{exact:true}).fill(campaignCode);
    await admin.getByLabel('Tên chiến dịch',{exact:true}).fill(campaignName);
    await admin.getByLabel('Bắt đầu (giờ Việt Nam)',{exact:true}).fill('2020-01-01T00:00');
    await admin.getByLabel('Kết thúc (giờ Việt Nam)',{exact:true}).fill('2099-01-01T00:00');
    await admin.getByLabel('Trạng thái chiến dịch',{exact:true}).selectOption('CONG_BO');
    await admin.getByRole('button',{name:'Lưu chiến dịch',exact:true}).click();
    await admin.waitForURL(/\/quan-tri\/khuyen-mai\/\d+$/);campaign=new URL(admin.url()).pathname.split('/').pop();
    await admin.getByLabel('Mã SKU (số)',{exact:true}).fill(sku);
    await admin.getByLabel('Kiểu giảm SKU',{exact:true}).selectOption('PHAN_TRAM');
    await admin.getByLabel('Giá trị giảm SKU',{exact:true}).fill('100');
    await admin.getByRole('button',{name:'Lưu quy tắc SKU',exact:true}).click();
    assert.match(await admin.locator('main').innerText(),/nhỏ hơn 100/);
    assert.equal(await admin.getByLabel('Mã SKU (số)',{exact:true}).inputValue(),sku);
    await admin.getByLabel('Giá trị giảm SKU',{exact:true}).fill('12.34');
    await admin.getByRole('button',{name:'Lưu quy tắc SKU',exact:true}).click();
    assert.match(await admin.locator('main').innerText(),/Đã lưu quy tắc SKU/);assert.match(await admin.locator('main').innerText(),/12,34 %/);
    await admin.setViewportSize({width:390,height:1000});await overflow(admin);
    await admin.screenshot({path:path.join(out,'campaign-admin-mobile.png'),fullPage:true});
    await customer.goto(base+'/san-pham/'+slug+'?sku='+sku);
    assert.equal((await customer.locator('.price').first().innerText()).replace(/\D/g,''),'8766003');
    assert.match(await customer.locator('.campaign-price').innerText(),new RegExp(campaignName));
    await overflow(customer);await customer.screenshot({path:path.join(out,'campaign-product-mobile.png'),fullPage:true});
    await customer.goto(base+'/san-pham?q='+encodeURIComponent('QA iPhone 13')+'&giaTu=8766003&giaDen=8766003');
    assert.equal(await customer.locator('.catalog-products .product').count(),1);
    await admin.goto(base+'/quan-tri/bien-the/'+sku);
    assert.equal(await admin.getByLabel('Giá bán (VND)',{exact:true}).inputValue(),'10000003');
    await admin.goto(base+'/quan-tri/ma-giam-gia/moi');await admin.getByLabel('Kiểu giảm của mã',{exact:true}).selectOption('PHAN_TRAM');await admin.getByLabel('Trần giảm (VND)',{exact:true}).fill('100000');
    for(const[label,value]of[['Mã giảm giá',coupon],['Mô tả mã','Mã riêng kiểm thử ưu đãi, sẽ tắt sau QA'],['Giá trị giảm','10'],['Đơn tối thiểu (VND)','0'],['Tổng lượt tối đa','5'],['Lượt mỗi khách','1']])await admin.getByLabel(label,{exact:true}).fill(value);
    await admin.getByLabel('Bắt đầu (giờ Việt Nam)',{exact:true}).fill('2020-01-01T00:00');
    await admin.getByLabel('Kết thúc (giờ Việt Nam)',{exact:true}).fill('2099-01-01T00:00');
    await admin.getByLabel('Hiện trên trang ưu đãi',{exact:true}).check();await admin.getByLabel('Bật mã giảm giá',{exact:true}).check();
    await admin.getByRole('button',{name:'Lưu mã giảm giá',exact:true}).click();
    await admin.waitForURL(/\/quan-tri\/ma-giam-gia\/\d+$/);voucher=new URL(admin.url()).pathname.split('/').pop();
    await overflow(admin);await admin.screenshot({path:path.join(out,'voucher-admin-mobile.png'),fullPage:true});
    await customer.goto(base+'/uu-dai');assert.match(await customer.locator('main').innerText(),new RegExp(coupon));assert.match(await customer.locator('article').filter({has:customer.getByRole('heading',{name:coupon,exact:true})}).innerText(),/10,00 %/);await overflow(customer);
    await customer.goto(base+'/san-pham/'+slug+'?sku='+sku);await customer.getByRole('button',{name:'Thêm vào giỏ hàng',exact:true}).click();await customer.waitForURL('**/gio-hang');
    await customer.getByRole('link',{name:'Tiếp tục đặt hàng',exact:true}).click();
    for(const[label,value]of[['Người nhận','Khách kiểm thử ưu đãi'],['Điện thoại nhận hàng','0901234567'],['Tỉnh / Thành phố','Thành phố Hồ Chí Minh'],['Phường / Xã','Phường Bến Nghé'],['Số nhà, đường, căn hộ','123 Đường thử nghiệm'],['Mã giảm giá',coupon]])await customer.getByLabel(label,{exact:true}).fill(value);
    await customer.getByRole('button',{name:'Kiểm tra ưu đãi',exact:true}).click();
    assert.match(await customer.locator('main').innerText(),/không cho dùng kèm mã/);
    await update(admin,'/quan-tri/khuyen-mai/'+campaign,f=>{f.kemMa='true';});
    await customer.getByRole('button',{name:'Kiểm tra ưu đãi',exact:true}).click();
    assert.match(await customer.locator('main').innerText(),/8\.696\.003/);
    await customer.getByRole('button',{name:'Đặt hàng',exact:true}).click();await customer.waitForURL('**/don-hang/LM-*');order=new URL(customer.url()).pathname.split('/').pop();
    await update(admin,'/quan-tri/khuyen-mai/'+campaign,f=>{f.trangThai='TAM_DUNG';f.ten='Tên chiến dịch đã đổi sau khi đặt';});
    await customer.reload();assert.match(await customer.locator('main').innerText(),new RegExp(campaignName));assert.match(await customer.locator('main').innerText(),/8\.696\.003/);await overflow(customer);
    await customer.screenshot({path:path.join(out,'campaign-order-mobile.png'),fullPage:true});
    await customer.getByRole('button',{name:'Hủy trước khi bàn giao',exact:true}).click();assert.match(await customer.locator('main').innerText(),/Đã hủy/);order=null;
    await admin.setViewportSize({width:1440,height:1050});await admin.goto(base+'/quan-tri/khuyen-mai');await overflow(admin);await admin.screenshot({path:path.join(out,'campaign-list-desktop.png'),fullPage:true});
    assert.deepEqual(errors,[]);
    console.log('Promotion/cart browser QA passed: discontinued guest SKU retained on login; campaign/rule/voucher admin validation; exact shared price and filter; admin base preserved; no-stack rejection -> allowed voucher; immutable order snapshot; cancel and fixture cleanup.');
  }finally{
    const cleanup=[
      async()=>{if(order){await customer.goto(base+'/don-hang/'+order);await customer.getByRole('button',{name:'Hủy trước khi bàn giao',exact:true}).click();}},
      async()=>{if(campaign)await update(admin,'/quan-tri/khuyen-mai/'+campaign,f=>{f.trangThai='TAM_DUNG';});},
      async()=>{if(voucher)await update(admin,'/quan-tri/ma-giam-gia/'+voucher,f=>{delete f.hoatDong;delete f.congKhai;});},
      async()=>update(admin,`/quan-tri/model/${product}/sku/${sku}`,f=>{delete f.kinhDoanh;f['giaTon.soLuongTon']='0';f['giaTon.lyDo']='QA kết thúc: trả SKU mẫu về ngừng bán, tồn 0';}),
      async()=>update(admin,`/quan-tri/model/${product}`,f=>{delete f.hienThi;})
    ];
    for(const work of cleanup)try{await work();}catch(e){console.error('QA cleanup needs review:',{campaign,voucher,product,sku},e.message);process.exitCode=1;}
    await browser.close();
  }
})().catch(e=>{console.error(e);process.exitCode=1;});

async function login(page,email,password){await page.goto(base+'/login');await page.getByLabel('Email',{exact:true}).fill(email);await page.getByLabel('Mật khẩu',{exact:true}).fill(password);await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');}
async function overflow(page){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow');}
async function update(page,url,change){await page.goto(base+url);const form=await page.locator('main form').first().evaluate(f=>Object.fromEntries(new FormData(f)));change(form);const response=await page.request.post(base+url,{form,maxRedirects:0});assert.equal(response.status(),302,'Admin update '+url+' must succeed');}
async function registerVerify(page,email){
  await page.goto(base+'/dang-ky');for(const[label,value]of[['Họ tên','Khách kiểm thử ưu đãi'],['Email',email],['Mật khẩu','PromotionDemo!2026'],['Nhập lại mật khẩu','PromotionDemo!2026']])await page.getByLabel(label,{exact:true}).fill(value);
  await page.getByRole('button',{name:'Tạo tài khoản',exact:true}).click();await page.waitForURL('**/login?registered');
  const mail=path.resolve(__dirname,'../.local/mail');let link;
  for(const file of await fs.readdir(mail)){const content=await fs.readFile(path.join(mail,file),'utf8');if(content.includes('Đến: '+email+'\n'))link=content.match(/http:\/\/localhost:8082\/xac-minh-email\?token=[A-Za-z0-9_-]+/)?.[0];}
  assert.ok(link);await page.goto(link.replace('localhost','127.0.0.1'));await page.getByRole('button',{name:'Xác nhận email',exact:true}).click();
}

