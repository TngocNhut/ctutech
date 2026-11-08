"""Build two original, generic device illustrations as self-contained glTF 2.0 GLBs.
No brand geometry, downloaded model, image texture, or external buffer is used.
Run: python scripts/generate-demo-models.py
"""
from pathlib import Path
import json
import math
import struct

OUT = Path(__file__).resolve().parent.parent / 'src/main/resources/static/media/models'


class Model:
    def __init__(self, name):
        self.data = bytearray()
        self.doc = {'asset': {'version': '2.0', 'generator': 'LUMA original procedural demo',
                              'extras': {'description': 'Generic illustration, not an exact branded product.'}},
                    'scene': 0, 'scenes': [{'name': name, 'nodes': []}], 'nodes': [], 'meshes': [],
                    'materials': [], 'bufferViews': [], 'accessors': []}

    def material(self, name, color, metal=0, rough=.5, glow=None):
        mat = {'name': name, 'pbrMetallicRoughness': {'baseColorFactor': [*color, 1],
               'metallicFactor': metal, 'roughnessFactor': rough}}
        if glow:
            mat['emissiveFactor'] = glow
        self.doc['materials'].append(mat)
        return len(self.doc['materials']) - 1

    def accessor(self, values, kind, component, target):
        width = {'VEC3': 3, 'SCALAR': 1}[kind]
        fmt = 'f' if component == 5126 else 'H'
        packed = struct.pack('<' + fmt * len(values), *values)
        values = struct.unpack('<' + fmt * len(values), packed)
        while len(self.data) % 4:
            self.data.append(0)
        view = len(self.doc['bufferViews'])
        self.doc['bufferViews'].append({'buffer': 0, 'byteOffset': len(self.data),
                                       'byteLength': len(packed), 'target': target})
        self.data.extend(packed)
        item = {'bufferView': view, 'componentType': component, 'count': len(values) // width, 'type': kind}
        if kind == 'VEC3':
            item.update(min=[min(values[i::3]) for i in range(3)], max=[max(values[i::3]) for i in range(3)])
        self.doc['accessors'].append(item)
        return len(self.doc['accessors']) - 1

    def rounded(self, name, w, h, d, radius, material, position=(0, 0, 0), rx=0, parent=None):
        # Counterclockwise outline in XY, then front/back fans and outward sides.
        outline = []
        for cx, cy, angle in [(w/2-radius, h/2-radius, 0), (-w/2+radius, h/2-radius, 90),
                               (-w/2+radius, -h/2+radius, 180), (w/2-radius, -h/2+radius, 270)]:
            for i in range(9):
                a = math.radians(angle+i*90/8)
                outline.append((cx+radius*math.cos(a), cy+radius*math.sin(a)))
        positions, normals, indices = [], [], []

        def triangle(a, b, c):
            u, v = [b[i]-a[i] for i in range(3)], [c[i]-a[i] for i in range(3)]
            n = (u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0])
            length = math.sqrt(sum(x*x for x in n))
            if length < 1e-14:
                return
            start = len(positions)//3
            for p in (a, b, c):
                positions.extend(p)
                normals.extend(x/length for x in n)
            indices.extend((start, start+1, start+2))

        for i, p in enumerate(outline):
            q = outline[(i+1) % len(outline)]
            pf, qf, pb, qb = (*p, d/2), (*q, d/2), (*p, -d/2), (*q, -d/2)
            triangle((0, 0, d/2), pf, qf)
            triangle((0, 0, -d/2), qb, pb)
            triangle(pf, pb, qb)
            triangle(pf, qb, qf)
        primitive = {'attributes': {'POSITION': self.accessor(positions, 'VEC3', 5126, 34962),
                                   'NORMAL': self.accessor(normals, 'VEC3', 5126, 34962)},
                     'indices': self.accessor(indices, 'SCALAR', 5123, 34963), 'material': material}
        mesh = len(self.doc['meshes'])
        self.doc['meshes'].append({'name': name, 'primitives': [primitive]})
        node = {'name': name, 'mesh': mesh, 'translation': list(position)}
        if rx:
            node['rotation'] = [math.sin(math.radians(rx)/2), 0, 0, math.cos(math.radians(rx)/2)]
        index = len(self.doc['nodes'])
        self.doc['nodes'].append(node)
        if parent is None:
            self.doc['scenes'][0]['nodes'].append(index)
        else:
            self.doc['nodes'][parent].setdefault('children', []).append(index)
        return index

    def group(self, name, position, rx):
        index = len(self.doc['nodes'])
        self.doc['nodes'].append({'name': name, 'translation': list(position),
                                 'rotation': [math.sin(math.radians(rx)/2), 0, 0, math.cos(math.radians(rx)/2)]})
        self.doc['scenes'][0]['nodes'].append(index)
        return index

    def save(self, filename):
        while len(self.data) % 4:
            self.data.append(0)
        self.doc['buffers'] = [{'byteLength': len(self.data)}]
        js = json.dumps(self.doc, ensure_ascii=True, separators=(',', ':')).encode()
        js += b' ' * (-len(js) % 4)
        result = struct.pack('<4sII', b'glTF', 2, 12+8+len(js)+8+len(self.data))
        result += struct.pack('<I4s', len(js), b'JSON') + js
        result += struct.pack('<I4s', len(self.data), b'BIN\x00') + self.data
        OUT.mkdir(parents=True, exist_ok=True)
        (OUT / filename).write_bytes(result)
        print(filename, len(result), 'bytes')


