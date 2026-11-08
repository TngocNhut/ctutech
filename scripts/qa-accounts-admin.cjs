const path=require('node:path'),fs=require('node:fs/promises'),assert=require('node:assert/strict');
const {randomUUID}=require('node:crypto');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082',out=path.resolve(__dirname,'../.local/qa');
(async()=>{
  const browser=await chromium.launch({channel:'msedge',headless:true});
  const admin=await browser.newPage({viewport:{width:1440,height:1050}}),customer=await browser.newPage({viewport:{width:390,height:1000}}),other=await browser.newPage();
  const email='account-qa-'+randomUUID()+'@example.test',password='AccountDemo!2026';let id,address;
  const errors=[];for(const p of [admin,customer,other])p.on('pageerror',e=>errors.push(e.message));
  try{
    await register(customer,email,password);await login(customer,email,password);await login(admin,'admin@luma.local','LumaDemo!2026');
    await customer.goto(base+'/tai-khoan/dia-chi');
    for(const[label,value]of[['Người nhận','Khách kiểm thử địa chỉ'],['Điện thoại nhận hàng','0901234567'],['Tỉnh / Thành phố','Thành phố Hồ Chí Minh'],['Phường / Xã','Phường Bến Nghé'],['Số nhà, đường, căn hộ','123 Đường kiểm thử']])await customer.getByLabel(label,{exact:true}).fill(value);
    await customer.getByLabel('Đặt làm địa chỉ mặc định',{exact:true}).check();await customer.getByRole('button',{name:'Lưu địa chỉ',exact:true}).click();
    await customer.getByRole('link',{name:'Sửa địa chỉ',exact:true}).click();address=new URL(customer.url()).pathname.split('/').at(-2);
    assert.equal(await customer.getByLabel('Số nhà, đường, căn hộ',{exact:true}).inputValue(),'123 Đường kiểm thử');assert.ok(await customer.getByLabel('Đặt làm địa chỉ mặc định',{exact:true}).isChecked());
    await customer.getByLabel('Số nhà, đường, căn hộ',{exact:true}).fill('456 Đường đã sửa');await overflow(customer);await customer.screenshot({path:path.join(out,'address-edit-mobile.png'),fullPage:true});
    await customer.getByRole('button',{name:'Lưu thay đổi địa chỉ',exact:true}).click();assert.match(await customer.locator('main').innerText(),/456 Đường đã sửa/);
    await login(other,'khach@luma.local','LumaDemo!2026');assert.equal((await other.goto(base+`/tai-khoan/dia-chi/${address}/sua`)).status(),404);
    await admin.goto(base+'/quan-tri/nguoi-dung?q='+encodeURIComponent(email));await admin.getByRole('link',{name:'Quyền & phiên',exact:true}).click();id=new URL(admin.url()).pathname.split('/').pop();
    await admin.setViewportSize({width:360,height:1000});await overflow(admin);assert.equal(await admin.locator('[data-admin-nav]').getAttribute('open'),null);await admin.screenshot({path:path.join(out,'user-admin-mobile.png'),fullPage:true});
    await changeRole(admin,id,'QuanTri','HOAT_DONG','QA cấp quyền cho tài khoản đã xác minh');
    await customer.goto(base+'/tai-khoan');await customer.waitForURL('**/login?expired');await login(customer,email,password);assert.equal((await customer.goto(base+'/quan-tri/nguoi-dung')).status(),200);
    await changeRole(admin,id,'KhachHang','BI_KHOA','QA khóa và thu hồi mọi phiên');await customer.goto(base+'/tai-khoan');await customer.waitForURL('**/login?expired');
    await customer.getByLabel('Email',{exact:true}).fill(email);await customer.getByLabel('Mật khẩu',{exact:true}).fill(password);await customer.getByRole('button',{name:'Đăng nhập',exact:true}).click();await customer.waitForURL('**/login?error');
    await changeRole(admin,id,'KhachHang','HOAT_DONG','QA mở lại tài khoản, giữ quyền khách');await login(customer,email,password);assert.equal((await customer.goto(base+'/quan-tri/nguoi-dung')).status(),403);
    await admin.goto(base+'/quan-tri/nguoi-dung/'+id);await admin.getByRole('button',{name:'Thu hồi mọi phiên của tài khoản',exact:true}).click();await customer.goto(base+'/tai-khoan');await customer.waitForURL('**/login?expired');
    await admin.goto(base+'/quan-tri/nhat-ky/bao-mat');assert.match(await admin.locator('main').innerText(),/THU_HOI_PHIEN/);await overflow(admin);await admin.screenshot({path:path.join(out,'security-audit-mobile.png'),fullPage:true});
    await admin.setViewportSize({width:1440,height:1050});await admin.goto(base+'/quan-tri/nhat-ky/quan-tri');assert.match(await admin.locator('main').innerText(),/QA mở lại tài khoản/);await overflow(admin);await admin.screenshot({path:path.join(out,'admin-audit-desktop.png'),fullPage:true});
    assert.deepEqual(errors,[]);console.log('Account/admin browser QA passed: edit/default saved address; owner-only edit; verified role grant -> old session revoked -> new permission; lock -> login denied; unlock -> customer permission; explicit session revocation; private audit and responsive nav/forms/tables.');
  }finally{
    if(id)try{await changeRole(admin,id,'KhachHang','HOAT_DONG','QA cleanup: tài khoản kiểm thử giữ quyền khách');}catch(e){console.error('Account fixture cleanup needs review:',id,e.message);process.exitCode=1;}
    await browser.close();
  }
})().catch(e=>{console.error(e);process.exitCode=1;});
async function login(p,email,password){await p.goto(base+'/login');await p.getByLabel('Email',{exact:true}).fill(email);await p.getByLabel('Mật khẩu',{exact:true}).fill(password);await p.getByRole('button',{name:'Đăng nhập',exact:true}).click();await p.waitForURL('**/tai-khoan');}
async function changeRole(p,id,role,state,reason){await p.goto(base+'/quan-tri/nguoi-dung/'+id);await p.getByLabel('Vai trò tài khoản',{exact:true}).selectOption(role);await p.getByLabel('Trạng thái tài khoản',{exact:true}).selectOption(state);await p.getByLabel('Lý do thay đổi quyền / trạng thái',{exact:true}).fill(reason);await p.getByRole('button',{name:'Lưu quyền và trạng thái',exact:true}).click();assert.match(await p.locator('main').innerText(),/Đã lưu quyền và trạng thái/);}
async function overflow(p){assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-document.documentElement.clientWidth)<=1,'Page must not overflow');}
async function register(p,email,password){
  await p.goto(base+'/dang-ky');for(const[label,value]of[['Họ tên','Khách QA tài khoản'],['Email',email],['Mật khẩu',password],['Nhập lại mật khẩu',password]])await p.getByLabel(label,{exact:true}).fill(value);await p.getByRole('button',{name:'Tạo tài khoản',exact:true}).click();await p.waitForURL('**/login?registered');
  const mail=path.resolve(__dirname,'../.local/mail');let link;for(const file of await fs.readdir(mail)){const content=await fs.readFile(path.join(mail,file),'utf8');if(content.includes('Đến: '+email+'\n'))link=content.match(/http:\/\/localhost:8082\/xac-minh-email\?token=[A-Za-z0-9_-]+/)?.[0];}assert.ok(link);await p.goto(link.replace('localhost','127.0.0.1'));await p.getByRole('button',{name:'Xác nhận email',exact:true}).click();
}
