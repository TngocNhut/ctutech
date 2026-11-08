'use strict';
const filters=document.querySelector('[data-catalog-filter]');
if(filters&&window.matchMedia('(max-width:850px)').matches&&filters.dataset.active!=='true')filters.open=false;
const main=document.querySelector('[data-gallery-main]');
if(main)for(const link of document.querySelectorAll('[data-gallery-link]'))link.addEventListener('click',event=>{
  event.preventDefault();main.src=link.href;main.alt=link.querySelector('img').alt;
  const source=document.querySelector('[data-gallery-source]');if(source){source.hidden=!link.dataset.source;if(link.dataset.source)source.href=link.dataset.source;}
  for(const item of document.querySelectorAll('[data-gallery-link]'))item.removeAttribute('aria-current');link.setAttribute('aria-current','true');
});

const brandFilter=document.querySelector('#filter-brand'),lineFilter=document.querySelector('#filter-line');
function updateLines(){if(!brandFilter||!lineFilter)return;for(const option of lineFilter.options){const excluded=!!option.value&&!!brandFilter.value&&option.dataset.brand!==brandFilter.value;option.hidden=excluded;option.disabled=excluded;if(excluded&&option.selected)lineFilter.value='';}}
brandFilter?.addEventListener('change',updateLines);updateLines();
