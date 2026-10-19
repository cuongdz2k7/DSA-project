"""Check report artifacts and explicit fixture expectations, not Maven tests.

This script does not implement the project algorithms or change expected results
to hide the two known gender deviations. Run after producing Actual API JSON.
"""
from pathlib import Path
import hashlib
import json
import re
import unicodedata
from urllib.parse import unquote
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
REPORTS = HERE.parent
PROJECT = REPORTS.parent.parent


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


actual = {p.stem: read_json(p) for p in (HERE / "actual").glob("*.json")}
assert len(actual) == 21, f"Expected 21 observed fixtures, got {len(actual)}"

# Independent, concrete expected values for the documented fixtures.
family_expected = {
    "family_tree_example": (["P01", "P02", "P03", "P04"], {"P01": [1], "P02": [1], "P03": [0], "P04": [-1]}, {"P01": ["FATHER"], "P02": ["MOTHER"], "P03": ["SELF"], "P04": ["DAUGHTER"]}, 3),
    "family_tree_multiple_levels": (["P10", "P11", "P12", "P13"], {"P10": [1, 3], "P11": [2], "P12": [1], "P13": [0]}, {"P10": ["FATHER", "GREAT_GRANDFATHER_1"], "P11": ["GRANDFATHER"], "P12": ["MOTHER"], "P13": ["SELF"]}, 4),
    "family_tree_descendant_multiple_levels": (["P10", "P11", "P12", "P13"], {"P10": [0], "P11": [-1], "P12": [-2], "P13": [-1, -3]}, {"P10": ["SELF"], "P11": ["SON"], "P12": ["GRANDDAUGHTER"], "P13": ["SON", "GREAT_GRANDSON_1"]}, 4),
    "family_tree_one_generation": (["P03"], {"P03": [0]}, {"P03": ["SELF"]}, 0),
    "family_tree_diamond": (["A", "B", "C", "T"], {"A": [2], "B": [1], "C": [1], "T": [0]}, {"A": ["GRANDPARENT"], "B": ["FATHER"], "C": ["MOTHER"], "T": ["SELF"]}, 4),
    "family_tree_union_edges": (["A", "B", "C", "T"], {"A": [1, 2], "B": [1, 2], "C": [1], "T": [0]}, {"A": ["PARENT", "GRANDPARENT"], "B": ["PARENT", "GRANDPARENT"], "C": ["PARENT"], "T": ["SELF"]}, 6),
    "topo_fifo": (["A", "C", "B"], {"A": [0], "C": [-1], "B": [-1]}, {"A": ["SELF"], "C": ["CHILD"], "B": ["CHILD"]}, 2),
}
relationship_expected = {
    "relationship_example": (["P01", "P02"], [["P05", "P03", "P01"], ["P06", "P04", "P01"], ["P05", "P03", "P02"], ["P06", "P04", "P02"]]),
    "relationship_multiple_paths": (["P00"], [["P01", "P10", "P00"], ["P01", "P11", "P00"], ["P02", "P20", "P00"], ["P02", "P21", "P00"]]),
    "relationship_short_and_long": (["P00"], [["P01", "P00"], ["P01", "P10", "P00"], ["P02", "P00"]]),
    "relationship_direct": (["P01"], [["P01"], ["P02", "P01"]]),
    "relationship_outside_boundary": ([], []),
    "relationship_disconnected": ([], []),
    "relationship_same_gender_bug": (["P00"], [["P01", "P00"], ["P02", "P00"]]),
    "relationship_unknown_gender_bug": (["P00"], [["P01", "P00"], ["P02", "P00"]]),
}
invalid_expected = {
    "invalid_count": ("INVALID_COUNT", "PERSON_COUNT expected 2 but found 1"),
    "invalid_cycle_example": ("DIRECTED_CYCLE", "P01 -> P02 -> P03 -> P01"),
    "invalid_duplicate_edge": ("DUPLICATE_EDGE", "P00 P02"),
    "invalid_parent_gender": ("SAME_GENDER_PARENTS", "child=P03 parents=P01,P02 gender=MALE"),
    "invalid_quote": ("MALFORMED_INPUT", "line 2: invalid PERSON name"),
    "invalid_unknown_id": ("UNKNOWN_ID", "P99"),
}
known_deviations = {"relationship_same_gender_bug", "relationship_unknown_gender_bug"}
checks = []
for stem, data in sorted(actual.items()):
    observation = data["validationResult"]
    if stem in family_expected:
        assert observation["valid"]
        topo, levels, codes, edge_count = family_expected[stem]
        view, result = data["orderedFamilyGraph"], data["familyTreeResult"]
        observed_levels = {n["person"]["id"]: n["relationLevels"] for n in view["nodes"]}
        assert result["topoOrder"] == topo
        assert observed_levels == levels
        assert data["relationCodesById"] == codes
        assert data["generationOffsetsById"] == levels
        assert len(result["edges"]) == edge_count
        assert [n["person"]["id"] for n in result["persons"]] == topo
        checks.append({"fixture": stem, "status": "matches_specification", "topo": topo, "levels": levels, "codes": codes})
    elif stem in invalid_expected:
        code, detail = invalid_expected[stem]
        assert not observation["valid"] and observation["data"] is None
        assert observation["error"]["code"] == code
        assert observation["error"]["detail"] == detail
        assert "familyTreeResult" not in data and "relationshipResult" not in data
        if stem == "invalid_cycle_example":
            assert observation["error"]["cycle"] == ["P01", "P02", "P03", "P01"]
        checks.append({"fixture": stem, "status": "matches_specification", "error": observation["error"]})
    else:
        expected_common, expected_paths = relationship_expected[stem]
        assert observation["valid"]
        result = data["relationshipResult"]
        observed_paths = [p["nodeIds"] for p in result["paths"]]
        matches = result["commonAncestorIds"] == expected_common and observed_paths == expected_paths and data["related"] == bool(expected_common)
        assert data["related"] == bool(result["commonAncestorIds"])
        if stem in known_deviations:
            assert not matches
            assert result["commonAncestorIds"] == [] and observed_paths == []
        else:
            assert matches
        graph = observation["data"]["graph"]
        person_ids, edge_keys = set(), set()
        for path in result["paths"]:
            ids = path["nodeIds"]
            assert ids[0] == path["sourceId"] and ids[-1] == path["ancestorId"]
            assert 0 <= len(ids) - 1 <= 2
            person_ids.update(ids)
            edge_keys.update((ids[i + 1], ids[i]) for i in range(len(ids) - 1))
        assert result["evidencePersons"] == [p for p in graph["persons"] if p["id"] in person_ids]
        assert result["evidenceEdges"] == [e for e in graph["edges"] if (e["parentId"], e["childId"]) in edge_keys]
        checks.append({"fixture": stem, "status": "matches_specification" if matches else "known_gender_deviation", "expectedCommon": expected_common, "actualCommon": result["commonAncestorIds"], "expectedPaths": expected_paths, "actualPaths": observed_paths})

