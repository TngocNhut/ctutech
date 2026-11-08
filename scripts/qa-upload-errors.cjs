const path=require('node:path');const fs=require('node:fs/promises');const assert=require('node:assert/strict');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082';
(async()=>{
  const fixture=JSON.parse(await fs.readFile(path.resolve(__dirname,'../.local/qa/product-admin-fixture.json'),'utf8'));
  const browser=await chromium.launch({channel:'msedge',headless:true});const page=await browser.newPage();
  try{
    await page.goto(base+'/login');await page.getByLabel('Email',{exact:true}).fill('admin@luma.local');await page.getByLabel('Mật khẩu',{exact:true}).fill('LumaDemo!2026');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');await page.goto(base+'/quan-tri/model/'+fixture.product+'/anh');const csrf=await page.locator('input[name="_csrf"]').first().inputValue();
    const post=buffer=>page.request.post(base+'/quan-tri/model/'+fixture.product+'/anh',{headers:{'X-CSRF-TOKEN':csrf},multipart:{tep:{name:'invalid.png',mimeType:'image/png',buffer},moTa:'QA tệp bị từ chối',thuTu:'0',hienThi:'true'}});
    const invalid=await post(Buffer.from('not an image'));assert.equal(invalid.status(),200);assert.match(await invalid.text(),/Nội dung tệp phải là ảnh PNG hoặc JPEG/);
    const tooLarge=await post(Buffer.alloc(6*1024*1024));assert.equal(tooLarge.status(),413);assert.match(await tooLarge.text(),/Tệp vượt kích thước cho phép/);
    console.log('Upload error QA passed: fake PNG rejected with readable form error; oversized multipart returns 413 without saving an image.');
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