def phone():
    m = Model('Generic phone illustration')
    metal = m.material('Brushed graphite frame', (.28, .32, .36), .85, .28)
    back = m.material('Soft teal back', (.055, .25, .25), .25, .42)
    glass = m.material('Dark glass', (.009, .014, .022), .25, .16)
    screen = m.material('Teal screen illustration', (.025, .25, .27), .1, .4, (.015, .12, .14))
    light = m.material('Screen accents', (.35, .67, .65), .1, .5)
    m.rounded('Frame', .074, .152, .009, .010, metal, (0, .076, 0))
    m.rounded('Back panel', .071, .149, .001, .009, back, (0, .076, -.0046))
    m.rounded('Glass front', .070, .148, .001, .0085, glass, (0, .076, .0046))
    m.rounded('Display', .065, .138, .0004, .0065, screen, (0, .075, .0052))
    m.rounded('Earpiece', .017, .0025, .0005, .0012, glass, (0, .145, .0057))
    for y, width in [(.103, .044), (.091, .035), (.079, .027)]:
        m.rounded('Display accent', width, .003, .0003, .001, light, (-.006, y, .0055))
    m.rounded('Power key', .0015, .018, .003, .0007, metal, (.0375, .092, 0))
    m.rounded('Volume key', .0015, .026, .003, .0007, metal, (-.0375, .10, 0))
    m.rounded('Camera island', .028, .040, .002, .007, metal, (-.018, .126, -.0055))
    for y in (.116, .135):
        m.rounded('Lens ring', .014, .014, .002, .007, metal, (-.018, y, -.007))
        m.rounded('Lens glass', .010, .010, .0022, .005, glass, (-.018, y, -.0073))
    m.save('demo-phone.glb')


def laptop():
    m = Model('Generic laptop illustration')
    metal = m.material('Aluminium', (.42, .46, .50), .82, .3)
    glass = m.material('Dark glass and keys', (.012, .02, .03), .15, .35)
    screen = m.material('Teal display illustration', (.02, .20, .23), .05, .6, (.02, .13, .15))
    light = m.material('Display accents', (.35, .60, .60), .05, .65)
    pad = m.material('Trackpad', (.32, .36, .40), .6, .45)
    m.rounded('Base', .32, .22, .012, .009, metal, (0, .008, 0), rx=-90)
    for row in range(5):
        for col in range(13):
            m.rounded('Keyboard key', .017, .012, .0016, .0015, glass,
                      ((col-6)*.021, .015, -.078+row*.018), rx=-90)
    m.rounded('Trackpad', .102, .050, .0006, .004, pad, (0, .0144, .065), rx=-90)
    lid = m.group('Opened lid', (0, .016, -.101), -16)
    m.rounded('Lid', .316, .204, .007, .008, metal, (0, .102, 0), parent=lid)
    m.rounded('Display bezel', .306, .194, .0008, .006, glass, (0, .102, .0038), parent=lid)
    m.rounded('Display', .288, .176, .0004, .003, screen, (0, .103, .0044), parent=lid)
    for y, width in [(.132, .20), (.109, .16), (.086, .12)]:
        m.rounded('Display accent', width, .008, .0003, .002, light, (-.025, y, .0047), parent=lid)
    m.rounded('Camera', .003, .003, .0004, .0015, pad, (0, .195, .0044), parent=lid)
    m.save('demo-laptop.glb')


if __name__ == '__main__':
    phone()
    laptop()
