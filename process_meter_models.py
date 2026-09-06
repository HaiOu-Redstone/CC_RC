import json
import os

SOURCE_DIR = r"e:\trae\program\CC_RC\模型\仪表"
TARGET_MODEL_DIR = r"e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\models\block\meter"
TARGET_BLOCKSTATE_DIR = r"e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\blockstates"
TARGET_ITEM_DIR = r"e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\models\item"

os.makedirs(TARGET_MODEL_DIR, exist_ok=True)
os.makedirs(TARGET_BLOCKSTATE_DIR, exist_ok=True)
os.makedirs(TARGET_ITEM_DIR, exist_ok=True)

def process_model(input_path, output_path):
    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    data.pop('format_version', None)
    data.pop('credit', None)
    data.pop('texture_size', None)
    data.pop('groups', None)
    
    data['parent'] = 'block/block'
    data['ambientocclusion'] = False
    
    if 'textures' in data:
        if data['textures'].get('0') == 'meter':
            data['textures']['0'] = 'cc_rc:block/meter/meter'
        if data['textures'].get('particle') == 'meter':
            data['textures']['particle'] = 'cc_rc:block/meter/meter'
    
    if 'elements' in data:
        for elem in data['elements']:
            if 'rotation' in elem and elem['rotation'].get('angle') == 0:
                del elem['rotation']
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')

for i in range(16):
    input_file = os.path.join(SOURCE_DIR, f'meter_{i}.json')
    output_file = os.path.join(TARGET_MODEL_DIR, f'meter_{i}.json')
    if os.path.exists(input_file):
        process_model(input_file, output_file)
        print(f"Processed: meter_{i}.json")
    else:
        print(f"Warning: {input_file} not found!")

faces = ['floor', 'ceiling', 'wall']
facings = ['north', 'east', 'south', 'west']

rotation_map = {
    'floor': {
        'north': {'x': 0, 'y': 180},
        'east': {'x': 0, 'y': 270},
        'south': {'x': 0, 'y': 0},
        'west': {'x': 0, 'y': 90}
    },
    'ceiling': {
        'north': {'x': 180, 'y': 180},
        'east': {'x': 180, 'y': 270},
        'south': {'x': 180, 'y': 0},
        'west': {'x': 180, 'y': 90}
    },
    'wall': {
        'north': {'x': 90, 'y': 0},
        'east': {'x': 90, 'y': 90},
        'south': {'x': 90, 'y': 180},
        'west': {'x': 90, 'y': 270}
    }
}

variants = {}
for face in faces:
    for facing in facings:
        rot = rotation_map[face][facing]
        for power in range(16):
            key = f'face={face},facing={facing},power={power}'
            value = {
                'model': f'cc_rc:block/meter/meter_{power}',
                'x': rot['x'],
                'y': rot['y']
            }
            variants[key] = value

blockstate_data = {'variants': variants}
blockstate_path = os.path.join(TARGET_BLOCKSTATE_DIR, 'meter.json')
with open(blockstate_path, 'w', encoding='utf-8') as f:
    json.dump(blockstate_data, f, indent=4, ensure_ascii=False)
    f.write('\n')
print(f"Created blockstate: meter.json with {len(variants)} variants")

item_model_data = {
    'parent': 'cc_rc:block/meter/meter_0'
}
item_model_path = os.path.join(TARGET_ITEM_DIR, 'meter.json')
with open(item_model_path, 'w', encoding='utf-8') as f:
    json.dump(item_model_data, f, indent=4, ensure_ascii=False)
    f.write('\n')
print(f"Created item model: meter.json")

print("\nDone!")
