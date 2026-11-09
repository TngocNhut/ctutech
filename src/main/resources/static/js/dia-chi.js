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
const field = name => document.querySelector(`[name="diaChi.${name}"]`) || document.querySelector(`[name="${name}"]`);
document.querySelectorAll('[data-saved-address]').forEach(button => button.addEventListener('click', () => {
  for (const [name, key] of Object.entries({tenNguoiNhan:'name',soDienThoai:'phone',diaChiCuThe:'street',ghiChu:'note'})) {
    const input = field(name);
    if (input) input.value = button.dataset[key] || '';
  }
  setOption(citySelect, button.dataset.city || '');
  populateWards(button.dataset.ward || '');
}));
