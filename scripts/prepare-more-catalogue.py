"""Select 75 extra models per category from captured source pages and download attributed images."""
from pathlib import Path
import json,re,unicodedata,sys
root=Path(__file__).resolve().parents[1]
source=(root/'scripts/prepare-expanded-catalogue.py').read_text('utf8')
namespace={'__file__':str(root/'scripts/prepare-expanded-catalogue.py')}
exec(source[:source.index('queue=[')],namespace)
exec(source[source.index('def image(source):'):source.index('for source_slug,kind,existing,accessory,brand_override in queue:')],namespace)
base_line=namespace['line']
def source_line(name,brand,kind,accessory):
 if not accessory and brand=='Tecno':
  match=re.search(r'\b(Camon|Pova|Spark|Pop|Phantom)\b',name,re.I)
  if match:return match.group(1).title()
 if not accessory and brand=='HONOR':return 'Magic' if re.search(r'\bMagic\b',name,re.I) else ('X Series' if re.search(r'\bX\d',name,re.I) else ('Play' if re.search(r'\bPlay\b',name,re.I) else 'Number Series'))
 if not accessory and brand=='Realme':return 'GT' if re.search(r'\bGT\b',name,re.I) else ('C Series' if re.search(r'\bC\d',name,re.I) else 'Number Series')
 if not accessory and brand=='Acer':
  match=re.search(r'\b(Predator|Nitro|Aspire|Swift)\b',name,re.I)
  if match:return match.group(1).title()
 return base_line(name,brand,kind,accessory)
namespace['line']=source_line
clean,slug=namespace['clean'],namespace['slug'];cache=namespace['CACHE'];original=json.loads((root/'tai-lieu/31_DanhMucMoRong.json').read_text('utf8'))['models'];candidates=json.loads((cache/'candidates-more.json').read_text('utf8'))
def key(text):
 text=clean(text).lower().replace('đ','d');text=unicodedata.normalize('NFD',text).encode('ascii','ignore').decode();text=re.sub(r'^(dien thoai|laptop)\s+','',text);text=re.sub(r'\b\d+\s*(?:gb|tb)\b','',text);text=re.sub(r'\s*[|｜].*','',text);text=re.sub(r'\s+-\s*(?:nhap khau|bao hanh).*','',text);return re.sub('[^a-z0-9]+','',text)
def classify(name):
 n=name.lower()
 if 'tai nghe' in n:return 'Tai nghe headphone' if any(x in n for x in ['chụp tai','chup tai','headphone','over-ear']) else 'Tai nghe in-ear'
 if 'bàn phím' in n or 'ban phim' in n:return 'Bàn phím'
 if 'chuột' in n or 'chuot' in n:return 'Chuột'
 if 'dự phòng' in n:return 'Pin sạc dự phòng'
 if 'cáp' in n or 'cap ' in n:return 'Cáp sạc'
 if 'hub' in n or 'chuyển đổi' in n or 'chuyen doi' in n:return 'Đầu chuyển'
 if 'sạc' in n or 'sac ' in n:return 'Củ sạc'
 return None
seen={key(x['name']) for x in original};models=[];queue=[];groups={'dien-thoai':0,'laptop':0,'phu-kien':0}
for c in candidates:
 kind=c['category']
 if groups[kind]>=100:continue
 file=cache/(c['url'].split('/')[-1].replace('.html','')+'.json')
 if not file.exists():continue
 raw=json.loads(file.read_text('utf8'));g=raw.get('product',{}).get('general',{});name=clean(g.get('name'));k=key(name)
 if not name or not k or k in seen:continue
 if kind=='laptop' and not re.search('laptop|macbook',name,re.I):continue
 typ=classify(name) if kind=='phu-kien' else None
 if kind=='phu-kien' and not typ:continue
 brand=next((x.get('brand',{}).get('name') for x in raw.get('schema',[]) if isinstance(x,dict) and x.get('@type')=='Product' and isinstance(x.get('brand'),dict)),None) or g.get('manufacturer')
 brand=re.sub(r'\s+Chính hãng$','',clean(brand),flags=re.I)
 if brand.lower()=='vivo':brand='vivo'
 if not brand or brand.lower() in ('khác','other','khong'):continue
 if kind!='phu-kien' and not (raw.get('variants') or g.get('attributes',{}).get('image')):continue
 queue.append((file.stem,kind,None,typ,brand));seen.add(k);groups[kind]+=1
if any(v<70 for v in groups.values()):raise SystemExit('Not enough distinct sourced models: '+repr(groups))
print('Selected',groups,flush=True)
namespace.update({'models':models,'queue':queue,'jobs':{}})
body=source[source.index('for source_slug,kind,existing,accessory,brand_override in queue:'):source.index('assert len(models)==75')]
exec(body,namespace)
models=namespace['models'];jobs=namespace['jobs']
# Recover only facts present in the product metadata or its title. Do not guess missing specifications.
def quantities(value):
 return [int(float(n.replace(',','.'))*(1024 if unit.lower()=='tb' else 1)) for n,unit in re.findall(r'(\d+(?:[.,]\d+)?)\s*(GB|TB)\b',str(value or ''),re.I)]
