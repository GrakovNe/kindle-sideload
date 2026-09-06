#!/usr/bin/env python3
"""Migrate fb2converter (rupor-github/fb2converter) user configurations to
fb2cng (rupor-github/fb2cng) YAML format.

Usage:
    python3 fb2cng-migrate.py <input> <output-dir> [--fbc /path/to/fbc] [--convert]

<input> is either a user_configurations directory (per-user subfolders with
configuration.zip) or a single configuration.zip / .toml / .yaml file.

For every user the script writes <uid>.yaml (migrated configuration) and
<uid>.report.txt (mapping notes and dropped options). With --fbc each result is
validated by running `fbc -c <yaml> dumpconfig`; with --convert a real
conversion of a test book is attempted in the unpacked user environment.
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
import tomllib
import zipfile

import yaml

OLD_CONFIG_NAMES = ("configuration.toml", "configuration.yaml", "configuration.yml", "configuration.json")

# v1 pattern keywords, longest first so that substring replacement is stable
V1_KEYWORDS = [
    "#file_name_ext", "#series_first_word", "#ABBRseries", "#abbrseries", "#padnumber",
    "#bookid", "#authors", "#author", "#title", "#series", "#number", "#date",
    "#body_number", "#fi", "#f", "#mi", "#m", "#l",
]

# v1 notes.mode -> fb2cng footnotes.mode
NOTES_MODE = {
    "default": ("default", None),
    "inline": ("default", "notes.mode 'inline' has no equivalent, using 'default'"),
    "append": ("default", "notes.mode 'append' has no equivalent, using 'default'"),
    "float-old": ("float", "notes.mode 'float-old' mapped to 'float'"),
    "float-new": ("float", None),
    "float-new-more": ("float", "notes.mode 'float-new-more' mapped to 'float', see footnotes.more_paragraphs"),
}

TOC_TYPE = {"normal": "normal", "kindle": "old_kindle", "flat": "flat"}

DROPPED = {
    "document.chapter_per_file": "no equivalent in fb2cng (sections are always split)",
    "document.ignore_nonbreakable_space": "no equivalent in fb2cng",
    "document.version": "obsolete",
    "document.series_number_positions": "consumed by %0Nd padding in migrated templates",
    "document.author_format_meta": "fb2cng has a single creator_name_template, author_format dropped",
    "document.author_format_file_name": "no equivalent, file_name_format migrated instead",
    "document.toc.book_title_from_meta": "no equivalent in fb2cng",
    "document.toc.page_title": "no equivalent in fb2cng",
    "document.toc.page_maxlevel": "no equivalent in fb2cng",
    "document.cover.stamp_placement": "no equivalent in fb2cng",
    "document.cover.default": "unexpected key, ignored",
    "document.kindlegen": "Kindlegen pipeline removed in fb2cng (native KFX/AZW8 generator)",
    "sendtokindle": "converter no longer sends e-mail, the bot does it",
    "fb2epub": "legacy generators removed in fb2cng",
    "fb2mobi": "legacy generators removed in fb2cng",
    "overwrites": "no equivalent in fb2cng",
}


# ---------------------------------------------------------------------------
# v1 pattern -> go template conversion

def parse_v1(pattern):
    """Parse a v1 pattern into a node list: ('lit', s) | ('kw', name) | ('block', [nodes])."""
    text = pattern.replace("\\{", "\x01").replace("\\}", "\x02")
    pos = 0
    stack = [[]]
    while pos < len(text):
        ch = text[pos]
        if ch == "{":
            stack.append([])
            pos += 1
        elif ch == "}":
            if len(stack) > 1:
                inner = stack.pop()
                stack[-1].append(("block", inner))
            pos += 1
        else:
            kw = next((k for k in V1_KEYWORDS if text.startswith(k, pos)), None)
            if kw:
                stack[-1].append(("kw", kw))
                pos += len(kw)
            else:
                if stack[-1] and stack[-1][-1][0] == "lit":
                    stack[-1][-1] = ("lit", stack[-1][-1][1] + ch)
                else:
                    stack[-1].append(("lit", ch))
                pos += 1
    if len(stack) != 1:
        raise ValueError(f"unbalanced blocks in pattern: {pattern}")
    return stack[0]


def collect_keywords(nodes):
    kws = set()
    for kind, val in nodes:
        if kind == "kw":
            kws.add(val)
        elif kind == "block":
            kws |= collect_keywords(val)
    return kws


def go_escape_literal(s):
    # a literal "{" directly before an action breaks the go template lexer, so
    # every brace is emitted as its own action
    s = s.replace("\x01", "{").replace("\x02", "}")
    s = s.replace("{{", "\x03").replace("}}", "\x04")
    s = s.replace("{", '{{"{"}}')
    return s.replace("\x03", '{{"{{"}}').replace("\x04", '{{"}}"}}')


def author_expr(kw, prefix):
    """go expression for an author keyword, author struct in variable `prefix`."""
    base = prefix.rstrip(".")
    first_rune = lambda v: f"(first (splitList \"\" {v}))"
    if kw == "#l":
        return f"{base}.LastName"
    if kw == "#f":
        return f"{base}.FirstName"
    if kw == "#m":
        return f"{base}.MiddleName"
    if kw == "#fi":
        return f"(ternary {base}.FirstName (printf \"%s.\" {first_rune(base + '.FirstName')}) (ne {base}.FirstName \"\"))"
    if kw == "#mi":
        return f"(ternary {base}.MiddleName (printf \"%s.\" {first_rune(base + '.MiddleName')}) (ne {base}.MiddleName \"\"))"
    raise KeyError(kw)


def author_nonempty(prefix):
    return f'(or {prefix}.LastName (or {prefix}.FirstName (or {prefix}.MiddleName {prefix}.Nickname)))'


def render_v1(pattern, ctx, series_pad=2, author_format=None):
    """Convert a v1 pattern to a go template string.

    ctx: 'title' | 'output' | 'author' | 'note'
    """
    nodes = parse_v1(pattern)
    kws = collect_keywords(nodes)
    if not kws:
        return go_escape_literal(pattern), []

    captures = []
    captured = set()

    def capture(name, statement):
        if name not in captured:
            captured.add(name)
            captures.append((name, statement))

    def kw_fragment(kw):
        """Return (render fragment, non-empty condition) for a v1 keyword."""
        if ctx == "author":
            expr = author_expr(kw, ".")
            return "{{ " + expr + " }}", f'ne ({expr} | toString) ""'
        if ctx == "note":
            if kw == "#number":
                return "{{ .NoteNumber }}", "true"
            if kw == "#body_number":
                return "{{ .BodyNumber }}", "gt .BodyNumber 0"
            raise KeyError(kw)
        # title / output contexts
        if kw == "#title":
            capture("$title", "{{ $title := .Title }}")
            return "{{ $title }}", 'ne $title ""'
        if kw == "#series":
            capture("$series", '{{ $series := "" }}{{ with first .Series }}{{ $series = .Name }}{{ end }}')
            return "{{ $series }}", 'ne $series ""'
        if kw == "#number":
            capture("$sernum", "{{ $sernum := 0 }}{{ with first .Series }}{{ $sernum = .Number }}{{ end }}")
            return "{{ $sernum }}", "gt $sernum 0"
        if kw == "#padnumber":
            capture("$sernum", "{{ $sernum := 0 }}{{ with first .Series }}{{ $sernum = .Number }}{{ end }}")
            capture("$pad", '{{ $pad := "" }}{{ if gt $sernum 0 }}{{ $pad = printf "%0'
                    + str(series_pad) + 'd" $sernum }}{{ end }}')
            return "{{ $pad }}", "gt $sernum 0"
        if kw == "#series_first_word":
            capture("$series", '{{ $series := "" }}{{ with first .Series }}{{ $series = .Name }}{{ end }}')
            capture("$serword", '{{ $serword := "" }}{{ if $series }}'
                                '{{ $serword = index (splitList " " (trim $series)) 0 }}{{ end }}')
            return "{{ $serword }}", 'ne $series ""'
        if kw == "#abbrseries":
            capture("$series", '{{ $series := "" }}{{ with first .Series }}{{ $series = .Name }}{{ end }}')
            capture("$abbr", '{{ $abbr := "" }}{{ if $series }}{{ range $w := splitList " " $series }}'
                             '{{ if $w }}{{ $abbr = printf "%s%s" $abbr (lower (first (splitList "" $w))) }}{{ end }}'
                             '{{ end }}{{ end }}')
            return "{{ $abbr }}", 'ne $series ""'
        if kw == "#ABBRseries":
            capture("$series", '{{ $series := "" }}{{ with first .Series }}{{ $series = .Name }}{{ end }}')
            capture("$abbrup", '{{ $abbrup := "" }}{{ if $series }}{{ range $w := splitList " " $series }}'
                               '{{ if $w }}{{ $abbrup = printf "%s%s" $abbrup (upper (first (splitList "" $w))) }}{{ end }}'
                               '{{ end }}{{ end }}')
            return "{{ $abbrup }}", 'ne $series ""'
        if kw == "#date":
            raise KeyError("#date")
        if kw == "#file_name_ext":
            raise KeyError("#file_name_ext")
        if kw == "#file_name":
            return "{{ .SourceFile }}", 'ne .SourceFile ""'
        if kw == "#bookid":
            return "{{ .BookID }}", 'ne .BookID ""'
        if ctx == "output" and kw in ("#f", "#m", "#l", "#fi", "#mi"):
            capture_author()
            expr = author_expr(kw, "$fa")
            return "{{ " + expr + " }}", f'ne ({expr} | toString) ""'
        if ctx == "output" and kw == "#author":
            capture_author()
            inner, _ = render_v1(author_format or "#l{ #f}{ #m}", "author", series_pad)
            inner = (inner.replace(".LastName", "$fa.LastName").replace(".FirstName", "$fa.FirstName")
                         .replace(".MiddleName", "$fa.MiddleName"))
            return inner, author_nonempty("$fa")
        if ctx == "output" and kw == "#authors":
            inner, _ = render_v1(author_format or "#l{ #f}{ #m}", "author", series_pad)
            inner = (inner.replace(".LastName", "$a.LastName").replace(".FirstName", "$a.FirstName")
                         .replace(".MiddleName", "$a.MiddleName"))
            return "{{ range $i, $a := .Authors }}{{ if $i }}, {{ end }}" + inner + "{{ end }}", "gt (len .Authors) 0"
        raise KeyError(kw)

    def capture_author():
        capture("$fa", '{{ $fa := dict "LastName" "" "FirstName" "" "MiddleName" "" "Nickname" "" }}'
                       "{{ if .Authors }}{{ $fa = first .Authors }}{{ end }}")

    def block_condition(nodes):
        conds = []
        for kind, val in nodes:
            if kind == "kw":
                _, cond = kw_fragment(val)
                conds.append(cond)
            elif kind == "block":
                inner = block_condition(val)
                if inner:
                    conds.append(inner)
        if not conds:
            return None
        out = conds[0]
        for c in conds[1:]:
            out = f"(or ({out}) ({c}))"
        return out

    def render(nodes):
        out = ""
        for kind, val in nodes:
            if kind == "lit":
                out += go_escape_literal(val)
            elif kind == "kw":
                fragment, _ = kw_fragment(val)
                out += fragment
            elif kind == "block":
                cond = block_condition(val)
                if cond is None:  # v1: a block without keywords always disappears
                    continue
                out += "{{ if " + cond + " }}" + render(val) + "{{ end }}"
        return out

    body = render(nodes)
    head = ""
    if captures:
        head = "".join(stmt.replace("{{", "{{-", 1) + "\n" for _, stmt in captures)
        if body.startswith("{{"):
            body = "{{-" + body[2:]
        else:
            body = '{{- "" }}' + body
    template = head + body
    warnings = []
    if "#date" in kws:
        warnings.append("keyword #date dropped (no equivalent)")
    if "#file_name_ext" in kws:
        warnings.append("keyword #file_name_ext dropped (no equivalent)")
    return template, warnings


# ---------------------------------------------------------------------------
# old -> new configuration mapping

def set_path(tree, path, value):
    node = tree
    for part in path.split(".")[:-1]:
        node = node.setdefault(part, {})
    node[path.split(".")[-1]] = value


def get_path(tree, path):
    node = tree
    for part in path.split("."):
        if not isinstance(node, dict) or part not in node:
            return None
        node = node[part]
    return node


def migrate_old(cfg):
    """Map an old fb2converter config dict to fb2cng schema. Returns (new_cfg, report_lines)."""
    doc = {}
    logging = {}
    out = {"version": 1, "document": doc, "logging": logging}
    report = []

    def set_out(path, value):
        if path.startswith("logging."):
            set_path(logging, path[len("logging."):], value)
        else:
            set_path(doc, path, value)

    def drop(path, reason=None):
        node = cfg
        parts = path.split(".")
        for p in parts[:-1]:
            node = node.get(p, {}) if isinstance(node, dict) else {}
        if parts[-1] in node:
            report.append(f"dropped: {path} - {reason or DROPPED.get(path, 'no equivalent in fb2cng')}")

    def move(old_path, new_path, transform=None):
        val = get_path(cfg, old_path)
        if val is None:
            return
        if transform:
            val = transform(val)
            if val is None:
                return
        set_out(new_path, val)
        report.append(f"mapped: {old_path} -> {new_path}")

    # logger
    move("logger.console.level", "logging.console.level")
    move("logger.file.level", "logging.file.level")
    move("logger.file.mode", "logging.file.mode")
    move("logger.file.destination", "logging.file.destination_template")

    # document scalars
    move("document.fix_zip_format", "fix_zip")
    move("document.open_from_cover", "open_from_cover")
    move("document.style", "stylesheet_path")
    move("document.remove_png_transparency", "images.remove_transparency")
    move("document.insert_soft_hyphen", "insert_soft_hyphen")
    move("document.file_name_transliterate", "file_name_transliterate")
    move("document.characters_per_page", "page_map.size")

    no_page_map = get_path(cfg, "document.no_page_map")
    if no_page_map is not None:
        set_out("page_map.enable", not bool(no_page_map))
        report.append("mapped: document.no_page_map -> page_map.enable (inverted)")

    # templates
    series_pad = get_path(cfg, "document.series_number_positions") or 2
    author_format = get_path(cfg, "document.author_format")
    for old_key, new_key, ctx in (
        ("document.title_format", "metainformation.title_template", "title"),
        ("document.author_format", "metainformation.creator_name_template", "author"),
        ("document.file_name_format", "output_name_template", "output"),
        ("document.notes.link_format", "footnotes.label_template", "note"),
    ):
        pattern = get_path(cfg, old_key)
        if pattern is None:
            continue
        try:
            template, warnings = render_v1(pattern, ctx, series_pad, author_format)
        except KeyError as e:
            report.append(f"dropped: {old_key} - unsupported keyword {e}")
            continue
        set_out(new_key, template)
        report.append(f"mapped: {old_key} '{pattern}' -> {new_key}")
        for w in warnings:
            report.append(f"warning: {old_key} - {w}")

    # toc
    toc_type = get_path(cfg, "document.toc.type")
    if toc_type is not None:
        mapped = TOC_TYPE.get(str(toc_type))
        if mapped:
            set_out("toc_type", mapped)
            report.append(f"mapped: document.toc.type '{toc_type}' -> toc_type '{mapped}'")
        else:
            report.append(f"dropped: document.toc.type '{toc_type}' - unknown value")
    move("document.toc.page_placement", "toc_page.placement")
    move("document.toc.include_chapters_without_title", "toc_page.include_chapters_without_title")

    # annotation
    move("document.annotation.create", "annotation.enable")
    move("document.annotation.title", "annotation.title")

    # footnotes
    mode = get_path(cfg, "document.notes.mode")
    renumber = bool(get_path(cfg, "document.notes.renumber"))
    if mode is not None:
        mapped, warn = NOTES_MODE.get(str(mode), ("default", f"unknown notes.mode '{mode}', using 'default'"))
        if renumber and mapped == "float":
            mapped = "floatRenumbered"
            report.append("mapped: document.notes.mode + renumber -> footnotes.mode 'floatRenumbered'")
        else:
            report.append(f"mapped: document.notes.mode '{mode}' -> footnotes.mode '{mapped}'")
        set_out("footnotes.mode", mapped)
        if warn:
            report.append(f"warning: {warn}")
    move("document.notes.body_names", "footnotes.bodies")

    # cover
    move("document.cover.always_convert", "images.cover.generate")
    move("document.cover.image_path", "images.cover.default_image_path")
    move("document.cover.resize", "images.cover.resize")
    move("document.cover.width", "images.screen.width")
    move("document.cover.height", "images.screen.height")

    # vignettes
    if get_path(cfg, "document.vignettes.create"):
        level_map = {"default": "book", "h0": "chapter", "h1": "section", "h2": "section"}
        pos_map = {"before_title": "title_top", "after_title": "title_bottom", "chapter_end": "end"}
        for level, new_level in level_map.items():
            images = get_path(cfg, f"document.vignettes.images.{level}") or {}
            for pos, image in images.items():
                if pos not in pos_map:
                    report.append(f"dropped: document.vignettes.images.{level}.{pos} - unknown position")
                    continue
                target = f"vignettes.{new_level}.{pos_map[pos]}"
                if get_path(doc, target) is not None:
                    report.append(f"dropped: document.vignettes.images.{level}.{pos} - collides with already mapped {target}")
                    continue
                set_out(target, image)
                report.append(f"mapped: document.vignettes.images.{level}.{pos} -> {target}")
    else:
        if get_path(cfg, "document.vignettes.images") is not None:
            report.append("dropped: document.vignettes.images - vignettes.create is false")

    # text transformations
    for name in ("speech", "dashes"):
        transform = get_path(cfg, f"document.transform.{name}")
        if isinstance(transform, dict) and ("from" in transform or "to" in transform):
            set_out(f"text_transformations.{name}.enable", True)
            for key in ("from", "to"):
                if key in transform:
                    set_out(f"text_transformations.{name}.{key}", transform[key])
            report.append(f"mapped: document.transform.{name} -> text_transformations.{name} (enabled)")

    for key, value in (("dropcaps.create", "dropcaps.enable"), ("dropcaps.ignore_symbols", "dropcaps.ignore_symbols")):
        move(f"document.{key}", value)

    # everything below is intentionally dropped
    for section in ("sendtokindle", "fb2epub", "fb2mobi", "overwrites"):
        if section in cfg:
            drop(section)
    for key in ("document.kindlegen", "document.cover.stamp_placement", "document.cover.default",
                "document.chapter_per_file", "document.ignore_nonbreakable_space", "document.version",
                "document.series_number_positions", "document.author_format_meta",
                "document.author_format_file_name", "document.toc.book_title_from_meta",
                "document.toc.page_title", "document.toc.page_maxlevel"):
        drop(key)

    return out, report


# renames for configs that are already close to the fb2cng schema
NEW_FORMAT_RENAMES = {
    ("logging", "file", "destination"): "destination_template",
    ("reporting", "destination"): "destination_template",
    ("document", "footnotes", "backlinks"): "backlink_template",
}
NEW_FORMAT_DROPS = {
}


def migrate_new_format(cfg):
    out = {}
    report = []

    def walk(node, path):
        if not isinstance(node, dict):
            return node
        result = {}
        for key, value in node.items():
            new_key = NEW_FORMAT_RENAMES.get(tuple(path + [key]), key)
            if tuple(path + [key]) in NEW_FORMAT_DROPS:
                report.append(f"dropped: {'.'.join(path + [key])} - {NEW_FORMAT_DROPS[tuple(path + [key])]}")
                continue
            if new_key != key:
                report.append(f"mapped: {'.'.join(path + [key])} -> {'.'.join(path[:-1] + [new_key])}")
            result[new_key] = walk(value, path + [key])
        return result

    migrated = walk(cfg, [])
    out.update(migrated)
    return out, report


def looks_new_format(cfg):
    if not isinstance(cfg, dict):
        return False
    doc = cfg.get("document", {})
    return isinstance(doc, dict) and bool({"output_name_template", "stylesheet_path", "fix_zip"} & set(doc))


# ---------------------------------------------------------------------------
# YAML output (block scalars for templates)

class BlockDumper(yaml.SafeDumper):
    pass


def _str_representer(dumper, data):
    if "\n" in data:
        return dumper.represent_scalar("tag:yaml.org,2002:str", data, style="|")
    return dumper.represent_scalar("tag:yaml.org,2002:str", data)


BlockDumper.add_representer(str, _str_representer)


def dump_yaml(cfg):
    return yaml.dump(cfg, Dumper=BlockDumper, allow_unicode=True, sort_keys=False, width=1000)


# ---------------------------------------------------------------------------
# per-user processing

def find_config_in_zip(zf):
    names = [n for n in zf.namelist() if "__MACOSX" not in n and not n.endswith("/")]
    for name in OLD_CONFIG_NAMES:
        if name in names:
            return name
    for name in names:
        if os.path.basename(name).lower() in OLD_CONFIG_NAMES:
            return name
    for name in names:
        if os.path.basename(name).lower().endswith((".toml", ".yaml", ".yml")) and "config" in name.lower():
            return name
    return None


def load_cfg(data, name):
    if name.endswith(".toml"):
        return tomllib.loads(data.decode("utf-8"))
    return yaml.safe_load(data.decode("utf-8"))


FILE_REF_FIELDS = [
    "document.stylesheet_path",
    "document.images.cover.default_image_path",
    "document.vignettes.book.title_top", "document.vignettes.book.title_bottom", "document.vignettes.book.end",
    "document.vignettes.chapter.title_top", "document.vignettes.chapter.title_bottom", "document.vignettes.chapter.end",
    "document.vignettes.section.title_top", "document.vignettes.section.title_bottom", "document.vignettes.section.end",
]


def fix_file_refs(migrated, tmp, report):
    """Point file references at files that actually exist in the environment."""
    avail = {}
    for root, _, files in os.walk(tmp):
        for name in files:
            avail.setdefault(name.lower(), os.path.relpath(os.path.join(root, name), tmp))
    for path in FILE_REF_FIELDS:
        val = get_path(migrated, path)
        if not isinstance(val, str) or not val or val == "builtin":
            continue
        parts = path.split(".")
        node = migrated
        for p in parts[:-1]:
            node = node.setdefault(p, {})
        if val.lower() == "none":  # old fb2converter: "none" suppresses the vignette
            node.pop(parts[-1], None)
            report.append(f"dropped: {path} 'none' - vignette suppression, omitted in fb2cng")
            continue
        if os.path.exists(os.path.join(tmp, val)):
            continue
        base = os.path.basename(val.replace("\\", "/")).lower()
        if base in avail:
            set_path(migrated, path, avail[base])
            report.append(f"fixed: {path} '{val}' -> '{avail[base]}' (file found in environment)")
        else:
            node.pop(parts[-1], None)
            report.append(f"dropped: {path} '{val}' - file is not present in the environment")


def process_zip(path, out_dir, fbc, convert):
    uid = os.path.basename(os.path.dirname(path))
    report = []
    with zipfile.ZipFile(path) as zf:
        config_name = find_config_in_zip(zf)
        if config_name is None:
            return uid, "no-config", ["no configuration file found inside zip - user was running on defaults"]
        data = zf.read(config_name)
        try:
            cfg = load_cfg(data, config_name)
        except Exception as e:  # noqa: BLE001
            return uid, "error", [f"unable to parse {config_name}: {e}"]

        if config_name.endswith(".toml"):
            migrated, report = migrate_old(cfg)
        elif looks_new_format(cfg):
            migrated, report = migrate_new_format(cfg)
        else:
            migrated, report = migrate_old(cfg)

        yaml_path = os.path.join(out_dir, f"{uid}.yaml")
        status = "migrated"
        if fbc:
            with tempfile.TemporaryDirectory() as tmp:
                zf.extractall(tmp)
                fix_file_refs(migrated, tmp, report)
                with open(yaml_path, "w", encoding="utf-8") as f:
                    f.write(dump_yaml(migrated))
                cfg_in_env = os.path.join(tmp, "configuration.yaml")
                shutil.copy(yaml_path, cfg_in_env)
                check = subprocess.run(
                    [fbc, "-c", cfg_in_env, "dumpconfig", os.path.join(tmp, "active.yaml")],
                    capture_output=True, text=True, cwd=tmp,
                )
                if check.returncode != 0:
                    status = "invalid"
                    report.append(f"fbc rejected the migrated config: {check.stderr.strip() or check.stdout.strip()}")
                elif convert:
                    book = prepare_test_book(tmp)
                    run = subprocess.run(
                        [fbc, "-c", cfg_in_env, "convert", "--to", "azw8", book],
                        capture_output=True, text=True, cwd=tmp,
                    )
                    if run.returncode != 0:
                        status = "convert-failed"
                        report.append(f"test conversion failed: {(run.stderr or run.stdout).strip()[-500:]}")
                    else:
                        output = run.stdout + run.stderr
                        bad = [l for l in output.splitlines()
                               if "Unable to expand" in l or "unexpected" in l]
                        if bad:
                            status = "template-error"
                            report.extend(f"template render error: {l.strip()}" for l in bad)
                        produced = [n for n in os.listdir(tmp) if n.endswith(".azw8")]
                        report.append(f"test conversion produced: {produced}")
        else:
            with open(yaml_path, "w", encoding="utf-8") as f:
                f.write(dump_yaml(migrated))
        return uid, status, report


TEST_BOOK = """<?xml version="1.0" encoding="UTF-8"?>
<FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:l="http://www.w3.org/1999/xlink">
<description><title-info>
<genre>novel</genre>
<book-title>Тестовая книга</book-title>
<lang>ru</lang>
<author><first-name>Иван</first-name><middle-name>Иванович</middle-name><last-name>Петров</last-name></author>
<sequence name="Тестовая серия" number="3"/>
</title-info><document-info><id>11111111-2222-3333-4444-555555555555</id></document-info></description>
<body><title><p>Тестовая книга</p></title>
<section><title><p>Глава первая</p></title><p>Начало текста<note id="n1"><p>Выносная сноска.</p></note>.</p></section>
<section><title><p>Глава вторая</p></title><p>Продолжение текста.</p></section>
</body>
</FictionBook>
"""


def prepare_test_book(tmp):
    book = os.path.join(tmp, "test_book.fb2")
    with open(book, "w", encoding="utf-8") as f:
        f.write(TEST_BOOK)
    return book


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", help="user_configurations dir or a single zip/toml/yaml")
    parser.add_argument("output", help="output directory for migrated configs and report")
    parser.add_argument("--fbc", help="path to fbc binary - validate every migrated config")
    parser.add_argument("--convert", action="store_true", help="with --fbc: also run a test conversion")
    args = parser.parse_args()

    os.makedirs(args.output, exist_ok=True)
    jobs = []
    if os.path.isdir(args.input):
        for uid in sorted(os.listdir(args.input)):
            candidate = os.path.join(args.input, uid, "configuration.zip")
            if os.path.isfile(candidate):
                jobs.append(candidate)
    else:
        jobs.append(args.input)

    summary = {}
    full_report = []
    for job in jobs:
        uid, status, report = process_zip(job, args.output, args.fbc, args.convert)
        summary[uid] = status
        full_report.append(f"=== {uid}: {status}")
        full_report.extend(f"  {line}" for line in report)
        full_report.append("")

    with open(os.path.join(args.output, "report.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(full_report))

    from collections import Counter
    counts = Counter(summary.values())
    print(f"processed {len(jobs)} configs: " + ", ".join(f"{k}={v}" for k, v in sorted(counts.items())))
    bad = {uid: s for uid, s in summary.items() if s not in ("migrated", "no-config")}
    if bad:
        print("problem users: " + ", ".join(f"{uid} ({s})" for uid, s in sorted(bad.items())))
        sys.exit(1)


if __name__ == "__main__":
    main()
