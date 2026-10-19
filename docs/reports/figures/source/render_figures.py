"""Render explanatory SVG and editable DOT from observed report fixtures.

No Java algorithms are implemented here. Layout positions are illustrative.
Run with Python 3 from any directory; only the standard library is required.
"""
from pathlib import Path
import json
import math
from html import escape

HERE = Path(__file__).resolve().parent
FIGURES = HERE.parent
REPORTS = FIGURES.parent
ACTUAL = REPORTS / "evidence" / "actual"
BLUE, GREEN, AMBER, RED = "#2563eb", "#15803d", "#b45309", "#b91c1c"
INK, MUTED = "#172b4d", "#526479"
manifest = []


def snapshot(stem):
    return json.loads((ACTUAL / f"{stem}.json").read_text(encoding="utf-8"))


def text(x, y, lines, size=17, color=INK, anchor="middle"):
    parts = [f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-size="{size}" fill="{color}">']
    for index, line in enumerate(lines):
        parts.append(f'<tspan x="{x}" dy="{0 if index == 0 else size + 7}">{escape(str(line))}</tspan>')
    return "".join(parts) + "</text>"


def render(name, title, subtitle, nodes, edges, width=1200, height=860, legend=None, sources=None):
    """nodes carry explicit center, size, text, fill; edges carry direction/color."""
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img">',
             f'<title>{escape(title)}</title><desc>{escape(subtitle)}</desc>',
             '<defs><marker id="arrow" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto"><path d="M0,0 L10,4 L0,8 Z" fill="#64748b"/></marker></defs>',
             '<rect width="100%" height="100%" rx="18" fill="#f8fafc"/>',
             '<g font-family="Arial, DejaVu Sans, sans-serif">',
             text(40, 45, [title], 25, anchor="start"),
             text(40, 78, [subtitle], 16, MUTED, "start")]
    dot = ["digraph report {", '  graph [rankdir=TB, bgcolor="white"];',
           '  node [shape=box, style="rounded,filled", fontname="Arial"];',
           "  label=" + json.dumps(title, ensure_ascii=False) + ";"]
    for edge in edges:
        a, b = nodes[edge["from"]], nodes[edge["to"]]
        dx, dy = b["x"] - a["x"], b["y"] - a["y"]
        def clipped(n, direction):
            limits = []
            if dx:
                limits.append(n.get("w", 250) / (2 * abs(dx)))
            if dy:
                limits.append(n.get("h", 112) / (2 * abs(dy)))
            t = min(limits)
            return n["x"] + direction * dx * t, n["y"] + direction * dy * t
        x1, y1 = clipped(a, 1)
        x2, y2 = clipped(b, -1)
        color = edge.get("color", "#64748b")
        dash = ' stroke-dasharray="8 6"' if edge.get("dash") else ""
        curve = edge.get("curve", 0)
        if curve:
            norm = math.hypot(dx, dy)
            cx, cy = (x1 + x2) / 2 - dy / norm * curve, (y1 + y2) / 2 + dx / norm * curve
            d = f"M{x1},{y1} Q{cx},{cy} {x2},{y2}"
        else:
            d = f"M{x1},{y1} L{x2},{y2}"
        parts.append(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="3"{dash} marker-end="url(#arrow)"/>')
        dot.append(f'  "{edge["from"]}" -> "{edge["to"]}" [color="{color}"' + (',style="dashed"' if edge.get("dash") else '') + '];')
    for key, node in nodes.items():
        w, h = node.get("w", 250), node.get("h", 112)
        fill, stroke = node.get("fill", "#ffffff"), node.get("stroke", "#94a3b8")
        dash = ' stroke-dasharray="6 4"' if node.get("dash") else ""
        parts.append(f'<rect x="{node["x"] - w/2}" y="{node["y"] - h/2}" width="{w}" height="{h}" rx="12" fill="{fill}" stroke="{stroke}" stroke-width="2"{dash}/>')
        lines = node["lines"]
        size = node.get("font", 17)
        first_y = node["y"] - ((len(lines) - 1) * (size + 7)) / 2 + size / 3
        parts.append(text(node["x"], first_y, lines, size, node.get("color", INK)))
        dot.append(f'  "{key}" [label=' + json.dumps("\n".join(lines), ensure_ascii=False) + f',fillcolor="{fill}",fontcolor="{node.get("color", INK)}"];')
    for index, (color, label) in enumerate(legend or []):
        y = height - 78 + index * 24
        parts.append(f'<rect x="40" y="{y-13}" width="18" height="18" rx="3" fill="{color}"/>')
        parts.append(text(70, y, [label], 15, MUTED, "start"))
    parts.append('</g></svg>')
    dot.append('}')
    (FIGURES / f"{name}.svg").write_text("\n".join(parts), encoding="utf-8")
    (HERE / f"{name}.dot").write_text("\n".join(dot) + "\n", encoding="utf-8")
    manifest.append({"figure": f"{name}.svg", "sources": sources or [], "nodes": nodes, "edges": edges,
                     "note": "Illustration from observed data; coordinates are manually specified, not application output."})


def node(x, y, lines, **style):
    return {"x": x, "y": y, "lines": lines, **style}


def family(stem, name, title, positions):
    data = snapshot(stem)
    graph = data["orderedFamilyGraph"]
    nodes = {}
    for item in graph["nodes"]:
        p, levels = item["person"], item["relationLevels"]
        fill = "#dbeafe" if levels[0] > 0 else "#dcfce7" if levels[0] < 0 else "#1e40af"
        stroke = BLUE if levels[0] >= 0 else GREEN
        labels = data["relationCodesById"][p["id"]]
        lines = [f'{p["id"]} | {p["gender"]}', f'levels = {levels}', *labels]
        nodes[p["id"]] = node(*positions[p["id"]], lines, fill=fill, stroke=stroke,
                               w=320, h=138, font=17, color="#ffffff" if levels == [0] else INK)
    edges = [{"from": e["parentId"], "to": e["childId"]} for e in graph["edges"]]
    topo = " → ".join(graph["topoOrder"])
    render(name, title, "Topo: " + topo + " | Cạnh: parent → child", nodes, edges, height=880,
           legend=[(BLUE, "Tổ tiên / TARGET (SELF); đọc mức để phân biệt"),
                   (GREEN, "Hậu duệ; vị trí chỉ minh họa, không phải hàng suy từ topo")], sources=[stem])


family("family_tree_example", "family_tree_example", "Gia phả P03 — k=2, BOTH",
       {"P01": (320, 220), "P02": (880, 220), "P03": (600, 440), "P04": (600, 660)})
family("family_tree_multiple_levels", "family_tree_multiple_levels", "P10 có hai quan hệ với TARGET P13",
       {"P10": (260, 220), "P11": (900, 220), "P12": (900, 450), "P13": (260, 670)})
family("family_tree_union_edges", "family_tree_union_edges", "Hợp cạnh khác với mọi đường ghép trong biên — k=3",
       {"A": (260, 220), "B": (900, 220), "C": (900, 450), "T": (260, 670)})


def relationship(stem, name, title, positions, first_branch, second_branch):
    data = snapshot(stem)
    result = data["relationshipResult"]
    nodes = {}
    common = set(result["commonAncestorIds"])
    for p in result["evidencePersons"]:
        pid = p["id"]
        if pid in common:
            fill, stroke, role = "#fef3c7", AMBER, "COMMON ANCESTOR"
        elif pid in first_branch:
            fill, stroke, role = "#dbeafe", BLUE, "Vùng source 1"
        else:
            fill, stroke, role = "#dcfce7", GREEN, "Vùng source 2"
        nodes[pid] = node(*positions[pid], [f'{pid} | {p["gender"]}', role], fill=fill, stroke=stroke, w=240)
    edges = []
    for e in result["evidenceEdges"]:
        color = BLUE if e["childId"] in first_branch else GREEN
        edges.append({"from": e["parentId"], "to": e["childId"], "color": color})
    render(name, title, "Mũi tên: parent → child. PATH được đọc từ source đi ngược lên ancestor.", nodes, edges,
           legend=[(BLUE, "Các path source 1; màu lục: các path source 2"),
                   (AMBER, "Node giao; hình là hợp đúng các cạnh evidence của result")], sources=[stem])


relationship("relationship_example", "relationship_paths", "P05 / P06 — hai ancestor chung, bốn path",
             {"P01": (320, 200), "P02": (880, 200), "P03": (320, 430), "P04": (880, 430), "P05": (320, 650), "P06": (880, 650)},
             {"P03", "P05"}, {"P04", "P06"})
relationship("relationship_multiple_paths", "relationship_multiple_paths", "P01 / P02 — mọi đường hai cạnh tới P00",
             {"P00": (600, 190), "P10": (160, 420), "P11": (450, 420), "P20": (750, 420), "P21": (1040, 420), "P01": (305, 650), "P02": (895, 650)},
             {"P10", "P11", "P01"}, {"P20", "P21", "P02"})

short = snapshot("relationship_short_and_long")["relationshipResult"]["paths"]
nodes, edges = {}, []
for index, path in enumerate(short):
    x = 220 + index * 390
    color = BLUE if path["sourceId"] == "P01" else GREEN
    for j, pid in enumerate(path["nodeIds"]):
        y = 640 - j * (400 / max(1, len(path["nodeIds"]) - 1))
        key = f"panel{index}_{pid}"
        nodes[key] = node(x, y, [pid, "SOURCE" if j == 0 else "COMMON" if j == len(path["nodeIds"]) - 1 else "Trung gian"],
                          fill="#fef3c7" if pid == "P00" else "#dbeafe" if color == BLUE else "#dcfce7", stroke=color, w=250)
        if j:
            edges.append({"from": key, "to": f'panel{index}_{path["nodeIds"][j-1]}', "color": color})
render("relationship_short_and_long", "Ba PATH result: độ dài 1, 2 và 1", "Mỗi cột là một path; ID P00 lặp để so sánh, không phải Person mới.", nodes, edges,
       width=1240, legend=[(BLUE, "Hai path của P01, gồm cả đường dài hơn shortest"), (GREEN, "Một path của P02; số path 2+1=3, không ghép cặp")], sources=["relationship_short_and_long"])

boundary = snapshot("relationship_outside_boundary")["validationResult"]["data"]["graph"]
positions = {"P01": (600, 170), "P02": (310, 330), "P03": (310, 490), "P04": (310, 650), "P06": (890, 330), "P07": (890, 490), "P08": (890, 650)}
depth = {"P04": 0, "P03": 1, "P02": 2, "P08": 0, "P07": 1, "P06": 2}
nodes = {p["id"]: node(*positions[p["id"]], [p["id"], f'depth {depth[p["id"]]}' if p["id"] in depth else "depth 3 — ngoài biên"],
                        fill="#e2e8f0" if p["id"] == "P01" else "#dbeafe" if p["id"] in {"P02", "P03", "P04"} else "#dcfce7", w=240, h=96) for p in boundary["persons"]}
edges = [{"from": e["parentId"], "to": e["childId"], "dash": e["parentId"] == "P01"} for e in boundary["edges"]]
render("relationship_boundary", "Ancestor chung có trong input nhưng nằm ngoài cửa sổ", "Actual: RELATED false; không path/evidence. Màu chỉ vùng BFS đã xét, không phải evidence result.", nodes, edges,
       legend=[(BLUE, "P04 → P03 → P02; lục: P08 → P07 → P06"), ("#94a3b8", "P01 bị loại vì depth 3; cạnh nét đứt không được mở rộng")], sources=["relationship_outside_boundary"])

bug = snapshot("relationship_same_gender_bug")["validationResult"]["data"]["graph"]
nodes = {"P00": node(600, 250, ["P00 | MALE", "Common ancestor theo input"], fill="#fef3c7", stroke=AMBER, w=330),
         "P01": node(300, 510, ["P01 | MALE", "Source 1"], fill="#fee2e2", stroke=RED),
         "P02": node(900, 510, ["P02 | MALE", "Source 2"], fill="#fee2e2", stroke=RED)}
render("relationship_same_gender_bug", "Phản ví dụ: expected RELATED true, actual false", "Input hợp lệ; checker dừng ở gender guard, không chạy BFS. Source chưa được sửa.", nodes,
       [{"from": e["parentId"], "to": e["childId"]} for e in bug["edges"]], height=730,
       legend=[(RED, "Actual common/paths/evidence đều rỗng"), (AMBER, "Cạnh trong hình là INPUT; không gắn nhãn là evidence result")], sources=["relationship_same_gender_bug"])

cycle = snapshot("invalid_cycle_example")["parseResult"]["data"]["graph"]
nodes = {"P01": node(270, 240, ["P01", "VISITING, index 0"], fill="#fee2e2", stroke=RED),
         "P02": node(920, 240, ["P02", "VISITING, index 1"], fill="#fee2e2", stroke=RED),
         "P03": node(600, 530, ["P03", "VISITING, index 2"], fill="#fee2e2", stroke=RED)}
render("validation_cycle", "DIRECTED_CYCLE — Input bị từ chối", "Cycle thực tế: [P01, P02, P03, P01]. Không tạo NormalizedInput hoặc query result.", nodes,
       [{"from": e["parentId"], "to": e["childId"], "color": RED if e["parentId"] == "P03" else "#64748b"} for e in cycle["edges"]], height=760,
       legend=[(RED, "Back edge P03 → P01 gặp node đang VISITING"), ("#64748b", "Trace DFS minh họa từ fixture; graph chưa qua validation")], sources=["invalid_cycle_example"])

nodes, edges = {}, []
for i, stem in enumerate(["invalid_duplicate_edge", "invalid_unknown_id", "invalid_parent_gender"]):
    data = snapshot(stem)
    graph = data["parseResult"]["data"]["graph"]
    x = 220 + i * 440
    nodes[f"title{i}"] = node(x, 180, [data["validationResult"]["error"]["code"]], fill="#fee2e2", stroke=RED, w=410, h=70, font=17)
    if i == 0:
        nodes["d0"] = node(x, 340, ["P00 | MALE"], w=210, h=70)
        nodes["d2"] = node(x, 580, ["P02 | UNKNOWN"], w=240, h=70)
        edges += [{"from": "d0", "to": "d2", "color": RED, "curve": 60}, {"from": "d0", "to": "d2", "color": RED, "curve": -60}]
    elif i == 1:
        nodes["u99"] = node(x, 340, ["P99 — chưa khai báo"], w=280, h=70, dash=True, fill="#fee2e2")
        nodes["u1"] = node(x, 580, ["P01 | MALE"], w=240, h=70)
        edges.append({"from": "u99", "to": "u1", "color": RED})
    else:
        nodes["m1"] = node(x - 105, 340, ["P01 | MALE"], w=190, h=70)
        nodes["m2"] = node(x + 105, 340, ["P02 | MALE"], w=190, h=70)
        nodes["c"] = node(x, 580, ["P03 | UNKNOWN"], w=240, h=70)
        edges += [{"from": "m1", "to": "c", "color": RED}, {"from": "m2", "to": "c", "color": RED}]
render("validation_errors", "Ba ví dụ lỗi độc lập — dữ liệu dừng tại validator", "Các panel là hình rút gọn điểm gây lỗi; xem fixture để có toàn bộ người/cạnh/query.", nodes, edges, width=1360, height=800,
       legend=[(RED, "Trái: cạnh lặp; giữa: endpoint lạ; phải: hai known parent cùng gender"), ("#94a3b8", "Không có result Family Tree/BFS cho ba Input này")],
       sources=["invalid_duplicate_edge", "invalid_unknown_id", "invalid_parent_gender"])

nodes = {
    "raw": node(560, 170, ["RawInputParser", "String → ValidationResult"], w=340, h=85, fill="#e0f2fe"),
    "parse": node(560, 300, ["RawTextParser", "ParsedInput / lỗi cú pháp"], w=340, h=85),
    "validate": node(560, 430, ["ParsedInputValidator", "DFS + parent rule + query"], w=340, h=85),
    "normalize": node(560, 560, ["InputNormalizer", "NormalizedInput"], w=340, h=85, fill="#dcfce7"),
    "failure": node(170, 430, ["ValidationError", "Dừng trước truy vấn"], w=280, h=85, fill="#fee2e2", stroke=RED),
    "family": node(300, 720, ["FamilyTreePipeline", "Kahn → view → labels"], w=360, h=85, fill="#dbeafe"),
    "relation": node(850, 720, ["RelationshipChecker", "Ancestor paths → evidence"], w=370, h=85, fill="#dcfce7"),
    "output": node(560, 920, ["app / output / web", "Chưa triển khai trong checkout"], w=440, h=90, fill="#e2e8f0", dash=True),
}
edges = [{"from": a, "to": b} for a, b in [("raw", "parse"), ("parse", "validate"), ("validate", "normalize"), ("validate", "failure")]]
edges += [{"from": a, "to": b, "dash": True} for a, b in [("normalize", "family"), ("normalize", "relation"), ("family", "output"), ("relation", "output")]]
render("system_architecture", "Kiến trúc hiện có và ranh giới tích hợp", "Shared model dùng chung; query dispatch chỉ do caller kiểm chứng thực hiện, chưa có production app.", nodes, edges, width=1160, height=1100,
       legend=[(BLUE, "Nét liền: lời gọi/luồng trong module đã có"), ("#94a3b8", "Nét đứt: caller cần nối hoặc thành phần còn thiếu")])

nodes = {
    "raw": node(170, 250, ["Raw text"], w=240, h=100, fill="#e0f2fe"),
    "parse": node(510, 250, ["Parser", "ParsedInput"], w=280, h=100),
    "validate": node(850, 250, ["Validator", "ValidatedInput"], w=280, h=100),
    "normal": node(1190, 250, ["Normalizer", "NormalizedInput"], w=280, h=100, fill="#dcfce7"),
    "failure": node(680, 510, ["Failure: data=null", "ERROR_CODE + detail + cycle nếu có"], w=520, h=100, fill="#fee2e2", stroke=RED),
}
edges = [{"from": a, "to": b} for a, b in [("raw", "parse"), ("parse", "validate"), ("validate", "normal")]]
edges += [{"from": a, "to": "failure", "color": RED} for a in ["parse", "validate"]]
render("input_pipeline", "Raw text → object đã kiểm tra → domain model", "Parser giữ token/count; validator thêm invariant; normalizer chỉ chuyển kiểu, không thêm dữ liệu.", nodes, edges, width=1370, height=750,
       legend=[(GREEN, "Chỉ success đi tới thuật toán query; tên thiếu đã thành chuỗi rỗng từ parser"), (RED, "Failure dừng pipeline, không gọi normalizer hoặc query")])

(HERE / "figure_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Rendered {len(manifest)} SVG and DOT figures")
