document.querySelectorAll('[data-device-model]').forEach(root=>{
  const button=root.querySelector('[data-model-open]'),stage=root.querySelector('[data-model-stage]'),status=root.querySelector('[data-model-status]'),reset=root.querySelector('[data-model-reset]');let viewer;
  button.addEventListener('click',async()=>{
    if(viewer){stage.hidden=!stage.hidden;button.setAttribute('aria-expanded',String(!stage.hidden));button.textContent=stage.hidden?'Xem mô hình minh họa 3D':'Ẩn mô hình 3D';return;}
    button.disabled=true;status.textContent='Đang tải mô hình 3D…';stage.hidden=false;stage.setAttribute('aria-busy','true');let timeout;
    try{
      await Promise.race([import('/vendor/model-viewer-4.4.0.min.js'),new Promise((_,reject)=>{timeout=setTimeout(()=>reject(Error('Viewer load timeout')),10000);})]);clearTimeout(timeout);const model=document.createElement('model-viewer');model.setAttribute('alt',root.dataset.alt);model.setAttribute('camera-controls','');model.setAttribute('touch-action','pan-y');model.setAttribute('interaction-prompt','none');model.setAttribute('camera-orbit','25deg 72deg auto');model.setAttribute('shadow-intensity','1');
      await new Promise((resolve,reject)=>{model.addEventListener('load',resolve,{once:true});model.addEventListener('error',reject,{once:true});timeout=setTimeout(()=>reject(Error('Model load timeout')),20000);model.src=root.dataset.deviceModel;stage.append(model);});viewer=model;status.textContent='Kéo để xoay, cuộn hoặc dùng hai ngón để phóng to. Khi khung được chọn, có thể dùng phím mũi tên.';reset.hidden=false;button.textContent='Ẩn mô hình 3D';button.setAttribute('aria-expanded','true');
    }catch{stage.replaceChildren();stage.hidden=true;status.textContent='Chưa mở được 3D. Bạn vẫn có thể xem ảnh và thông số; thử lại nếu muốn.';button.textContent='Thử mở lại mô hình 3D';}finally{clearTimeout(timeout);stage.removeAttribute('aria-busy');button.disabled=false;}
  });
  reset.addEventListener('click',()=>{if(viewer){viewer.cameraOrbit='25deg 72deg auto';viewer.cameraTarget='auto auto auto';viewer.jumpCameraToGoal();}});
});
