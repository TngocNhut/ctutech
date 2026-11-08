"""Generate a new, additive Flyway migration from the reviewed catalogue manifest."""
from pathlib import Path
import json,unicodedata,re
ROOT=Path(__file__).resolve().parents[1]
x=json.loads((ROOT/'tai-lieu/33_DanhMucBoSung.json').read_text('utf8'));seed=json.loads((ROOT/'tai-lieu/05_DuLieuSanPham.json').read_text('utf8'))
def q(s):return 'NULL' if s is None else "'"+str(s).replace("'","''")+"'"
def norm(s):return unicodedata.normalize('NFD',s.replace('Đ','D').replace('đ','d')).encode('ascii','ignore').decode().lower()
def slug(s):return re.sub('[^a-z0-9]+','-',norm(s)).strip('-')
lines=['-- Additive demo catalogue: additional public-sourced demo models.','-- Existing SKU price/stock and order snapshots are preserved. Media remains attributed.','DO $catalogue$ DECLARE product_id bigint; category_id bigint; brand_id bigint; line_id bigint; sku_id bigint; BEGIN']
for i,m in enumerate(x['models']):
 lines+=['IF NOT EXISTS (SELECT 1 FROM "SanPham" WHERE "DuongDan"='+q(m['slug'])+') THEN']
 lines+=['SELECT "MaPhanLoai" INTO category_id FROM "PhanLoai" WHERE "DuongDan"='+q(m['category'])+';', 'INSERT INTO "ThuongHieu" ("TenThuongHieu","DuongDan","DangHienThi","NgayCapNhat") VALUES ('+q(m['brand'])+','+q(m['brandSlug'])+',true,now()) ON CONFLICT ("DuongDan") DO NOTHING;', 'SELECT "MaThuongHieu" INTO brand_id FROM "ThuongHieu" WHERE "DuongDan"='+q(m['brandSlug'])+';', 'INSERT INTO "DongSanPham" ("MaThuongHieu","TenDongSanPham","DuongDan") VALUES (brand_id,'+q(m['line'])+','+q(slug(m['line']))+') ON CONFLICT ("MaThuongHieu","DuongDan") DO NOTHING;', 'SELECT "MaDongSanPham" INTO line_id FROM "DongSanPham" WHERE "MaThuongHieu"=brand_id AND "DuongDan"='+q(slug(m['line']))+';']
 if m['existingId']:
  lines+=['SELECT "MaSanPham" INTO product_id FROM "SanPham" WHERE "MaSanPham"='+str(m['existingId'])+';', 'UPDATE "SanPham" SET "MaDongSanPham"=coalesce("MaDongSanPham",line_id),"NgayCapNhat"=now() WHERE "MaSanPham"=product_id;']
 else:
  lines+=['INSERT INTO "SanPham" ("MaPhanLoai","MaThuongHieu","MaDongSanPham","TenSanPham","DuongDan","TenTimKiem","MoTaNgan","MoTaChiTiet","SoThangBaoHanh","ThongSoChung","DuongDanNguon","NgayThuThap","LaDuLieuMau","DangHienThi","NgayCapNhat") VALUES (category_id,brand_id,line_id,'+','.join(q(v) for v in [m['name'],m['slug'],norm(m['name']+' '+m['brand']),m['summary'],'Dữ liệu trình diễn. Thông tin model và ảnh có nguồn tham khảo; giá, vốn, tồn và bảo hành là mô phỏng.'])+",12,'{}'::jsonb,"+q(m['source'])+",'2026-11-08',true,true,now()) ON CONFLICT (\"DuongDan\") DO NOTHING;", 'SELECT "MaSanPham" INTO product_id FROM "SanPham" WHERE "DuongDan"='+q(m['slug'])+';']
 existing=[v for v in seed['BienTheSanPham'] if v['MaSanPham']==m['existingId']]
 for j,v in enumerate(m['variants']):
  matching=next((e for e in existing if norm(e['MauSac'])==norm(v['colour']) and e['DungLuongRam']==v['ram'] and e['DungLuongLuuTru']==v['storage']),None)
  if matching:lines+=['sku_id := '+str(matching['MaBienThe'])+';']
  else:
   price=min(e['GiaBan'] for e in existing) if existing else (17990000+i%5*1000000 if m['category']=='laptop' else (9990000+i%6*1000000 if m['category']=='dien-thoai' else {'Tai nghe in-ear':2490000,'Tai nghe headphone':5490000,'Cáp sạc':390000,'Củ sạc':590000,'Đầu chuyển USB-A sang C':190000,'Đầu chuyển USB-C sang HDMI':790000,'Bàn phím':990000,'Chuột':490000}.get(m['accessoryType'],490000)))
   code='LUMA-NEW24-'+str(i+1).zfill(3)+'-'+str(j+1).zfill(2);label=(f"{v['ram']}GB/{v['storage']}GB · {v['colour']}" if m['category']!='phu-kien' and v['ram'] else v['colour'])[:220]
   lines+=['INSERT INTO "BienTheSanPham" ("MaSanPham","MaHang","TenBienThe","MauSac","BoXuLy","BoXuLyDoHoa","DungLuongRam","DungLuongLuuTru","KichThuocManHinh","HeDieuHanh","ThongSoBoSung","GiaNiemYet","GiaBan","GiaVon","DangKinhDoanh","NgayCapNhat") VALUES (product_id,'+','.join(q(z) for z in [code,label,v['colour'],v['cpu'],v['gpu']])+','+','.join(str(v[k]) for k in ['ram','storage','screen'])+','+q(v['os'])+",'{}'::jsonb,"+f'{price+1000000},{price},{price*8//10},true,now()) ON CONFLICT ("MaHang") DO NOTHING;', 'SELECT "MaBienThe" INTO sku_id FROM "BienTheSanPham" WHERE "MaHang"='+q(code)+';', 'INSERT INTO "TonKho" ("MaKhoHang","MaBienThe","SoLuongTon","SoLuongGiu","NgayCapNhat") VALUES (1,sku_id,12,0,now()) ON CONFLICT ("MaKhoHang","MaBienThe") DO NOTHING;']
  # Put the correct colour first when a specific SKU is selected.
  lines+=['INSERT INTO "TepSanPham" ("MaSanPham","MaBienThe","LoaiTep","DuongDanTep","MoTaThayThe","ThuTu","DuongDanNguon","ThongTinSuDung","NgayCapNhat") VALUES (product_id,sku_id,\'ANH\','+q(v['image'])+','+q(m['name']+' · '+v['colour'])+',0,'+q(m['source'])+",'Ảnh tham khảo công khai cho đồ án localhost; giữ nguồn, không tự tô màu.',now());"]
  if m['accessoryType']:
   lines+=['INSERT INTO "ThuocTinhDanhMuc" ("MaPhanLoai","KyHieu","TenThuocTinh","KieuDuLieu","ChoPhepLoc","ThuTu") VALUES (category_id,\'LOAI_PHU_KIEN\',\'Loại phụ kiện\',\'CHU\',true,0) ON CONFLICT ("MaPhanLoai","KyHieu") DO NOTHING;', 'INSERT INTO "GiaTriThuocTinhBienThe" ("MaBienThe","MaSanPham","MaPhanLoai","MaThuocTinh","KieuDuLieu","GiaTriChu") SELECT sku_id,product_id,category_id,"MaThuocTinh",\'CHU\','+q(m['accessoryType'])+' FROM "ThuocTinhDanhMuc" WHERE "MaPhanLoai"=category_id AND "KyHieu"=\'LOAI_PHU_KIEN\' ON CONFLICT ("MaBienThe","MaThuocTinh") DO NOTHING;']
 for j,file in enumerate(m['gallery']):
  lines+=['INSERT INTO "TepSanPham" ("MaSanPham","LoaiTep","DuongDanTep","MoTaThayThe","ThuTu","DuongDanNguon","ThongTinSuDung","NgayCapNhat") VALUES (product_id,\'ANH\','+q(file)+','+q(m['name']+' · ảnh tham khảo '+str(j+1))+','+str(j+1)+','+q(m['source'])+",'Ảnh công khai tham khảo model cho đồ án localhost.',now());"]
 lines+=['END IF;']
lines+=['END $catalogue$;']
target=ROOT/'src/main/resources/db/migration/V24__ThemMauSanPham.sql'
if target.exists():raise SystemExit('V24 already exists. Preserve applied migrations; generate a new version for any changes.')
target.write_text('\n'.join(lines)+'\n',encoding='utf8');print('Generated V24 with',len(x['models']),'models; original SKU price/stock untouched.')