for model in models:
 model['name']=model['name'][:200];model['line']=model['line'][:120]
 if model['category']=='phu-kien':continue
 raw=json.loads((cache/(model['slug']+'.json')).read_text('utf8'));base=raw['product']['general']['attributes'];spec={i['key']:clean(i.get('value')) for group in raw['product'].get('specification',{}).values() if isinstance(group,list) for i in group if isinstance(i,dict) and i.get('key')}
 named=quantities(model['name']);storage_from_source=quantities(base.get('o_cung_laptop') or spec.get('o_cung_laptop') or base.get('memory') or base.get('mobile_rom') or base.get('internal_memory'))
 source_screen=namespace['number'](base.get('display_size') or spec.get('display_size'))
 for variant in model['variants']:
  variant['colour']=variant['colour'][:60]
  if variant['storage']>65536 or not variant['storage']:variant['storage']=(storage_from_source or ([max(named)] if named else [0]))[0]
  if not variant['screen']:variant['screen']=source_screen
  if not variant['ram'] and len(named)>=2:variant['ram']=min(named)
 model['summary']=f"{model['name']}. {model['variants'][0]['cpu']}; RAM "+(str(model['variants'][0]['ram'])+' GB' if model['variants'][0]['ram'] else 'chưa công bố')+', bộ nhớ '+(str(model['variants'][0]['storage'])+' GB' if model['variants'][0]['storage'] else 'chưa công bố')+'. Chọn cấu hình và màu theo nhu cầu.'
models=[m for m in models if m['category']=='phu-kien' or all(0<v['storage']<=65536 and 0<v['screen']<=99.9 for v in m['variants'])]
chosen=[];per_kind={k:0 for k in groups}
for model in models:
 if per_kind[model['category']]<75:chosen.append(model);per_kind[model['category']]+=1
models=chosen;groups=per_kind
if any(v<70 for v in groups.values()):raise SystemExit('Not enough models with sourced specifications: '+repr(groups))
needed={v['image'].split('/')[-1] for m in models for v in m['variants'] if v['image']}|{p.split('/')[-1] for m in models for p in m['gallery'] if p}
jobs={f:u for f,u in jobs.items() if f in needed}
exec(source[source.index('def download(item):'):source.index("print('Curated'")],namespace)
from concurrent.futures import ThreadPoolExecutor
def safe_download(item):
 try:namespace['download'](item);return item[0],None
 except Exception as ex:return item[0],str(ex)
bad=set()
with ThreadPoolExecutor(max_workers=5) as pool:
 for i,(file,error) in enumerate(pool.map(safe_download,jobs.items()),1):
  if error:bad.add(file);print('Image unavailable:',file,error,flush=True)
  if i%50==0:print('Images checked',i,'/',len(jobs),flush=True)
for model in models:
 model['variants']=[v for v in model['variants'] if v['image'] and v['image'].split('/')[-1] not in bad]
 model['gallery']=[v for v in model['gallery'] if v and v.split('/')[-1] not in bad]
 if not model['gallery'] and model['variants']:model['gallery']=[model['variants'][0]['image']]
models=[m for m in models if m['variants'] and m['gallery']]
groups={kind:sum(m['category']==kind for m in models) for kind in groups}
if any(v<70 for v in groups.values()):raise SystemExit('Fewer than 70 valid-image models remain: '+repr(groups))
print('Valid image models',groups,flush=True)
jobs={f:u for f,u in jobs.items() if f not in bad}
import hashlib
out=namespace['OUT'];# Keep actual image encoding and HTTP content type aligned without recolouring or transcoding.
from PIL import Image
import shutil
remap={};corrected={}
for filename,url in jobs.items():
 file=out/filename if 'out' in globals() else namespace['OUT']/filename
 actual=Image.open(file).format.lower().replace('jpeg','jpg');extension=file.suffix[1:].lower().replace('jpeg','jpg')
 target=filename if actual==extension else str(Path(filename).with_suffix('.'+actual))
 if target!=filename:
  shutil.copyfile(file,namespace['OUT']/target);remap['/media/catalogue/'+filename]='/media/catalogue/'+target
 corrected[target]=url
jobs=corrected
for model in models:
 for variant in model['variants']:variant['image']=remap.get(variant['image'],variant['image'])
 model['gallery']=[remap.get(path,path) for path in model['gallery']]
assets=[{'file':'/media/catalogue/'+f,'source':url,'bytes':(out/f).stat().st_size,'sha256':hashlib.sha256((out/f).read_bytes()).hexdigest()} for f,url in jobs.items()]
manifest={'collected':'2026-11-08','usage':'Ảnh tham khảo model/màu từ nguồn công khai phục vụ demo local; giá, vốn, tồn và bảo hành mô phỏng.','models':models,'assets':assets}
(root/'tai-lieu/33_DanhMucBoSung.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8');print('Saved',len(models),'models and',len(assets),'images',flush=True)
