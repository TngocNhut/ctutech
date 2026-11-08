const path=require('node:path'),fs=require('node:fs/promises'),assert=require('node:assert/strict');
const {randomUUID}=require('node:crypto');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',out=path.resolve(__dirname,'../.local/qa');
(async()=>{
  const fixture=JSON.parse(await fs.readFile(path.join(out,'return-order.json'),'utf8'));
  const browser=await chromium.launch({channel:'msedge',headless:true}),buyer=await browser.newPage({viewport:{width:390,height:1000}}),admin=await browser.newPage({viewport:{width:1440,height:1050}}),publicPage=await browser.newPage({viewport:{width:360,height:1000}});
  const title='QA nhận xét '+randomUUID().slice(0,12),body='Nhận xét kiểm thử từ đơn demo đã hoàn thành. <script>window.REVIEW_XSS=true</script>',errors=[];let review;
  for(const p of [buyer,admin,publicPage])p.on('pageerror',e=>errors.push(e.message));
  try{
    await login(buyer,fixture.email,'BrowserDemo!2026');await login(admin,'admin@luma.local','LumaDemo!2026');
    await buyer.goto(base+'/tai-khoan/danh-gia');assert.match(await buyer.locator('main').innerText(),/iPhone 13/);await buyer.getByRole('link',{name:/^(Viết|Sửa) đánh giá$/}).first().click();
    await buyer.getByLabel('Số sao',{exact:true}).selectOption('4');await buyer.getByLabel('Tiêu đề đánh giá',{exact:true}).fill(title);await buyer.getByLabel('Nội dung đánh giá',{exact:true}).fill(body);await buyer.getByRole('button',{name:'Lưu đánh giá của tôi',exact:true}).click();assert.match(await buyer.locator('main').innerText(),/Đã lưu đánh giá/);await overflow(buyer);await buyer.screenshot({path:path.join(out,'review-owner-mobile.png'),fullPage:true});
    await admin.goto(base+'/quan-tri/danh-gia?q=iPhone%2013');await admin.locator('tbody tr').filter({has:admin.getByText(title,{exact:true})}).getByRole('link',{name:'Phản hồi & kiểm duyệt',exact:true}).click();review=new URL(admin.url()).pathname.split('/').pop();
    await admin.getByLabel('Cho hiển thị đánh giá công khai',{exact:true}).check();await admin.getByLabel('Phản hồi của cửa hàng',{exact:true}).fill('Cảm ơn nhận xét của khách trong kịch bản kiểm thử. '+title);await admin.getByLabel('Lý do kiểm duyệt / phản hồi',{exact:true}).fill('QA kiểm tra hiển thị và phản hồi đúng khách đã mua');await admin.getByRole('button',{name:'Lưu phản hồi và kiểm duyệt',exact:true}).click();
    assert.equal((await publicPage.goto(base+'/san-pham/iphone-13/danh-gia')).status(),200);const entry=publicPage.locator('.review-entry').filter({has:publicPage.getByRole('heading',{name:'4 / 5 · '+title,exact:true})});assert.equal(await entry.count(),1);assert.match(await entry.innerText(),/Đã mua/);assert.match(await entry.innerText(),/Cảm ơn nhận xét/);assert.ok((await entry.innerText()).includes(body));assert.equal(await publicPage.evaluate(()=>Boolean(window.REVIEW_XSS)),false);assert.ok(!(await publicPage.locator('main').innerText()).includes(fixture.email));assert.ok(!(await publicPage.locator('main').innerText()).includes(fixture.code));await overflow(publicPage);await publicPage.screenshot({path:path.join(out,'review-public-mobile.png'),fullPage:true});
    await admin.setViewportSize({width:360,height:1000});await overflow(admin);await admin.screenshot({path:path.join(out,'review-moderation-mobile.png'),fullPage:true});
    await hide(admin,review);
    await publicPage.reload();assert.equal(await publicPage.getByText(title,{exact:true}).count(),0);
    await buyer.reload();assert.match(await buyer.locator('main').innerText(),/Đánh giá đang bị ẩn/);await buyer.getByLabel('Nội dung đánh giá',{exact:true}).fill(body+' Khách đã cập nhật nội dung.');await buyer.getByRole('button',{name:'Lưu đánh giá của tôi',exact:true}).click();assert.match(await buyer.locator('main').innerText(),/Đánh giá đang bị ẩn/);await publicPage.reload();assert.equal(await publicPage.getByText(title,{exact:true}).count(),0);
    await buyer.goto(base+'/tai-khoan/thong-bao');assert.equal(await buyer.getByRole('heading',{name:'Bạn có phản hồi từ cửa hàng',exact:true}).count(),1);
    assert.deepEqual(errors,[]);console.log('Review browser QA passed: completed/paid buyer writes and edits one model review, literal markup stays text, anonymized public name/purchase label, admin reply/hide, owner edit preserves hidden status; mobile layouts. QA review remains hidden.');
  }finally{
    if(review)try{await hide(admin,review);}catch(e){console.error('QA review cleanup needs review:',review,e.message);process.exitCode=1;}
    await browser.close();
  }
})().catch(e=>{console.error(e);process.exitCode=1;});
async function login(p,email,password){await p.goto(base+'/login');await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();await p.waitForURL('**/tai-khoan');}
async function hide(p,id){await p.goto(base+'/quan-tri/danh-gia/'+id);await p.getByLabel('Cho hiển thị đánh giá công khai',{exact:true}).uncheck();await p.getByLabel('Lý do kiểm duyệt / phản hồi',{exact:true}).fill('QA kết thúc: ẩn nhận xét kiểm thử để không đưa vào điểm công khai');await p.getByRole('button',{name:'Lưu phản hồi và kiểm duyệt',exact:true}).click();assert.match(await p.locator('main').innerText(),/Đã lưu phản hồi/);}
async function overflow(p){assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow');}
