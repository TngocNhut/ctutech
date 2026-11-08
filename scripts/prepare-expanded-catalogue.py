"""Curate 30 phones, 30 laptops and 15 accessories from saved public source evidence.
Prices/stock/warranty are explicitly simulated. No recolouring or fabricated photo variants.
"""
from pathlib import Path
import json,re,html,unicodedata,urllib.request,hashlib,concurrent.futures,time,sys
sys.stdout.reconfigure(encoding='utf8')
ROOT=Path(__file__).resolve().parents[1];CACHE=ROOT/'.local/catalogue-expansion';OUT=ROOT/'src/main/resources/static/media/catalogue';OUT.mkdir(parents=True,exist_ok=True)
seed=json.loads((ROOT/'tai-lieu/05_DuLieuSanPham.json').read_text('utf8'))
phones='''iphone-17-256gb
dien-thoai-samsung-galaxy-s25-ultra
dien-thoai-samsung-galaxy-s26-fe
dien-thoai-samsung-galaxy-z-flip-8
dien-thoai-vivo-y28-8gb-256gb
vivo-y16
vivo-y36
vivo-y17s
dien-thoai-vivo-y03
dien-thoai-vivo-v40-lite
dien-thoai-xiaomi-redmi-note-15
dien-thoai-xiaomi-redmi-note-15-pro
dien-thoai-xiaomi-poco-f9-ultra
dien-thoai-xiaomi-poco-x8-pro-max
dien-thoai-xiaomi-17t
dien-thoai-xiaomi-17-ultra
dien-thoai-oppo-reno12-5g
dien-thoai-oppo-reno15
dien-thoai-oppo-reno16-f
dien-thoai-oppo-find-x8
dien-thoai-oppo-find-x9-pro
dien-thoai-oppo-a6-pro'''.splitlines()
laptops='''macbook-neo-13-a18-pro-6-cpu-5-gpu-8gb-256gb
macbook-air-13-inch-m5-10-cpu-8-gpu-16gb-512gb-2026-sac-70w
macbook-air-15-m5-10-cpu-10-gpu-16gb-512gb
macbook-pro-14-m5-pro-15cpu-16-gpu-24gb-1tb
laptop-dell-pro-15-essential-pv15250-vkvkd
laptop-dell-15-dc15250-1xvhg
laptop-dell-14-dc14250-dc4c5386w
laptop-dell-inspiron-15-3530-j9xfd
laptop-lenovo-ideapad-slim-3-14iph11-83uq003nvn
laptop-lenovo-loq-15irh11e-83y9003vvn
laptop-lenovo-legion-5-15irx10-83ly00hrvn
laptop-lenovo-thinkbook-16-g9-irl-21us008fvn
laptop-asus-zenbook-14-ux3405ca-st1713w
laptop-asus-vivobook-14-x1407ca-ly008w
laptop-asus-gaming-v16-v3607vu-tk416w
laptop-asus-expertbook-p1-p1503cvay-s73484w
laptop-hp-omnibook-5-ai-16-af1048tu-bz7q9pa
laptop-hp-14-em0023au-d0bg7pa
laptop-hp-omnibook-7-14-fr0033tu-c1mn2pa
laptop-hp-15-fd1289tu-c2cv8pa
laptop-hp-250r-g9-ax3c8at
laptop-hp-omnibook-x-flip-14-fm0088tu-bz7q2pa'''.splitlines()
accessories=[('apple-airpods-pro-2-usb-c','Tai nghe in-ear','Apple'),('tai-nghe-sony-wf-c700','Tai nghe in-ear','Sony'),('tai-nghe-chup-tai-sony-wh-1000xm5','Tai nghe headphone','Sony'),('cap-type-c-to-type-c-mophie-100w-3m','Cáp sạc','Mophie'),('cap-type-c-to-lightning-baseus-dura-series-20w-1m','Cáp sạc','Baseus'),('cu-sac-anker-nano-a2692-1c-45w','Củ sạc','Anker'),('cu-sac-nhanh-iphone-20w-pd-type-c','Củ sạc','Apple'),('cong-chuyen-doi-ugreen-usb-a-to-type-c-us276-50533','Đầu chuyển USB-A sang C','Ugreen'),('hub-chuyen-doi-ugreen-usb-c-to-usb-a-2-0-usb-a-3-0-hdmi-pd-ho-tro-4k-15495','Đầu chuyển USB-C sang HDMI','Ugreen'),('ban-phim-bluetooth-logitech-pebble-k380s','Bàn phím','Logitech'),('ban-phim-apple-magic-keyboard-touch-id-2024-mxck3','Bàn phím','Apple'),('ban-phim-co-khong-day-aula-f75-den','Bàn phím','AULA'),('chuot-khong-day-logitech-signature-m650','Chuột','Logitech'),('chuot-khong-day-logitech-m331','Chuột','Logitech'),('chuot-gaming-khong-day-logitech-g304-lightspeed','Chuột','Logitech')]
def clean(s):return re.sub(r'\s+',' ',html.unescape(re.sub('<[^>]+>',' ',str(s or '')))).strip()
def slug(s):return re.sub('[^a-z0-9]+','-',unicodedata.normalize('NFD',s).encode('ascii','ignore').decode().lower()).strip('-')
def number(s,default=0):
 m=re.search(r'(\d+(?:[.,]\d+)?)',clean(s));return float(m.group(1).replace(',','.')) if m else default

