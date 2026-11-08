"""Generate a versioned seed from the existing, sourced demo catalogue."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
data = json.loads((root / 'tai-lieu/05_DuLieuSanPham.json').read_text(encoding='utf-8'))

def literal(value):
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return str(value).lower()
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, (dict, list)):
        return literal(json.dumps(value, ensure_ascii=False)) + '::jsonb'
    return "'" + value.replace("'", "''") + "'"

sql = ['-- Giá, giá vốn, tồn và bảo hành là dữ liệu mô phỏng cho localhost.']
for table in ('PhanLoai', 'ThuongHieu', 'SanPham', 'BienTheSanPham'):
    for source in data[table]:
        row = {k: v for k, v in source.items() if k not in ('SoLuongTon', 'SoLuongGiu')}
        columns = ','.join('"' + key + '"' for key in row)
        values = ','.join(literal(value) for value in row.values())
        sql.append(f'INSERT INTO "{table}" ({columns},"NgayCapNhat") VALUES ({values},now());')
    key = 'MaBienThe' if table == 'BienTheSanPham' else 'Ma' + table
    sql.append(f'SELECT setval(pg_get_serial_sequence(\'"{table}"\', \'{key}\'), (SELECT max("{key}") FROM "{table}"));')
for row in data['BienTheSanPham']:
    variant = row['MaBienThe']
    quantity = row['SoLuongTon']
    sql.append(f'INSERT INTO "TonKho" ("MaKhoHang","MaBienThe","SoLuongTon","SoLuongGiu","NgayCapNhat") VALUES (1,{variant},{quantity},0,now());')
    if quantity > 0:
        sql.append(f'INSERT INTO "BienDongKho" ("MaKhoHang","MaBienThe","LoaiBienDong","ThayDoiTon","ThayDoiGiu","KhoaChongLap","GhiChu") VALUES (1,{variant},\'SO_DU_DAU\',{quantity},0,\'seed:sku:{variant}\',\'Số dư demo ban đầu\');')
target = root / 'src/main/resources/db/migration/V2__DuLieuSanPhamMau.sql'
target.write_text('\n'.join(sql) + '\n', encoding='utf-8')
print(f'Generated {target.name}: 16 products, 24 SKUs; stock and ledger entries.')
