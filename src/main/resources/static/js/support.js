(()=>{
  const list=document.querySelector('[data-support-poll]');if(!list)return;
  const status=document.querySelector('[data-support-poll-status]'),state=document.querySelector('[data-support-state]');let stopped=false;
  const latest=()=>[...list.querySelectorAll('[data-message-id]')].reduce((max,el)=>{const id=BigInt(el.dataset.messageId);return id>max?id:max;},0n).toString();
  const setStatus=text=>{if(status.textContent!==text)status.textContent=text;};
  function add(t){if(typeof t.ma!=='string'||!/^[1-9][0-9]{0,18}$/.test(t.ma)||list.querySelector(`[data-message-id="${t.ma}"]`))return;const item=document.createElement('li');item.dataset.messageId=String(t.ma);item.className=t.loai==='KHACH_HANG'?'support-customer':'support-answer';const meta=document.createElement('div');meta.className='support-message-meta';const author=document.createElement('strong'),time=document.createElement('time'),text=document.createElement('p');author.textContent=t.loai==='CHATBOT'?'Bot hỗ trợ':t.loai==='NHAN_VIEN'?'Nhân viên':list.dataset.admin==='true'?'Khách hàng':'Bạn';time.textContent=t.lucGui;meta.append(author,time);text.className='support-text';text.textContent=t.noiDung;item.append(meta,text);if(t.nguon){const source=document.createElement('small');source.textContent=`Nguồn đã duyệt: ${t.nguon} · Phiên bản ${t.phienBan}`;item.append(source);}list.append(item);document.querySelector('[data-support-empty]')?.remove();}
  async function poll(){
    if(stopped)return;
    if(!document.hidden){const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),8000);try{
      const response=await fetch(list.dataset.supportPoll+'?sau='+latest(),{headers:{Accept:'application/json'},cache:'no-store',signal:controller.signal});if(!response.ok||!response.headers.get('Content-Type')?.includes('application/json'))throw Error('Unavailable');const result=await response.json();for(const t of result.tin)add(t);setStatus(result.tin.length?`Đã cập nhật ${result.tin.length} tin mới.`:'Tin nhắn được cập nhật tự động khi mở trang.');
      if(result.trangThai!==list.dataset.state||result.cheDo!==list.dataset.mode){state.textContent=result.trangThai==='DA_DONG'?'Cuộc hỗ trợ đã đóng':result.cheDo==='CHO_NHAN_VIEN'?'Đang chờ nhân viên tiếp nhận':result.cheDo==='NHAN_VIEN'?'Nhân viên đang hỗ trợ: '+result.nguoiPhuTrach:'Bot hỗ trợ đang tiếp nhận';document.querySelector('[data-support-state-change]').hidden=false;list.dataset.state=result.trangThai;list.dataset.mode=result.cheDo;}
      if(result.trangThai==='DA_DONG'){stopped=true;document.querySelectorAll('form[data-support-write] button').forEach(b=>b.disabled=true);} 
    }catch{setStatus('Chưa cập nhật được tin. Dùng “Tải tin mới” hoặc chờ lần thử tiếp theo.');}finally{clearTimeout(timeout);}}
    if(!stopped)setTimeout(poll,15000);
  }
  setTimeout(poll,15000);
})();