def line(name,brand,kind,accessory):
 if accessory:return accessory
 if brand=='vivo':
  match=re.search(r'\b([VYX])\d',name,re.I)
  if match:return match.group(1).upper()+' Series'
 if brand=='Apple' and kind=='dien-thoai':
  match=re.search(r'iPhone (\d+)',name,re.I)
  if match:return 'iPhone '+match.group(1)+' Series'
 patterns={'Apple':['MacBook Pro','MacBook Air','MacBook Neo','iPhone'],'Samsung':['Galaxy Z Flip','Galaxy Z Fold','Galaxy S','Galaxy A'],'vivo':['V','Y'],'Xiaomi':['POCO F','POCO X','Redmi Note','Redmi A','Xiaomi'],'OPPO':['Find X','Find N','Reno','A'],'Dell':['Inspiron','Vostro','Pro','Dell 14','Dell 15'],'Lenovo':['IdeaPad','Ideapad','LOQ','Legion','ThinkBook','ThinkPad'],'HP':['OmniBook X Flip','Omnibook X Flip','Omnibook','OmniBook','250R','HP 14','HP 15'],'ASUS':['Zenbook','Vivobook','VivoBook','TUF','Gaming V','ExpertBook']}
 for term in patterns.get(brand,[]):
  if re.search(r'\b'+re.escape(term),name,re.I):return {'Ideapad':'IdeaPad','VivoBook':'Vivobook','Omnibook':'OmniBook','Omnibook X Flip':'OmniBook X Flip'}.get(term,term)
 return brand

queue=[(s['DuongDanNguon'].split('/')[-1].replace('.html',''),'dien-thoai' if s['MaPhanLoai']==1 else 'laptop',s['MaSanPham'],None,None) for s in seed['SanPham']]+[(s,'dien-thoai',None,None,None) for s in phones]+[(s,'laptop',None,None,None) for s in laptops]+[(s,'phu-kien',None,t,b) for s,t,b in accessories]
models=[];jobs={}
def image(source):
 if not source or source=='no_selection':return None
 url=source if source.startswith('http') else 'https://cdn2.cellphones.com.vn/600x/media/catalog/product'+source
 url=re.sub(r'/\d+x/','/600x/',url)
 key=hashlib.sha256(url.encode()).hexdigest()[:20];ext=Path(url.split('?')[0]).suffix.lower();ext=ext if ext in ['.jpg','.jpeg','.png','.webp'] else '.jpg';file=key+ext;jobs[file]=url;return '/media/catalogue/'+file
