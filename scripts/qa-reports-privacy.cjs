const path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082';
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});try{
 const ctx=await browser.newContext(),page=await ctx.newPage(),errors=[],events=[];
 page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>{if(r.url().endsWith('/thong-ke/su-kien'))events.push(JSON.parse(r.postData()));});
 await page.goto(base+'/');await page.waitForTimeout(500);assert.equal(events.length,0,'default privacy');
 await page.goto(base+'/quyen-rieng-tu');await page.getByRole('checkbox').check();await page.getByRole('button',{name:'Lưu lựa chọn thống kê'}).click();
 await page.goto(base+'/san-pham?q=iphone');await page.waitForTimeout(800);assert(events.some(e=>e.loai==='TIM_KIEM'));assert(!JSON.stringify(events).includes('iphone'),'no search contents');
 const token=await page.locator('[data-analytics-config]').evaluate(e=>({header:e.dataset.header,token:e.dataset.token}));
 const bad=await ctx.request.post(base+'/thong-ke/su-kien',{data:{loai:'XEM_TRANG',nhom:'TRANG_CHU',khoa:crypto.randomUUID()}});assert.equal(bad.status(),403,'CSRF enforced');
 const valid=await ctx.request.post(base+'/thong-ke/su-kien',{headers:{[token.header]:token.token},data:{loai:'XEM_TRANG',nhom:'TRANG_CHU',khoa:crypto.randomUUID()}});assert.equal(valid.status(),200);
 await page.goto(base+'/quyen-rieng-tu');await page.getByRole('checkbox').uncheck();await page.getByRole('button',{name:'Lưu lựa chọn thống kê'}).click();const before=events.length;await page.goto(base+'/');await page.waitForTimeout(500);assert.equal(events.length,before,'opt out stops requests');
 await page.goto(base+'/login');assert.equal(await page.locator('a[href^="/oauth2/authorization/"]').count(),0,'disabled providers');
 await page.getByLabel('Email',{exact:true}).fill('admin@luma.local');await page.getByLabel('Mật khẩu',{exact:true}).fill('LumaDemo!2026');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');
 for(const width of [1440,390,360]){await page.setViewportSize({width,height:950});for(const route of ['/quan-tri','/quan-tri/bao-cao?tu=1900-01-01&den=1900-01-31&nhom=month','/tai-khoan/lien-ket']){assert.equal((await page.goto(base+route)).status(),200,route);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'overflow '+route);}
 await page.goto(base+'/quan-tri/bao-cao');await page.screenshot({path:path.resolve(__dirname,`../.local/qa/reports-${width}.png`),fullPage:true});}
 const csv=await ctx.request.get(base+'/quan-tri/bao-cao/doanh-so.csv?tu=1900-01-01&den=1900-01-31&nhom=day');assert.equal(csv.status(),200);assert.match(csv.headers()['content-type'],/text\/csv/);assert((await csv.body()).length>20);
 await page.goto(base+'/tai-khoan');await page.getByRole('button',{name:'Đăng xuất',exact:true}).click();
 const outsider=await ctx.request.get(base+'/quan-tri/bao-cao/doanh-so.csv?tu=1900-01-01&den=1900-01-31&nhom=day',{maxRedirects:0});assert.equal(outsider.status(),302);
 assert.deepEqual(errors,[]);await ctx.close();console.log('Reports/privacy/OAuth-disabled browser QA passed; CSV, authorization, CSRF, consent and responsive 1440/390/360.');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
