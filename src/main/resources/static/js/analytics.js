(()=>{
  const config=document.querySelector('[data-analytics-config]');if(!config||location.pathname.startsWith('/quan-tri'))return;
  const path=location.pathname;let group=path==='/'?'TRANG_CHU':path==='/san-pham'?'DANH_SACH':/^\/san-pham\/[^/]+$/.test(path)?'CHI_TIET':path==='/uu-dai'?'UU_DAI':path.startsWith('/su-kien')?'SU_KIEN':path==='/gio-hang'?'GIO_HANG':path==='/dat-hang'?'DAT_HANG':path.startsWith('/don-hang')?'DON_HANG':path.startsWith('/tai-khoan')?'TAI_KHOAN':null;if(!group)return;
  async function send(route,data){try{await fetch(route,{method:'POST',headers:{'Content-Type':'application/json',[config.dataset.header]:config.dataset.token},body:JSON.stringify(data),credentials:'same-origin',keepalive:true});}catch{/* Analytics never blocks shopping. */}}
  const event=(type,sku)=>send('/thong-ke/su-kien',{loai:type,nhom:group,khoa:crypto.randomUUID(),sku:sku||null});
  const sku=document.querySelector('[data-analytics-sku]')?.dataset.analyticsSku;event('XEM_TRANG',sku);
  if(group==='DANH_SACH'){const query=new URLSearchParams(location.search);if(query.get('q')?.trim())event('TIM_KIEM');if([...query.keys()].some(k=>k!=='q'&&k!=='trang'&&query.get(k)!==''&&query.get(k)!=='false'))event('LOC_DANH_MUC');}
  document.querySelectorAll('[data-analytics-banner]').forEach(a=>a.addEventListener('click',()=>send('/thong-ke/su-kien',{loai:'BAM_BANNER',nhom:group,khoa:crypto.randomUUID(),banner:a.dataset.analyticsBanner})));
  let lastAction=Date.now(),release,attempting=false;
  for(const type of ['pointerdown','keydown','scroll'])addEventListener(type,()=>{lastAction=Date.now();},{passive:true});
  function releaseLeader(){if(release){release();release=null;}}
  function tryLeader(){if(attempting||release||document.hidden||Date.now()-lastAction>60000||!navigator.locks)return;attempting=true;navigator.locks.request('luma-activity-leader',{ifAvailable:true},lock=>{attempting=false;if(!lock)return;return new Promise(resolve=>{const timer=setInterval(()=>{if(document.hidden||Date.now()-lastAction>60000){releaseLeader();return;}send('/thong-ke/nhip',{hoatDong:true});},10000);release=()=>{clearInterval(timer);resolve();};});}).catch(()=>{attempting=false;});}
  setInterval(tryLeader,10000);tryLeader();addEventListener('pagehide',releaseLeader);document.addEventListener('visibilitychange',()=>{if(document.hidden)releaseLeader();else{lastAction=Date.now();tryLeader();}});
})();
