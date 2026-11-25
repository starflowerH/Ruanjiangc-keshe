#!/usr/bin/env python3
"""爬取掌上高考(gaokao.cn)大学校徽 -> 下载到本地 -> 更新数据库 logo_url

策略: 慢速 + 断点续传 + 限流退避
  - 映射缓存到 /tmp/school_mapping.json, 已拉取的不重复请求
  - 下载文件已存在且>1KB的跳过
  - API 限流(1069)时递增等待, 最长等 5 分钟
"""
import json, urllib.request, urllib.parse, subprocess, time, os, sys

UA = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"}
LOGO_DIR = "/home/xiaoyuyu/work/zhaosheng/backend/src/main/resources/static/logo"
LOGO_BASE_URL = "https://hynu.site/logo"
CACHE_FILE = "/tmp/school_mapping.json"
API_HOSTS = ["api.zjzw.cn", "api.eol.cn"]  # 同源多 host 轮换, 降低单host限流概率

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

def api_get(url, max_retries=20):
    for i in range(max_retries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=20) as r:
                d = json.load(r)
                if isinstance(d, dict) and d.get("code") in ("1069", "1064"):
                    wait = min(60 * (i + 1), 300)
                    print(f"  [限流] {d.get('message','')} 等待{wait}s ({i+1}/{max_retries})", flush=True)
                    time.sleep(wait)
                    continue
                return d
        except Exception as e:
            wait = min(10 * (i + 1), 120)
            print(f"  [网络] {str(e)[:50]} 等待{wait}s", flush=True)
            time.sleep(wait)
    return {"error": "api_get failed", "code": "FAIL"}

def load_cache():
    if os.path.exists(CACHE_FILE):
        with open(CACHE_FILE) as f:
            return json.load(f)
    return {}

def save_cache(mapping):
    with open(CACHE_FILE, "w") as f:
        json.dump(mapping, f, ensure_ascii=False)

def fetch_all_schools(cached):
    """分页拉取全量学校列表, 增量补充到缓存"""
    mapping = dict(cached)
    page = 1
    host_idx = 0
    while True:
        host = API_HOSTS[host_idx % len(API_HOSTS)]
        host_idx += 1
        d = api_get(f"https://{host}/web/api/?local_batch_id=&local_province_id=&local_type_id=&page={page}&size=30&uri=apidata/api/gk/school/lists")
        if "error" in d:
            print(f"  第{page}页最终失败", flush=True)
            break
        data = d.get("data", {})
        items = data.get("item", [])
        if not items:
            break
        if page == 1:
            total = data.get("numFound", 0)
            print(f"共 {total} 所学校", flush=True)
        for it in items:
            name = it.get("name", "").strip()
            sid = it.get("school_id")
            if name and sid:
                cur = mapping.get(name)
                if cur is None or (it.get("level_name") == "本科" and cur.get("level") != "本科"):
                    mapping[name] = {"id": sid, "level": it.get("level_name", "")}
        if page % 10 == 0:
            save_cache(mapping)
            print(f"  已拉取{page}页, 映射{len(mapping)}所", flush=True)
        page += 1
        time.sleep(1.2)  # 慢速, 防限流
        if page > 110:
            break
    save_cache(mapping)
    return mapping

def download_logo(sid, out_path):
    if os.path.exists(out_path) and os.path.getsize(out_path) > 1000:
        return True
    url = f"https://static-data.gaokao.cn/upload/logo/{sid}.png"
    for i in range(3):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=20) as r:
                data = r.read()
                if len(data) > 1000 and data[:4] in (b"\x89PNG", b"\xff\xd8\xff"):
                    with open(out_path, "wb") as f:
                        f.write(data)
                    return True
        except Exception:
            time.sleep(3)
    return False

def get_universities():
    r = subprocess.run(mysql_cmd(["-N", "-e", "SELECT id, name FROM university ORDER BY id;"]), capture_output=True, text=True)
    return [(int(l.split("\t")[0]), l.split("\t")[1]) for l in r.stdout.strip().split("\n") if "\t" in l]

def main():
    os.makedirs(LOGO_DIR, exist_ok=True)
    cached = load_cache()
    print(f"缓存映射: {len(cached)} 所", flush=True)
    mapping = fetch_all_schools(cached)

    unis = get_universities()
    print(f"数据库大学: {len(unis)} 所", flush=True)

    def norm(name):
        return name.split("（")[0].split("(")[0].strip()

    results = {"ok": [], "no_match": [], "download_fail": []}
    updates = []
    for uid, name in unis:
        entry = mapping.get(name)
        if not entry:
            entry = mapping.get(norm(name))
        if not entry:
            results["no_match"].append(name)
            continue
        sid = entry["id"]
        out_path = f"{LOGO_DIR}/{uid}.png"
        if download_logo(sid, out_path):
            updates.append((f"{LOGO_BASE_URL}/{uid}.png", uid))
            results["ok"].append(name)
        else:
            results["download_fail"].append(name)

    for url, uid in updates:
        subprocess.run(mysql_cmd(["-e", f"UPDATE university SET logo_url='{url}' WHERE id={uid};"]),
                       capture_output=True)

    print(f"\n=== 结果 ===", flush=True)
    print(f"✅ 成功: {len(results['ok'])} | ❌ 未匹配: {len(results['no_match'])} | ❌ 下载失败: {len(results['download_fail'])}", flush=True)
    if results["no_match"]:
        print("未匹配:", results["no_match"], flush=True)
    if results["download_fail"]:
        print("下载失败:", results["download_fail"], flush=True)
    print("已写入数据库:", len(updates), "条", flush=True)

if __name__ == "__main__":
    main()
