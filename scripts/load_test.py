# -*- coding: utf-8 -*-
"""接口压测脚本

用法：
    python scripts/load_test.py --api products --concurrency 20 --total 500
    python scripts/load_test.py --api cart     --concurrency 20 --total 300
    python scripts/load_test.py --api checkout --concurrency 10 --total 100

说明：
- products：只读接口，测吞吐基线
- cart    ：写接口（upsert），测单表写入
- checkout：完整下单链路（加购 + 下单），每个线程使用独立账号，避免购物车互相干扰

统计口径：以 HTTP 200 且业务 code=0 记为成功；其余（含 4xx/5xx/异常）计入失败。
"""
import argparse
import statistics
import threading
import time
import uuid
from collections import Counter

import requests

BASE = "http://127.0.0.1:18091"


def login(username, password):
    r = requests.post(BASE + "/api/auth/login", json={"username": username, "password": password}, timeout=10)
    if r.status_code != 200:
        raise RuntimeError("login failed")
    return r.json()["data"]["token"]


def register_or_login(username, nickname="压测用户"):
    try:
        return login(username, "123456")
    except Exception:  # noqa: BLE001
        r = requests.post(BASE + "/api/auth/register",
                          json={"username": username, "password": "123456", "nickname": nickname}, timeout=10)
        return r.json()["data"]["token"]


def prepare_stock(product_id=1, stock=2_000_000):
    admin = login("admin", "admin123")
    headers = {"Authorization": "Bearer " + admin}
    requests.put(BASE + "/api/admin/products/%d" % product_id,
                 json={"name": "机械键盘 87 键", "category": "数码", "price": 299.00,
                       "stock": stock, "description": "压测用"},
                 headers=headers, timeout=10)


def business_ok(resp):
    if resp is None or resp.status_code != 200:
        return False
    try:
        return resp.json().get("code") == 0
    except Exception:  # noqa: BLE001
        return False


def once(api, token, product_id):
    headers = {"Authorization": "Bearer " + token}
    if api == "products":
        return business_ok(requests.get(BASE + "/api/products?page=1&size=8", timeout=15))

    if api == "cart":
        return business_ok(requests.post(BASE + "/api/cart",
                                         json={"product_id": product_id, "quantity": 1},
                                         headers=headers, timeout=15))

    if api == "checkout":
        added = requests.post(BASE + "/api/cart",
                              json={"product_id": product_id, "quantity": 1},
                              headers=headers, timeout=15)
        if not business_ok(added):
            return False
        created = requests.post(BASE + "/api/orders",
                                json={"request_id": uuid.uuid4().hex,
                                      "receiver_name": "压测",
                                      "receiver_phone": "13800000000",
                                      "receiver_address": "成都市武侯区压测路 1 号"},
                                headers=headers, timeout=20)
        return business_ok(created)

    raise SystemExit("未知 api: " + api)


def percentile(sorted_values, p):
    if not sorted_values:
        return 0.0
    k = (len(sorted_values) - 1) * p / 100
    lo, hi = int(k), min(int(k) + 1, len(sorted_values) - 1)
    return sorted_values[lo] + (sorted_values[hi] - sorted_values[lo]) * (k - lo)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api", default="products", choices=["products", "cart", "checkout"])
    parser.add_argument("--concurrency", type=int, default=20)
    parser.add_argument("--total", type=int, default=300)
    parser.add_argument("--product", type=int, default=1)
    args = parser.parse_args()

    if args.api == "checkout":
        prepare_stock(args.product)
        # 每个线程一个独立账号，避免共享购物车造成业务冲突
        tokens = [register_or_login("loaduser%d" % i) for i in range(args.concurrency)]
    else:
        tokens = [register_or_login("loadtester")] * args.concurrency

    results = []
    lock = threading.Lock()
    cursor = [0]

    def worker(idx):
        while True:
            with lock:
                if cursor[0] >= args.total:
                    return
                cursor[0] += 1
            token = tokens[idx % len(tokens)]
            start = time.time()
            try:
                ok = once(args.api, token, args.product)
            except Exception:  # noqa: BLE001
                ok = False
            cost = (time.time() - start) * 1000
            with lock:
                results.append((ok, cost))

    start = time.time()
    threads = [threading.Thread(target=worker, args=(i,)) for i in range(args.concurrency)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    elapsed = time.time() - start

    costs = sorted(c for _, c in results)
    total = len(results)
    success = sum(1 for ok, _ in results if ok)

    print("=" * 60)
    print("场景      : %s" % args.api)
    print("并发      : %d    总请求: %d" % (args.concurrency, total))
    print("总耗时    : %.2f s" % elapsed)
    print("TPS       : %.1f" % (total / elapsed))
    print("成功率    : %.2f%%  (%d/%d)" % (success * 100.0 / total, success, total))
    print("-" * 60)
    print("平均耗时  : %.1f ms" % statistics.mean(costs))
    print("P50       : %.1f ms" % percentile(costs, 50))
    print("P90       : %.1f ms" % percentile(costs, 90))
    print("P95       : %.1f ms" % percentile(costs, 95))
    print("P99       : %.1f ms" % percentile(costs, 99))
    print("最大耗时  : %.1f ms" % max(costs))
    print("=" * 60)


if __name__ == "__main__":
    main()
