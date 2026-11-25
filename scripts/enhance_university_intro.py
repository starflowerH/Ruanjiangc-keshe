#!/usr/bin/env python3
"""增强大学简介 - 从掌上高考(info.json)抓取真实学校数据, 拼装后更新数据库

优先级: 广东117所 -> 全国其余
数据结构: 结构化字段(层次/类型/隶属/双一流/985/211) + content(真实简介正文)
"""
import json, urllib.request, subprocess, time, os, re

UA = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"}
CACHE_FILE = "/tmp/school_mapping.json"
DONE_FILE = "/tmp/uni_intro_done.json"  # 断点续传: 已处理的学校id

# 数据库凭据: 从环境变量读取, 禁止硬编码进代码仓库
DB_USER = os.environ.get("DB_USER", "miniprogram")
DB_PASS = os.environ.get("DB_PASS", "")
DB_NAME = os.environ.get("DB_NAME", "miniprogram")

def mysql_cmd(extra_args):
    """构造 mysql 命令行（凭据来自环境变量）"""
    cmd = ["mysql", f"-u{DB_USER}"]
    if DB_PASS:
        cmd.append(f"-p{DB_PASS}")
    cmd.append(DB_NAME)
    cmd.extend(extra_args)
    return cmd

def api_get(url, max_retries=15):
    for i in range(max_retries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=20) as r:
                d = json.load(r)
                if isinstance(d, dict) and d.get("code") in ("1069", "1064"):
                    wait = min(30 * (i + 1), 240)
                    print(f"  [限流] 等待{wait}s ({i+1}/{max_retries})", flush=True)
                    time.sleep(wait)
                    continue
                return d
        except Exception as e:
            wait = min(10 * (i + 1), 60)
            print(f"  [网络] {str(e)[:50]} 等{wait}s", flush=True)
            time.sleep(wait)
    return {"error": "fail"}

def load_done():
    if os.path.exists(DONE_FILE):
        with open(DONE_FILE) as f:
            return set(json.load(f))
    return set()

def save_done(done):
    with open(DONE_FILE, "w") as f:
        json.dump(sorted(done), f)

def build_intro(info):
    """拼装增强简介"""
    parts = []
    tags = []
    nature = info.get("nature_name", "")       # 公办/民办
    level = info.get("level_name", "")         # 本科/专科
    typ = info.get("type_name", "")            # 综合类/理工类
    belong = info.get("belong", "")            # 教育部/广东省
    dual = info.get("dual_class_name", "")     # 双一流
    f211 = info.get("f211") == "1"
    f985 = info.get("f985") == "1"

    if nature: tags.append(nature)
    if level: tags.append(level)
    if typ: tags.append(typ)
    if dual and dual != "-": tags.append(dual)
    if f985: tags.append("985")
    if f211: tags.append("211")
    if belong: tags.append(f"{belong}直属" if "部" in belong or "省" in belong else belong)

    head = " ".join(tags) if tags else ""
    content = (info.get("content") or "").strip()
    # 清理content里的HTML标签
    content = re.sub(r"<[^>]+>", "", content)
    content = re.sub(r"\s+", " ", content).strip()

    # 拼接: 标签 + 简介正文(取前220字)
    if content:
        intro = f"{head}。{content[:220]}" if head else content[:220]
    else:
        intro = head or info.get("name", "")
    return intro

def get_school_info(sid):
    d = api_get(f"https://static-data.gaokao.cn/www/2.0/school/{sid}/info.json")
    if "error" in d:
        return None
    data = d.get("data")
    return data if isinstance(data, dict) else None

def main():
    # 加载映射(name -> school_id)
    if not os.path.exists(CACHE_FILE):
        print("❌ 无映射缓存, 先跑 fetch_university_logos.py")
        return
    with open(CACHE_FILE) as f:
        mapping = json.load(f)
    print(f"映射缓存: {len(mapping)} 所")

    done = load_done()
    print(f"已完成: {len(done)} 所(断点续传)")

    # 查询数据库所有大学(广东优先)
    r = subprocess.run(mysql_cmd(["-N", "-e",
        "SELECT id, name FROM university ORDER BY CASE WHEN name LIKE '广东%' OR name LIKE '广州%' OR name LIKE '深圳%' OR name LIKE '华南%' OR name LIKE '佛山%' OR name LIKE '东莞%' OR name LIKE '珠海%' OR name LIKE '惠州%' OR name LIKE '肇庆%' OR name LIKE '汕头%' OR name LIKE '湛江%' OR name LIKE '江门%' OR name LIKE '中山%' OR name LIKE '韶关%' OR name LIKE '潮州%' OR name LIKE '梅州%' OR name LIKE '汕尾%' OR name LIKE '河源%' OR name LIKE '清远%' OR name LIKE '阳江%' OR name LIKE '揭阳%' OR name LIKE '茂名%' OR name LIKE '云浮%' OR name LIKE '香港%' THEN 0 ELSE 1 END, id;"]),
        capture_output=True, text=True)
    unis = [(int(l.split("\t")[0]), l.split("\t")[1]) for l in r.stdout.strip().split("\n") if "\t" in l]
    print(f"数据库大学: {len(unis)} 所(广东优先排序)")

    def norm(name):
        return name.split("（")[0].split("(")[0].strip()

    ok, fail, no_match = 0, 0, 0
    for uid, name in unis:
        if uid in done:
            continue
        entry = mapping.get(name) or mapping.get(norm(name))
        if not entry:
            no_match += 1
            continue
        info = get_school_info(entry["id"])
        if not info:
            fail += 1
            continue
        intro = build_intro(info)
        # 更新数据库
        intro_esc = intro.replace("'", "''")
        subprocess.run(mysql_cmd(["-e",
            f"UPDATE university SET description='{intro_esc}' WHERE id={uid};"]),
            capture_output=True)
        ok += 1
        done.add(uid)
        if ok <= 3 or ok % 20 == 0:
            print(f"  [{ok}成功] {name}: {intro[:60]}...", flush=True)
        save_done(done)
        time.sleep(0.6)  # 慢速防限流

    print(f"\n=== 完成 ===\n✅ 成功: {ok} | ❌ 无数据: {fail} | ❌ 未匹配: {no_match}")

if __name__ == "__main__":
    main()
