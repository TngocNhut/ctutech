const path=require('node:path'),fs=require('node:fs/promises'),assert=require('node:assert/strict');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',out=path.resolve(__dirname,'../.local/qa');
(async()=>{
  const browser=await chromium.launch({channel:'msedge',headless:true}),admin=await browser.newPage({viewport:{width:1440,height:1000}}),customer=await browser.newPage({viewport:{width:390,height:1000}}),errors=[];
  for(const p of [admin,customer])p.on('pageerror',e=>errors.push(e.message));
  try{
    await login(admin,'admin@luma.local');await login(customer,'khach@luma.local');
    for(const route of ['/quan-tri/model','/quan-tri/san-pham','/quan-tri/don-hang','/quan-tri/tra-hang','/quan-tri/hoan-tien','/quan-tri/khuyen-mai','/quan-tri/ma-giam-gia']){
      for(const page of [1,2]){const r=await admin.goto(base+route+'?trang='+page);assert.equal(r.status(),200,route);assert.ok(await admin.locator('main tbody tr').count()<=30);assert.match(await admin.locator('main .catalog-pagination').innerText(),new RegExp('Trang '+page));if(page===2){const prev=new URL(await admin.getByRole('link',{name:'Trang trước',exact:true}).getAttribute('href'),base);assert.equal(prev.searchParams.get('trang'),'1');}await overflow(admin);}
    }
    for(const [route,query]of [['/quan-tri/model','q='+encodeURIComponent('C++ & máy Việt')],['/quan-tri/san-pham','q='+encodeURIComponent('Wi-Fi 6+')],['/quan-tri/don-hang','trangThai=DA_HUY']]){
      await admin.goto(base+route+'?trang=2&'+query);const prev=new URL(await admin.getByRole('link',{name:'Trang trước',exact:true}).getAttribute('href'),base),original=new URLSearchParams(query);for(const [key,value]of original)assert.equal(prev.searchParams.get(key),value,'Filter query must survive pagination');
    }
    for(const route of ['/don-hang','/uu-dai']){assert.equal((await customer.goto(base+route+'?trang=2')).status(),200);assert.ok(await customer.getByRole('link',{name:'Trang trước',exact:true}).count());await overflow(customer);}
    assert.equal((await customer.goto(base+'/uu-dai?trang=0')).status(),400);assert.match(await customer.locator('h1').innerText(),/Yêu cầu chưa hợp lệ/);
    await admin.setViewportSize({width:360,height:1000});await admin.goto(base+'/quan-tri/model?trang=2');await overflow(admin);await admin.screenshot({path:path.join(out,'model-pagination-mobile.png'),fullPage:true});
    await customer.goto(base+'/tai-khoan/ho-tro');await overflow(customer);for(const a of await customer.locator('.nav-row a').all())assert.equal(await a.evaluate(el=>el.getBoundingClientRect().height<=45),true,'Navigation labels stay on one line');await customer.screenshot({path:path.join(out,'support-list-mobile.png'),fullPage:true});
    assert.deepEqual(errors,[]);console.log('Pagination QA passed: 7 admin lists; 30-row bounds; page links; query preservation including literal plus/ampersand; orders/offers; 400; 360px layout and navigation. No business mutations.');
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
async function login(page,email){await page.goto(base+'/login');await page.getByLabel('Email',{exact:true}).fill(email);await page.getByLabel('Mật khẩu',{exact:true}).fill('LumaDemo!2026');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');}
async function overflow(page){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow');}
