# -*- coding: utf-8 -*-
"""将 Blockbench Bedrock(bedrock 1.9.0, 扁平 elements) 模型转换为 Java block model。
规则：
- from/to 坐标直接沿用（Java 与 Bedrock 块空间同为 y-up,x-east,z-south）。
- 旋转角度 22.5/45 受 Java 1.20.1 支持，保留 rotation 节点（origin=pivot）。
- 每个面的 uv：north/east/south/west 原样；up/down 交换 [u1,v1,u2,v2]->[u2,v2,u1,v1]（对齐 Bedrock->Java 翻转）。
- 贴图引用统一替换为 cc_rc:block/server_faas/<name>。
说明：当前转换 FAAS新 目录下的新模型（faas_1/faas_2/faas_3）。
"""
import json, os, sys

OUT = r"e:\trae\program\CC_RC\src\main\resources\assets\cc_rc\models\block\server_faas"

# (源模型 json 完整路径, 输出名)
FILES = [
    (r"e:\trae\program\CC_RC\模型\FAAS\FAAS新\faas_1\faas_1\FAAS_1.json", "faas_1"),
    (r"e:\trae\program\CC_RC\模型\FAAS\FAAS新\faas_2\faas_2\FAAS_2.json", "faas_2"),
    (r"e:\trae\program\CC_RC\模型\FAAS\FAAS新\faas_3\faas_3\FAAS_3.json", "faas_3"),
]

def r2(x):
    return round(x, 3)

def face_uv(key, uv):
    u1, v1, u2, v2 = uv
    if key in ("up", "down"):
        return [r2(u2), r2(v2), r2(u1), r2(v1)]
    return [r2(u1), r2(v1), r2(u2), r2(v2)]

def convert(src_path, out_name):
    with open(src_path, "r", encoding="utf-8-sig") as f:
        data = json.load(f)
    elements = []
    for e in data["elements"]:
        el = {
            "from": e["from"],
            "to": e["to"],
        }
        rot = e.get("rotation")
        if rot and rot.get("angle") not in (0, None):
            el["rotation"] = {
                "origin": rot["origin"],
                "axis": rot["axis"],
                "angle": rot["angle"],
            }
        faces = {}
        for key, f in e["faces"].items():
            if not f.get("uv"):
                continue
            faces[key] = {"uv": face_uv(key, f["uv"]), "texture": "#0"}
        el["faces"] = faces
        elements.append(el)
    model = {
        "parent": "block/block",
        "ambientocclusion": False,
        "textures": {
            "0": "cc_rc:block/server_faas/" + out_name,
            "particle": "cc_rc:block/server_faas/" + out_name,
        },
        "elements": elements,
    }
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, out_name + ".json"), "w", encoding="utf-8") as f:
        json.dump(model, f, ensure_ascii=False)
    print("wrote", out_name, "elements:", len(elements))

for src, out in FILES:
    convert(src, out)

# 输出各文件元素数用于校验
for _, out in FILES:
    print("converted", out)