for source_slug,kind,existing,accessory,brand_override in queue:
 raw=json.loads((CACHE/(source_slug+'.json')).read_text('utf8'));p=raw['product'];g=p['general'];a=g['attributes'];name=clean(g['name']);name=re.split(r'\s*[|｜]\s*|\s+-\s*(?:Nhập khẩu|Bảo hành)',name)[0].strip();name=re.sub(r'^Điện thoại\s+','',name,flags=re.I)
 brand=brand_override or next((b for b in ['Apple','Samsung','vivo','Xiaomi','OPPO','Dell','Lenovo','HP','ASUS'] if re.search(r'\b'+re.escape(b)+r'\b',name,re.I)),None)
 if brand is None and name.lower().startswith(('iphone','macbook')):brand='Apple'
 if brand is None and 'xiaomi' in source_slug:brand='Xiaomi'
 assert brand,(source_slug,name)
 if existing:name=next(s['TenSanPham'] for s in seed['SanPham'] if s['MaSanPham']==existing)
 model={'slug':next(s['DuongDan'] for s in seed['SanPham'] if s['MaSanPham']==existing) if existing else source_slug,'existingId':existing,'name':name,'category':kind,'brand':brand,'brandSlug':slug(brand),'line':line(name,brand,kind,accessory),'source':raw['url'],'accessoryType':accessory,'variants':[],'gallery':[]}
 spec={i['key']:clean(i.get('value')) for group in p.get('specification',{}).values() if isinstance(group,list) for i in group if isinstance(i,dict) and i.get('key')}
 properties={**a,**spec}
 for v in raw['variants'] or [p]:
  va={**properties,**v['general'].get('attributes',{})};colour=clean(va.get('color')) or 'Phiên bản tiêu chuẩn';ram=int(number(va.get('laptop_ram') or va.get('mobile_ram') or va.get('ram')));storage_text=va.get('o_cung_laptop') or va.get('memory') or va.get('mobile_rom') or va.get('internal_memory') or '';storage=int(number(storage_text));storage*=1024 if re.search(r'\bTB\b',str(storage_text),re.I) else 1
  if not ram and kind!='phu-kien':ram=int(number(next((i.get('value') for sch in raw.get('schema',[]) if isinstance(sch,dict) and sch.get('@type')=='Product' for i in sch.get('additionalProperty',[]) if i.get('name')=='Dung lượng RAM'),0)))
  if not storage and kind!='phu-kien':storage=int(number(next((i.get('value') for sch in raw.get('schema',[]) if isinstance(sch,dict) and sch.get('@type')=='Product' for i in sch.get('additionalProperty',[]) if i.get('name') in ['Bộ nhớ trong','Ổ cứng']),0)))
  if kind=='phu-kien':ram=storage=0
  chip=clean(va.get('chipset') or va.get('cpu'))[:120] or ('Không áp dụng' if kind=='phu-kien' else 'Xem nguồn thông số')
  model['variants'].append({'colour':colour,'ram':ram,'storage':storage,'cpu':chip,'gpu':clean(va.get('vga') or va.get('gpu'))[:120],'screen':number(va.get('display_size')) if kind!='phu-kien' else 0,'os':clean(va.get('os_version') or va.get('os'))[:100] or ('Không áp dụng' if kind=='phu-kien' else 'Xem nguồn thông số'),'image':image(v.get('filterable',{}).get('thumbnail') or va.get('image') or a.get('image'))})
 for source in g.get('galleries',[])[:3]:
  target=image(source)
  if target and target not in model['gallery']:model['gallery'].append(target)
 if not model['gallery']:model['gallery']=[model['variants'][0]['image']]
 if accessory:model['summary']=f'{name}. {accessory}; chọn phiên bản và màu theo ảnh.'
 else:
  first=model['variants'][0];model['summary']=f"{name}. {first['cpu']}; RAM {first['ram']} GB, bộ nhớ {first['storage']} GB. Chọn cấu hình và màu theo nhu cầu."
 model['summary']=model['summary'].replace('RAM 0 GB','RAM chưa được nguồn công bố')[:500];models.append(model)
assert len(models)==75 and sum(m['category']=='dien-thoai' for m in models)==30 and sum(m['category']=='laptop' for m in models)==30

def download(item):
 file,url=item;dest=OUT/file
 if dest.exists() and dest.stat().st_size>1000:return file
 for attempt in range(3):
  try:
   data=urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'}),timeout=35).read()
   from PIL import Image
   import io
   img=Image.open(io.BytesIO(data));img.verify();assert len(data)>1000;dest.write_bytes(data);return file
  except Exception:
   if attempt==2:raise
   time.sleep(1+attempt)
print('Curated',len(models),'models;',sum(len(m['variants']) for m in models),'source variants;',len(jobs),'images',flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
 for n,result in enumerate(pool.map(download,jobs.items()),1):
  if n%25==0:print('Downloaded',n,flush=True)
assets=[{'file':'/media/catalogue/'+f,'source':u,'bytes':(OUT/f).stat().st_size,'sha256':hashlib.sha256((OUT/f).read_bytes()).hexdigest()} for f,u in jobs.items()]
manifest={'collected':'2026-11-08','usage':'Ảnh sản phẩm tham khảo công khai, chỉ phục vụ đồ án localhost. Giữ nguồn, không khẳng định giấy phép tái phân phối thương mại. Không tự tô màu ảnh. Giá, vốn, tồn và bảo hành là mô phỏng.','models':models,'assets':assets}
(ROOT/'tai-lieu/31_DanhMucMoRong.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8');print('Manifest written: 30 phones / 30 laptops / 15 accessories',flush=True)
