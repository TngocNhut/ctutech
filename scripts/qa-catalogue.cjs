const path=require('node:path');const fs=require('node:fs/promises');const assert=require('node:assert/strict');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',output=path.resolve(__dirname,'../.local/qa');
(async()=>{
  await fs.mkdir(output,{recursive:true});const browser=await chromium.launch({channel:'msedge',headless:true});const errors=[];
  try{
    for(const width of [1440,390,360]){
      const context=await browser.newContext({viewport:{width,height:1000}});const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
      assert.equal((await page.goto(base+'/')).status(),200);assert.equal(await page.locator('.product').count(),12);assert.match(await page.locator('.section-top').innerText(),/180 sản phẩm/);
      for(const img of await page.locator('.product-media img').all()){await img.scrollIntoViewIfNeeded();await img.evaluate(image=>image.decode());assert.ok(await img.evaluate(image=>image.naturalWidth>=300));}
      await page.evaluate(()=>window.scrollTo(0,0));await overflow(page);await page.screenshot({path:path.join(output,'catalogue-'+width+'.png'),fullPage:true});
      await page.getByRole('link',{name:'Trang tiếp',exact:true}).click();assert.equal(await page.locator('.product').count(),12);assert.match(await page.locator('.catalog-pagination').innerText(),/Trang 2 \/ 15/);for(const img of await page.locator('.product-media img').all()){await img.scrollIntoViewIfNeeded();await img.evaluate(image=>image.decode());assert.ok(await img.evaluate(image=>image.naturalWidth>=300));}
      await page.goto(base+'/san-pham?loai=dien-thoai');if(!await page.locator('[data-catalog-filter]').evaluate(el=>el.open))await page.locator('[data-catalog-filter] summary').click();
      await page.getByLabel('Thương hiệu',{exact:true}).selectOption('samsung');await page.getByLabel('Sắp xếp',{exact:true}).selectOption('gia-tang');await page.getByLabel('Chỉ xem cấu hình còn hàng',{exact:true}).check();await page.getByRole('button',{name:'Áp dụng bộ lọc',exact:true}).click();
      assert.equal(await page.locator('.product').count(),12);assert.match(await page.locator('.section-top').innerText(),/17 sản phẩm/);assert.equal(await page.getByLabel('Thương hiệu',{exact:true}).inputValue(),'samsung');
      await page.getByRole('link',{name:'Xóa bộ lọc',exact:true}).click();assert.equal(await page.locator('.product').count(),12);
      await page.goto(base+'/san-pham?q=iPhone%2013&luuTru=128&giaDen=14000000');assert.equal(await page.locator('.product').count(),1);await page.locator('.product-link').click();assert.match(page.url(),/sku=1/);
      const photo=page.locator('[data-gallery-main]');await photo.evaluate(image=>image.decode());assert.ok(await photo.evaluate(image=>image.naturalWidth>=300));assert.match(await page.locator('.product-gallery').innerText(),/Ảnh tham khảo model/);await overflow(page);await page.screenshot({path:path.join(output,'product-image-'+width+'.png'),fullPage:true});
      await page.goto(base+'/san-pham?ram=abc');assert.match(await page.locator('.alert-danger').innerText(),/Kiểm tra lại RAM/);assert.doesNotMatch(await page.locator('.alert-danger').innerText(),/java\.|Exception|Failed to convert/);await overflow(page);
      await context.close();
    }
    assert.deepEqual(errors,[]);console.log('Catalogue browser QA passed: 180 sourced models, 12-item pages, filter/sort/reset/state, matching SKU links, readable errors and gallery at 1440/390/360px.');
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
async function overflow(page){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow horizontally');}

