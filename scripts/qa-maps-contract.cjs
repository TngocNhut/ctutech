const path=require('node:path');const assert=require('node:assert/strict');
const {chromium}=require(path.resolve(__dirname,'../.local/browser/node_modules/playwright'));
const base='http://127.0.0.1:8082';
const sdk=`(()=>{
  const qa=window.__qaMaps={};
  class LatLng{constructor(lat,lng){this.latitude=Number(lat);this.longitude=Number(lng);}lat(){return this.latitude;}lng(){return this.longitude;}}
  class Map{constructor(node,options){this.listeners={};this.options=options;qa.map=this;}addListener(name,fn){this.listeners[name]=fn;}setCenter(point){this.center=point;}setZoom(zoom){this.zoom=zoom;}}
  class Marker{constructor(options){this.listeners={};qa.marker=this;}addListener(name,fn){this.listeners[name]=fn;}}
  class Place extends HTMLElement{constructor(options){super();qa.widget=this;qa.region=options.includedRegionCodes;this.textContent='Địa chỉ giả lập kiểm thử';}}
  customElements.define('qa-place-autocomplete',Place);
  window.google={maps:{LatLng,importLibrary:async(name)=>name==='maps'?{Map}:name==='marker'?{AdvancedMarkerElement:Marker}:{PlaceAutocompleteElement:Place}}};
  window.lumaMapsReady();
})();`;
(async()=>{
  const browser=await chromium.launch({channel:'msedge',headless:true});const context=await browser.newContext();const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  try{
    await page.goto(base+'/login');await page.getByLabel('Email',{exact:true}).fill('khach@luma.local');await page.getByLabel('Mật khẩu',{exact:true}).fill('LumaDemo!2026');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();await page.waitForURL('**/tai-khoan');
    let referer;await page.route('https://maps.googleapis.com/maps/api/js?*',async route=>{referer=route.request().headers().referer;await route.fulfill({contentType:'application/javascript',body:sdk});});
    await open(page);await page.waitForFunction(()=>Boolean(window.__qaMaps?.widget));assert.equal(referer,base+'/');assert.deepEqual(await page.evaluate(()=>window.__qaMaps.region),['vn']);
    await page.evaluate(()=>window.__qaMaps.map.listeners.click({latLng:new google.maps.LatLng(10.77,106.70)}));assert.equal(await page.locator('[data-lat]').inputValue(),'10.7700000');assert.equal(await page.locator('[data-lng]').inputValue(),'106.7000000');assert.equal(await page.locator('[data-pin-confirm]').isChecked(),false);
    await page.locator('[data-pin-confirm]').check();await page.getByLabel('Tỉnh / Thành phố',{exact:true}).fill('Thành phố Hồ Chí Minh');assert.equal(await page.locator('[data-pin-confirm]').isChecked(),false);
    await page.evaluate(()=>{const marker=window.__qaMaps.marker;marker.position={lat:10.8,lng:106.8};marker.listeners.dragend();});assert.equal(await page.locator('[data-lat]').inputValue(),'10.8000000');
    await page.evaluate(()=>{const marker=window.__qaMaps.marker;marker.position=new google.maps.LatLng(10.81,106.81);marker.listeners.dragend();});assert.equal(await page.locator('[data-lat]').inputValue(),'10.8100000');
    await selectPlace(page,'VN');await page.waitForFunction(()=>document.querySelector('[data-place-id]').value==='verified-qa-place');assert.equal(await page.getByLabel('Phường / Xã',{exact:true}).inputValue(),'Phường Bến Nghé');assert.equal(await page.locator('[data-pin-confirm]').isChecked(),false);
    await selectPlace(page,'US');await page.waitForFunction(()=>document.querySelector('[data-map-status]').textContent.includes('Việt Nam'));assert.equal(await page.locator('[data-lat]').inputValue(),'');
    await page.evaluate(()=>Object.defineProperty(navigator,'geolocation',{configurable:true,value:{getCurrentPosition:(ok,fail)=>fail({code:1})}}));await page.getByRole('button',{name:'Dùng vị trí của tôi',exact:true}).click();assert.match(await page.locator('[data-map-status]').innerText(),/Chưa lấy được vị trí/);
    for(const mode of ['network','auth']){
      const failed=await context.newPage();failed.on('pageerror',e=>errors.push(e.message));await failed.route('https://maps.googleapis.com/maps/api/js?*',route=>mode==='network'?route.abort():route.fulfill({contentType:'application/javascript',body:'window.gm_authFailure();'}));
      await open(failed);await failed.waitForFunction(()=>document.querySelector('[data-map-status]').textContent.includes('nhập địa chỉ tay'));assert.equal(await failed.locator('[data-map-open]').isEnabled(),true);await failed.close();
    }
    assert.deepEqual(errors,[]);console.log('Maps browser contract QA passed: origin-only referrer, VN widget, select/click/drag literal+LatLng, pin re-confirmation, outside-country rejection, denied GPS, SDK network/auth fallback. Requests were intercepted; no Google API transaction.');
  }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
async function open(page){await page.goto(base+'/tai-khoan/dia-chi?token=qa-token-must-not-leak');await page.locator('.map-picker').evaluate(el=>el.dataset.mapKey='qa-fake-key');await page.getByRole('button',{name:'Chọn thêm điểm trên Google Maps',exact:true}).click();}
async function selectPlace(page,country){await page.evaluate(country=>{const event=new Event('gmp-select');Object.defineProperty(event,'placePrediction',{value:{toPlace:()=>({id:'verified-qa-place',location:new google.maps.LatLng(10.772,106.704),formattedAddress:'Địa chỉ QA',addressComponents:[{types:['country'],shortText:country},{types:['administrative_area_level_1'],longText:'Thành phố Hồ Chí Minh'},{types:['sublocality_level_1'],longText:'Phường Bến Nghé'}],fetchFields:async()=>{}})}});window.__qaMaps.widget.dispatchEvent(event);},country);}
