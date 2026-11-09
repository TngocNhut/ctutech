const form = document.querySelector('.search');
const input = document.getElementById('search-q');
const panel = document.getElementById('search-suggestions');
if (form && input && panel) {
  let timer, request;
  const hide = () => { panel.hidden = true; input.setAttribute('aria-expanded', 'false'); };
  async function update() {
    request?.abort();
    request = new AbortController();
    try {
      const response = await fetch('/api/tim-kiem/goi-y?q=' + encodeURIComponent(input.value.trim()), {signal: request.signal});
      if (!response.ok) throw new Error('search unavailable');
      const items = await response.json();
      if (document.activeElement !== input) return;
      panel.replaceChildren();
      const heading = document.createElement('p');
      heading.className = 'search-suggestions-heading';
      heading.textContent = input.value.trim() ? 'Sản phẩm phù hợp' : 'Gợi ý dành cho bạn';
      panel.append(heading);
      if (!items.length) {
        const empty = document.createElement('p'); empty.className = 'search-suggestions-empty'; empty.textContent = 'Không tìm thấy sản phẩm phù hợp.'; panel.append(empty);
      }
      for (const item of items) {
        const link = document.createElement('a'); link.className = 'search-suggestion'; link.href = '/san-pham/' + encodeURIComponent(item.slug);
        if (item.image && item.image.startsWith('/')) { const img = document.createElement('img'); img.src = item.image; img.alt = ''; img.width = 54; img.height = 54; link.append(img); }
        const name = document.createElement('span'); name.textContent = item.name; link.append(name); panel.append(link);
      }
      panel.hidden = false; input.setAttribute('aria-expanded', 'true');
    } catch (error) { if (error.name !== 'AbortError') hide(); }
  }
  input.addEventListener('focus', update);
  input.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(update, 180); });
  document.addEventListener('click', e => { if (!form.contains(e.target)) hide(); });
  input.addEventListener('keydown', e => { if (e.key === 'Escape') hide(); if (e.key === 'ArrowDown' && !panel.hidden) { e.preventDefault(); panel.querySelector('a')?.focus(); } });
  form.addEventListener('focusout', e => { if (!form.contains(e.relatedTarget)) hide(); });
}
