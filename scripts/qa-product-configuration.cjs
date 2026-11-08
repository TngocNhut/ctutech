const path=require('node:path'),fs=require('node:fs/promises'),assert=require('node:assert/strict');
const {randomUUID}=require('node:crypto');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',out=path.resolve(__dirname,'../.local/qa');
(async()=>{
  const fixture=JSON.parse(await fs.readFile(path.join(out,'product-admin-fixture.json'),'utf8')),{product,sku,slug}=fixture;
  const browser=await chromium.launch({channel:'msedge',headless:true});const admin=await browser.newPage({viewport:{width:1440,height:1000}}),publicPage=await browser.newPage({viewport:{width:390,height:1000}});
  const suffix=randomUUID().replaceAll('-','').slice(0,10),attributes=[];let line;const errors=[];
  for(const p of [admin,publicPage])p.on('pageerror',e=>errors.push(e.message));
  const attrPath=`/quan-tri/model/${product}/sku/${sku}/thuoc-tinh`;
  async function post(url,form){await admin.goto(base+'/tai-khoan');const csrf=await admin.locator('input[name="_csrf"]').first().inputValue();const r=await admin.request.post(base+url,{form:{_csrf:csrf,...form},maxRedirects:0});assert.equal(r.status(),302,url);return r;}
  try{
    await admin.goto(base+'/login');await admin.getByLabel('Email',{exact:true}).fill('admin@luma.local');await admin.getByLabel('Mật khẩu',{exact:true}).fill('LumaDemo!2026');await admin.getByRole('button',{name:'Đăng nhập',exact:true}).click();await admin.waitForURL('**/tai-khoan');
    await admin.goto(base+'/quan-tri/dong-san-pham/moi');await admin.getByLabel('Hãng của dòng máy',{exact:true}).selectOption('1');await admin.getByLabel('Tên dòng sản phẩm',{exact:true}).fill('Dòng QA '+suffix);await admin.getByLabel('Đường dẫn dòng sản phẩm',{exact:true}).fill('qa-'+suffix);await admin.getByRole('button',{name:'Lưu dòng sản phẩm',exact:true}).click();await admin.waitForURL(/\/dong-san-pham\/\d+$/);line=new URL(admin.url()).pathname.split('/').pop();
    await post(`/quan-tri/model/${product}/dong`,{maDong:line});
    for(const [type,name,value,unit]of [['SO','Camera QA','48.125','MP'],['CHU','Mạng QA','Wi-Fi 6+ <script>test</script>',''],['CO_KHONG','NFC QA','false','']]){
      await admin.goto(base+'/quan-tri/thuoc-tinh/moi');await admin.getByLabel('Danh mục của thuộc tính',{exact:true}).selectOption('1');await admin.getByLabel('Ký hiệu thuộc tính',{exact:true}).fill(('QA_'+type+'_'+suffix).toUpperCase());await admin.getByLabel('Tên thuộc tính',{exact:true}).fill(name+' '+suffix);await admin.getByLabel('Kiểu giá trị',{exact:true}).selectOption(type);await admin.getByLabel('Đơn vị (nếu có)',{exact:true}).fill(unit);await admin.getByLabel('Cho dùng thuộc tính trong bộ lọc sản phẩm',{exact:true}).check();await admin.getByRole('button',{name:'Lưu định nghĩa thuộc tính',exact:true}).click();await admin.waitForURL(/\/thuoc-tinh\/\d+$/);const id=new URL(admin.url()).pathname.split('/').pop();attributes.push({id,type,name,value});
      await admin.goto(base+attrPath+'/'+id);const input=admin.locator('#attribute-value');if(type==='CO_KHONG')await input.selectOption(value);else await input.fill(value);await admin.getByRole('button',{name:'Lưu giá trị thuộc tính',exact:true}).click();await admin.waitForURL(base+attrPath);assert.match(await admin.locator('main').innerText(),new RegExp(name));
    }
    await update(admin,`/quan-tri/model/${product}/sku/${sku}`,f=>{f.kinhDoanh='true';f['giaTon.soLuongTon']='1';f['giaTon.lyDo']='QA thông số SKU riêng';});await update(admin,`/quan-tri/model/${product}`,f=>{f.hienThi='true';});
    await publicPage.goto(base+'/san-pham/'+slug+'?sku='+sku);let text=await publicPage.locator('main').innerText();assert.match(text,/48\.125 MP/);assert.match(text,/Wi-Fi 6\+ <script>test<\/script>/);assert.match(text,/Không/);assert.match(text,new RegExp('Dòng QA '+suffix));assert.equal(await publicPage.locator('main script').count(),0);await overflow(publicPage);await publicPage.screenshot({path:path.join(out,'product-configuration-mobile.png'),fullPage:true});
    const query=new URLSearchParams({dong:line});for(const a of attributes)query.set('tt.'+a.id,a.value);await publicPage.goto(base+'/san-pham?'+query);assert.equal(await publicPage.locator('.catalog-products .product').count(),1);assert.match(await publicPage.locator('.product-link').getAttribute('href'),new RegExp('sku='+sku+'$'));assert.equal(await publicPage.locator('#filter-attr-'+attributes[1].id).inputValue(),attributes[1].value);await overflow(publicPage);await publicPage.screenshot({path:path.join(out,'extended-filter-mobile.png'),fullPage:true});
    await publicPage.goto(base+'/san-pham?tt.'+attributes[0].id+'=NaN');assert.match(await publicPage.locator('.alert-danger').innerText(),/Nhập số/);
    await admin.setViewportSize({width:390,height:1000});await admin.goto(base+attrPath);await overflow(admin);await admin.screenshot({path:path.join(out,'sku-attributes-admin-mobile.png'),fullPage:true});
    assert.deepEqual(errors,[]);console.log('Product configuration QA passed: line assignment; typed attributes; literal text; public selected SKU; combined filters and invalid input; mobile layout.');
  }finally{
    const cleanup=[()=>update(admin,`/quan-tri/model/${product}/sku/${sku}`,f=>{delete f.kinhDoanh;f['giaTon.soLuongTon']='0';f['giaTon.lyDo']='QA kết thúc: SKU mẫu ngừng bán, tồn 0';}),()=>update(admin,`/quan-tri/model/${product}`,f=>{delete f.hienThi;}),()=>post(`/quan-tri/model/${product}/dong`,{maDong:'0'}),...attributes.flatMap(a=>[()=>post(attrPath+'/'+a.id+'/xoa',{}),()=>post('/quan-tri/thuoc-tinh/'+a.id+'/xoa',{})]),()=>line&&post('/quan-tri/dong-san-pham/'+line+'/xoa',{})];
    for(const work of cleanup)try{await work();}catch(e){console.error('QA configuration cleanup needs review:',e.message);process.exitCode=1;}await browser.close();
  }
})().catch(e=>{console.error(e);process.exitCode=1;});
async function overflow(page){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow');}
async function update(page,url,change){await page.goto(base+url);const form=await page.locator('main form').first().evaluate(f=>Object.fromEntries(new FormData(f)));change(form);const r=await page.request.post(base+url,{form,maxRedirects:0});assert.equal(r.status(),302,'Admin update '+url);}
