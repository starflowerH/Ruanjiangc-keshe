#!/usr/bin/env python3
"""
采集广东大学录取分数线(2024年, 物理类/历史类, 广东生源)
数据源: 掌上高考 api.zjzw.cn (真实数据)
策略: 每所大学取 物理类最低分+位次、历史类最低分+位次 各一条
      (min 最小 = 该校该科类投档最低线)
限流: 遇 1069 码退避重试
"""
import json, urllib.request, urllib.parse, subprocess, time, sys

UA = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0"}

def get(url, retries=3):
    for i in range(retries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=20) as resp:
                return json.loads(resp.read().decode())
        except Exception as e:
            if i == retries - 1:
                return {"error": str(e)[:80]}
            time.sleep(2 * (i + 1))
    return {"error": "retry exhausted"}

def mysql(sql):
    subprocess.run(["mysql", "-uminiprogram", "-pzsq205980", "miniprogram", "-e", sql],
                   capture_output=True, text=True)

# 加载映射 (name -> school_id)
with open("/tmp/gd_school_mapping.json") as f:
    d = json.load(f)
by_name = d["by_name"]

# 手动补充的映射(升格/改名)
manual = {
    "中山大学（深圳校区）": "104",
    "佛山科学技术学院": "306",
    "肇庆医学高等专科学校": "968",
    "广东轻工职业技术学院": "2048",
    "顺德职业技术学院": "974",
    "广东湛江幼儿师范专科学校": "3236",
}

# 转设/合并无法准确映射, 跳过(数据真实原则)
SKIP = {"北京理工大学珠海学院", "广州番禺职业技术学院", "广东青年职业学院", "广东舞蹈戏剧职业学院"}

# 数据库广东大学
r = subprocess.run(["mysql", "-uminiprogram", "-pzsq205980", "miniprogram", "-N", "-e",
                    "SELECT id, name FROM university WHERE province='广东' ORDER BY id;"],
                   capture_output=True, text=True)
unis = [l.split("\t", 1) for l in r.stdout.strip().split("\n") if "\t" in l]
print(f"广东大学 {len(unis)} 所, 开始采集 2024 分数线...")

ok = 0
skip = 0
fail = 0
no_score = 0
for idx, (uid, name) in enumerate(unis, 1):
    name = name.strip()
    if name in SKIP:
        print(f"[{idx}/{len(unis)}] ⏭️ {name} (转设/合并, 跳过)")
        skip += 1
        continue

    # 找 school_id
    sid = by_name.get(name) or manual.get(name)
    if not sid:
        # 模糊: 去括号/去前缀
        norm = name.split("（")[0].split("(")[0].strip()
        for k, v in by_name.items():
            if norm in k or k in norm:
                sid = v
                break
    if not sid:
        print(f"[{idx}/{len(unis)}] ❌ {name} (无school_id映射)")
        fail += 1
        continue

    # 采集分数线
    base = {"uri": "apidata/api/gk/score/province", "school_id": sid,
            "local_province_id": 44, "page": 1, "size": 20, "year": 2024}
    qs = urllib.parse.urlencode(base)
    url = f"https://api.zjzw.cn/web/api/?{qs}"

    rr = get(url)
    # 限流退避: 1069 限流, 指数退避重试(最多20次, 覆盖较长的限流窗口)
    attempt = 0
    while rr.get("code") in ("1069", "1064") and attempt < 20:
        attempt += 1
        wait = min(30 * attempt, 120)
        print(f"  ⏳ 限流(1069), 退避{wait}s 重试({attempt}/20)")
        time.sleep(wait)
        rr = get(url)

    if "error" in rr:
        print(f"[{idx}/{len(unis)}] ❌ {name} 请求失败: {rr['error']}")
        fail += 1
        continue

    data = rr.get("data")
    if not isinstance(data, dict):
        # size 超限等异常会导致 data 返回 list(空), 记为失败而非"无分数线"
        print(f"[{idx}/{len(unis)}] ❌ {name} 接口异常(data={type(data).__name__})")
        fail += 1
        continue
    items = data.get("item") or []
    if not items:
        code = rr.get("code")
        print(f"[{idx}/{len(unis)}] ⚪ {name} 无2024分数线(code={code})")
        no_score += 1
        continue

    # 取 物理类最低min、历史类最低min
    best = {}
    for it in items:
        lt = it.get("local_type_name", "")
        if lt not in ("物理类", "历史类"):
            continue
        mn = it.get("min")
        if mn is None or mn == "-" or mn == "":
            continue
        try:
            mn = int(mn)
        except (ValueError, TypeError):
            continue
        ms = it.get("min_section")
        try:
            ms = int(ms) if ms not in (None, "-", "") else None
        except (ValueError, TypeError):
            ms = None
        key = lt
        if key not in best or mn < best[key]["min_score"]:
            best[key] = {
                "min_score": mn,
                "min_section": ms,
                "local_batch": it.get("local_batch_name", ""),
                "filing": it.get("filing"),
                "avg": it.get("average"),
            }

    if not best:
        print(f"[{idx}/{len(unis)}] ⚪ {name} 无物理/历史分数线")
        no_score += 1
        continue

    # 入库
    for lt, b in best.items():
        filing = b.get("filing")
        avg = b.get("avg")
        def to_int_or_null(v):
            if v is None or v in ("-", ""):
                return "NULL"
            try:
                return int(v)
            except (ValueError, TypeError):
                return "NULL"
        sql = (f"INSERT INTO university_score_line "
               f"(university_id, year, local_type, local_batch, min_score, min_section, filing, avg_score) VALUES "
               f"({uid}, 2024, '{lt}', '{b['local_batch']}', {b['min_score']}, "
               f"{b['min_section'] if b['min_section'] else 'NULL'}, "
               f"{to_int_or_null(filing)}, {to_int_or_null(avg)});")
        mysql(sql)

    ok += 1
    print(f"[{idx}/{len(unis)}] ✅ {name} (物理{best.get('物理类',{}).get('min_score','-')}分/历史{best.get('历史类',{}).get('min_score','-')}分)")
    time.sleep(1.5)  # 温和限速, 避免1069限流

print(f"\n===== 采集完成 =====\n成功: {ok} | 跳过(转设): {skip} | 失败: {fail} | 无分数线: {no_score}")
