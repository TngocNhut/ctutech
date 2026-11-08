'use strict';
const menu=document.querySelector('[data-admin-nav]');
if(menu){const narrow=window.matchMedia('(max-width:850px)');menu.open=!narrow.matches;narrow.addEventListener('change',e=>{menu.open=!e.matches;});}
if(menu)for(const link of menu.querySelectorAll('nav a'))if(window.location.pathname===link.getAttribute('href')||window.location.pathname.startsWith(link.getAttribute('href')+'/'))link.setAttribute('aria-current','page');
