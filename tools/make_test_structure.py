"""Write a minimal vanilla NBT empty GameTest structure using only the stdlib."""
from pathlib import Path
import struct
import gzip

def string(text):
    data = text.encode('utf-8')
    return struct.pack('>H',len(data)) + data

def tag(kind,name,payload):
    return bytes([kind]) + string(name) + payload

root = Path(__file__).resolve().parents[1]
payload = tag(3,'DataVersion',struct.pack('>i',3465))
payload += tag(9,'size',bytes([3]) + struct.pack('>iiii',3,8,8,8))
palette = tag(8,'Name',string('minecraft:air')) + b'\0'
payload += tag(9,'palette',bytes([10]) + struct.pack('>i',1) + palette)
payload += tag(9,'blocks',bytes([10]) + struct.pack('>i',0))
payload += tag(9,'entities',bytes([10]) + struct.pack('>i',0))
path = root / 'src/main/resources/data/tp/structures/empty.nbt'
path.parent.mkdir(parents=True,exist_ok=True)
path.write_bytes(gzip.compress(bytes([10]) + string('') + payload + b'\0',mtime=0))
print(path)
