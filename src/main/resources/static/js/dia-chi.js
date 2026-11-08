const citySelect = document.getElementById('address-city');
const wardSelect = document.getElementById('address-ward');
let provinces = [];
const norm = value => value.toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/^(thanh pho|tinh|phuong|xa|dac khu)\s+/,'').trim();
function setOption(select, value) {
  if (!value) { select.value = ''; return; }
  const found = [...select.options].find(o => norm(o.value) === norm(value));
  if (found) select.value = found.value;
  else { const option = new Option(value, value); option.dataset.legacy = 'true'; select.add(option); select.value = value; }
}
function populateWards(value = '') {
  wardSelect.replaceChildren(new Option('Chọn phường / xã', ''));
  const province = provinces.find(p => norm(p.name) === norm(citySelect.value));
  for (const ward of province?.wards || []) wardSelect.add(new Option(ward, ward));
  setOption(wardSelect, value);
}
async function loadDivisions() {
  if (!citySelect || !wardSelect) return;
  const city = citySelect.value, ward = wardSelect.value;
  try {
    const response = await fetch('/media/vietnam-addresses.json');
    if (!response.ok) throw new Error('unavailable');
    provinces = (await response.json()).provinces;
    citySelect.replaceChildren(new Option('Chọn tỉnh / thành phố', ''));
    for (const province of provinces) citySelect.add(new Option(province.name, province.name));
    setOption(citySelect, city); populateWards(ward);
    citySelect.addEventListener('change', () => populateWards());
  } catch { citySelect.insertAdjacentHTML('afterend', '<p role="alert">Không tải được danh sách địa chỉ. Hãy tải lại trang.</p>'); }
}
await loadDivisions();
const root = document.querySelector('.map-picker');
if (root) {
  const status = root.querySelector('[data-map-status]');
  const lat = root.querySelector('[data-lat]');
  const lng = root.querySelector('[data-lng]');
  const placeId = root.querySelector('[data-place-id]');
  const confirm = root.querySelector('[data-pin-confirm]');
  let map, marker;
  const show = message => { status.textContent = message; };
  const clear = () => { lat.value = ''; lng.value = ''; placeId.value = ''; confirm.checked = false; show('Đang dùng địa chỉ nhập tay.'); };
  root.querySelector('[data-map-clear]').addEventListener('click', clear);
  const field = name => document.querySelector(`[name="diaChi.${name}"]`) || document.querySelector(`[name="${name}"]`);
  for (const name of ['tinhThanh', 'phuongXa', 'diaChiCuThe']) field(name)?.addEventListener('input', () => { confirm.checked = false; });
  document.querySelectorAll('[data-saved-address]').forEach(button => button.addEventListener('click', () => {
    for (const [name, key] of Object.entries({tenNguoiNhan:'name',soDienThoai:'phone',diaChiCuThe:'street',ghiChu:'note'})) field(name).value = button.dataset[key] || '';
    setOption(citySelect, button.dataset.city || ''); populateWards(button.dataset.ward || '');
    clear();
  }));
  const setPin = point => {
    lat.value = point.lat().toFixed(7); lng.value = point.lng().toFixed(7); placeId.value = ''; confirm.checked = false;
    if (marker) marker.position = point;
    show('Hãy kiểm tra pin và xác nhận điểm giao nhận. Địa chỉ nhập tay vẫn cần đầy đủ.');
  };
  root.querySelector('[data-map-open]').addEventListener('click', async event => {
    if (!root.dataset.mapKey) { show('Bản đồ hiện chưa sẵn sàng. Bạn vẫn có thể nhập đầy đủ địa chỉ để tiếp tục.'); return; }
    const button = event.currentTarget;
    button.disabled = true; show('Đang tải Google Maps…');
    try {
      if (!window.google?.maps?.importLibrary) await new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('timeout')), 15000);
        window.lumaMapsReady = () => { clearTimeout(timer); resolve(); };
        window.gm_authFailure = () => { clearTimeout(timer); button.disabled = false; root.querySelector('[data-map-panel]').hidden = true; clear(); show('Không tải được bản đồ. Hãy kiểm tra địa chỉ nhập tay và tiếp tục.'); reject(new Error('auth')); };
        const script = document.createElement('script'); script.async = true; script.referrerPolicy = 'strict-origin-when-cross-origin';
        script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(root.dataset.mapKey)}&loading=async&callback=lumaMapsReady&language=vi&region=VN&v=weekly`;
        script.onerror = () => { clearTimeout(timer); reject(new Error('network')); }; document.head.appendChild(script);
      });
      root.querySelector('[data-map-panel]').hidden = false;
      const { Map } = await google.maps.importLibrary('maps');
      const { AdvancedMarkerElement } = await google.maps.importLibrary('marker');
      const { PlaceAutocompleteElement } = await google.maps.importLibrary('places');
      map = new Map(root.querySelector('[data-map-canvas]'), {center: {lat:10.7769,lng:106.7009},zoom:12,mapId:root.dataset.mapId || 'DEMO_MAP_ID'});
      marker = new AdvancedMarkerElement({map,gmpDraggable:true,title:'Điểm giao nhận'});
      map.addListener('click', e => { setPin(e.latLng); });
      marker.addListener('dragend', () => { const point = marker.position; if (point) setPin(new google.maps.LatLng(typeof point.lat === 'function' ? point.lat() : point.lat, typeof point.lng === 'function' ? point.lng() : point.lng)); });
      const search = new PlaceAutocompleteElement({includedRegionCodes:['vn']}); search.setAttribute('aria-label','Tìm địa chỉ tại Việt Nam'); root.querySelector('[data-place-search]').append(search);
      search.addEventListener('gmp-select', async ({placePrediction}) => {
        try {
          const place = placePrediction.toPlace(); await place.fetchFields({fields:['id','formattedAddress','location','addressComponents']});
          const country = place.addressComponents?.find(c => c.types.includes('country'));
          if (country?.shortText !== 'VN' || !place.location) { clear(); show('Hãy chọn địa chỉ tại Việt Nam.'); return; }
          map.setCenter(place.location); map.setZoom(17); setPin(place.location); placeId.value = place.id || '';
          const province = place.addressComponents.find(c => c.types.includes('administrative_area_level_1'));
          const ward = place.addressComponents.find(c => c.types.includes('sublocality_level_1') || c.types.includes('administrative_area_level_3'));
          if (province) { setOption(citySelect, province.longText); populateWards(); }
          if (ward) setOption(wardSelect, ward.longText);
          show(`Đã chọn: ${place.formattedAddress || ''}. Bổ sung số nhà/căn hộ, kiểm tra pin rồi xác nhận.`);
        } catch { clear(); show('Không lấy được thông tin địa điểm. Hãy thử lại hoặc nhập tay.'); }
      });
      show('Tìm địa chỉ hoặc bấm bản đồ để chọn điểm giao nhận.');
    } catch { button.disabled = false; show('Không tải được bản đồ. Bạn có thể nhập địa chỉ tay hoặc thử lại.'); }
  });
  root.querySelector('[data-map-location]').addEventListener('click', () => {
    if (!map || !window.google?.maps?.LatLng) { show('Hãy mở bản đồ trước hoặc nhập địa chỉ tay.'); return; }
    if (!navigator.geolocation) { show('Thiết bị không hỗ trợ vị trí. Bạn có thể tìm địa chỉ hoặc nhập tay.'); return; }
    navigator.geolocation.getCurrentPosition(p => { const point = new google.maps.LatLng(p.coords.latitude,p.coords.longitude); map.setCenter(point); map.setZoom(17); setPin(point); }, () => show('Chưa lấy được vị trí. Bạn có thể tìm trên bản đồ hoặc nhập tay.'), {timeout:10000,maximumAge:0});
  });
}
