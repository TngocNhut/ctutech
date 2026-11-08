const unreadDot = document.querySelector('[data-unread-dot]');
if (unreadDot && document.querySelector(".site-header")?.dataset.authenticated === "true") fetch('/tai-khoan/thong-bao/chua-doc', {headers:{Accept:'application/json'}})
  .then(response => response.ok && !response.redirected && response.headers.get('content-type')?.includes('application/json') ? response.json() : null)
  .then(data => { if (data?.soChuaDoc > 0) unreadDot.hidden = false; }).catch(() => {});
