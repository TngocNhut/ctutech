const path=require('node:path'),fs=require('node:fs/promises'),assert=require('node:assert/strict');
const {randomUUID}=require('node:crypto'),{execFile}=require('node:child_process'),{promisify}=require('node:util');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',root=path.resolve(__dirname,'..'),out=path.join(root,'.local/qa');
(async()=>{
  const returned=JSON.parse(await fs.readFile(path.join(out,'return-order.json'),'utf8'));assert.match(String(returned.ticket),/^\d+$/);
  const stateFile=path.join(out,'shipping-task-fixture.json');let fixture;
  try{fixture=JSON.parse(await fs.readFile(stateFile,'utf8'));}catch(e){if(e.code!=='ENOENT')throw e;}
  if(!fixture||fixture.ticket!==returned.ticket){
    fixture={ticket:returned.ticket,baseKey:randomUUID(),extraKey:randomUUID()};await fs.writeFile(stateFile,JSON.stringify(fixture,null,2));
  }
  // Only seed a QA follow-up on the already returned parcel. No new order, stock move or receipt.
  const pg=path.join(process.env.PG_BIN||'C:/Program Files/PostgreSQL/17/bin','psql.exe');
  const password=(await fs.readFile(path.join(root,'.local/db-password.txt'),'utf8')).trim();
  const sql=`INSERT INTO "ViecXuLyGiaoNhan" ("MaVanDon","LoaiViec","GhiChu","HanXuLy")
    SELECT v."MaVanDon",'DOI_SOAT_VAN_DON','QA task screen for returned fixture',clock_timestamp()+interval '1 day'
    FROM "VanDon" v JOIN "PhieuTraHang" f USING ("MaPhieuTraHang")
    WHERE v."MaPhieuTraHang"=${returned.ticket} AND v."LoaiVanDon"='HOAN_VE' AND f."TrangThai"='HOAN_THANH'
      AND NOT EXISTS (SELECT 1 FROM "ViecXuLyGiaoNhan" x WHERE x."MaVanDon"=v."MaVanDon" AND x."LoaiViec"='DOI_SOAT_VAN_DON' AND x."GhiChu"='QA task screen for returned fixture');
    SELECT x."MaViecXuLy" FROM "ViecXuLyGiaoNhan" x JOIN "VanDon" v USING ("MaVanDon")
    WHERE v."MaPhieuTraHang"=${returned.ticket} AND x."LoaiViec"='DOI_SOAT_VAN_DON' AND x."GhiChu"='QA task screen for returned fixture' ORDER BY x."MaViecXuLy" DESC LIMIT 1;`;
  const result=await promisify(execFile)(pg,['-X','-q','-t','-A','-h','127.0.0.1','-p','55432','-U','luma','-d','luma_shop','-c',sql],{env:{...process.env,PGPASSWORD:password},windowsHide:true});
  const task=result.stdout.trim();assert.match(task,/^\d+$/,'Run qa-returns first; this QA needs the completed returned parcel.');fixture.task=task;await fs.writeFile(stateFile,JSON.stringify(fixture,null,2));
  const browser=await chromium.launch({channel:'msedge',headless:true}),page=await browser.newPage({viewport:{width:1440,height:1050}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  try{
    await page.goto(base+'/login');await page.getByLabel('Email',{exact:true}).fill('admin@luma.local');await page.getByLabel('Mật khẩu',{exact:true}).fill('LumaDemo!2026');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');
    assert.equal((await page.goto(base+'/quan-tri/tra-hang/'+returned.ticket)).status(),200);
    const costs=page.locator('section').filter({has:page.getByRole('heading',{name:'Cước vận chuyển chiều hoàn',exact:true})});
    if(!/Đã ghi cước gốc/.test(await costs.innerText()))await fee(page,returned.ticket,fixture.baseKey,'CUOC_GOC','0','QA cước gốc chiều hoàn bằng 0');
    assert.match(await costs.innerText(),/Đã ghi cước gốc/);await fee(page,returned.ticket,fixture.extraKey,'PHU_PHI','1000','QA phụ phí chiều hoàn');
    const before=await costs.locator('tbody tr').count();await fee(page,returned.ticket,fixture.extraKey,'PHU_PHI','1000','QA phụ phí chiều hoàn');assert.equal(await costs.locator('tbody tr').count(),before,'Fee replay must not append a second line');
    await page.setViewportSize({width:360,height:1000});await overflow(page);await page.screenshot({path:path.join(out,'reverse-cost-mobile.png'),fullPage:true});
    assert.equal((await page.goto(base+'/quan-tri/giao-nhan/'+task)).status(),200);
    if(await page.getByRole('button',{name:'Lưu xử lý việc',exact:true}).count()){
      await page.getByLabel('Ghi chú xử lý việc',{exact:true}).fill('QA đã liên hệ đối tác và cập nhật hạn kiểm tra cước');await page.getByLabel('Hạn xử lý việc (giờ Việt Nam)',{exact:true}).fill('2026-12-01T10:30:00');await page.getByRole('button',{name:'Lưu xử lý việc',exact:true}).click();assert.match(await page.locator('main').innerText(),/Đã lưu ghi chú và hạn/);
      await page.getByLabel('Ghi chú xử lý việc',{exact:true}).fill('QA đã đối chiếu, đóng công việc theo dõi cước');await page.getByLabel('Đóng việc này sau khi lưu ghi chú',{exact:true}).check();await page.getByRole('button',{name:'Lưu xử lý việc',exact:true}).click();
    }
    assert.match(await page.locator('main').innerText(),/Phụ trách: Quản trị LUMA/);assert.doesNotMatch(await page.locator('main').innerText(),/viec\.NguoiPhuTrach/);assert.match(await page.locator('main').innerText(),/Đã xử lý/);assert.match(await page.locator('main').innerText(),/QA đã đối chiếu/);assert.equal(await page.getByRole('button',{name:'Lưu xử lý việc',exact:true}).count(),0);await overflow(page);await page.screenshot({path:path.join(out,'shipping-task-mobile.png'),fullPage:true});
    await page.goto(base+'/quan-tri/giao-nhan?daDong=true');await overflow(page);assert.match(await page.locator('main').innerText(),/QA task screen for returned fixture/);
    await page.setViewportSize({width:1440,height:1050});await page.goto(base+'/quan-tri/giao-nhan/'+task);await overflow(page);await page.screenshot({path:path.join(out,'shipping-task-desktop.png'),fullPage:true});
    assert.deepEqual(errors,[]);console.log('Shipping task/reverse cost browser QA passed: reuse completed return, unknown vs zero base fee, idempotent surcharge, task note/deadline/manual closure/history; mobile/desktop; no new order or stock/receipt/refund changes.');
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
async function fee(page,ticket,key,type,value,note){const form=await page.locator('form').filter({has:page.getByRole('button',{name:'Ghi cước chiều hoàn',exact:true})}).evaluate(f=>Object.fromEntries(new FormData(f)));Object.assign(form,{khoaChongLap:key,loaiPhi:type,soTien:value,ghiChu:note});const r=await page.request.post(base+'/quan-tri/tra-hang/'+ticket+'/cuoc',{form,maxRedirects:0});assert.equal(r.status(),302);await page.reload();assert.match(await page.locator('main').innerText(),/Đã ghi cước chiều hoàn/);}
async function overflow(page){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow');}