matched_count = sum(c["status"] == "matches_specification" for c in checks)
deviation_count = sum(c["status"] == "known_gender_deviation" for c in checks)
observed_log = (HERE / "examples-run.log").read_text(encoding="utf-8-sig")
assert len(re.findall(r"(?m)^OBSERVED ", observed_log)) == len(checks)
assert "Exception" not in observed_log and "Error:" not in observed_log
(HERE / "example-checks.json").write_text(json.dumps({"observed": len(checks), "matchesSpecification": matched_count, "knownDeviations": deviation_count, "checks": checks}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def anchors(markdown):
    result = set()
    in_fence = False
    for line in markdown.splitlines():
        if line.startswith("```"):
            in_fence = not in_fence
        if in_fence:
            continue
        match = re.match(r"^#{1,6}\s+(.+)$", line)
        if match:
            clean = re.sub(r"<[^>]*>", "", match.group(1)).strip().lower()
            clean = "".join(c for c in clean if unicodedata.category(c)[0] in "LN" or c in "_ -")
            result.add(clean.replace(" ", "-"))
    return result


missing, link_count, snippets = [], 0, 0
main_text = [re.sub(r"\s+", "", p.read_text(encoding="utf-8-sig")) for p in (PROJECT / "src/main/java").rglob("*.java")]
for report in REPORTS.rglob("*.md"):
    content = report.read_text(encoding="utf-8-sig")
    assert len(re.findall(r"(?m)^```", content)) % 2 == 0, f"Unbalanced fence: {report}"
    assert not re.search(r"\b(TODO|TBD|FIXME)\b", content), f"Unfilled placeholder: {report}"
    for target in re.findall(r"\]\(([^)]+)\)", content):
        if re.match(r"https?://", target):
            continue
        path, _, anchor = unquote(target).partition("#")
        resolved = (report.parent / path).resolve() if path else report
        link_count += 1
        if resolved == HERE / "quality-checks.json":
            # This self-report is created after the other checks have completed.
            continue
        if not resolved.exists():
            missing.append(f"{report.name}: {target}")
        elif anchor and resolved.suffix == ".md" and anchor not in anchors(resolved.read_text(encoding="utf-8-sig")):
            missing.append(f"{report.name}: anchor {target}")
    for snippet in re.findall(r"```java\n(.*?)\n```", content, re.S):
        normalized = re.sub(r"\s+", "", snippet)
        assert any(normalized in source for source in main_text), f"Java snippet diverged: {report}"
        snippets += 1
assert not missing, "Broken links: " + repr(missing)

hashes = read_json(HERE / "source-snapshot.json")["sourceHashes"]
for relative, expected in hashes.items():
    digest = hashlib.sha256((PROJECT / relative).read_bytes()).hexdigest().upper()
    assert digest == expected, f"Protected source changed: {relative}"

figures = read_json(REPORTS / "figures/source/figure_manifest.json")
for figure in figures:
    ET.parse(REPORTS / "figures" / figure["figure"])
    for node_data in figure["nodes"].values():
        assert node_data["lines"]
    stem = figure["sources"][0] if len(figure["sources"]) == 1 else None
    if figure["figure"].startswith("family_tree_"):
        result = actual[stem]["orderedFamilyGraph"]
        assert set(figure["nodes"]) == {n["person"]["id"] for n in result["nodes"]}
        assert [(e["from"], e["to"]) for e in figure["edges"]] == [(e["parentId"], e["childId"]) for e in result["edges"]]
        svg_text = " ".join(ET.parse(REPORTS / "figures" / figure["figure"]).getroot().itertext())
        assert all(code in svg_text for codes in actual[stem]["relationCodesById"].values() for code in codes)
    if figure["figure"] in {"relationship_paths.svg", "relationship_multiple_paths.svg"}:
        result = actual[stem]["relationshipResult"]
        assert set(figure["nodes"]) == {p["id"] for p in result["evidencePersons"]}
        assert [(e["from"], e["to"]) for e in figure["edges"]] == [(e["parentId"], e["childId"]) for e in result["evidenceEdges"]]

mermaid_count = sum(p.read_text(encoding="utf-8-sig").count("```mermaid") for p in REPORTS.rglob("*.md"))
quality = {"markdownFiles": len(list(REPORTS.rglob("*.md"))), "localLinksAndAnchorsChecked": link_count,
           "brokenLinks": missing, "sourceSnippetsMatched": snippets, "protectedFilesHashMatched": len(hashes),
           "svgFilesParsed": len(figures), "mermaidSourceBlocks": mermaid_count,
           "mermaidVerification": "Source reviewed for flow and syntax; not rendered with a Mermaid engine in this environment.",
           "observedExamples": len(checks), "matchesSpecification": matched_count,
           "knownBusinessDeviations": deviation_count, "note": "Artifact checks passing do not mean all business behavior is correct; two known gender deviations are preserved."}
(HERE / "quality-checks.json").write_text(json.dumps(quality, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(quality, ensure_ascii=False, indent=2))
