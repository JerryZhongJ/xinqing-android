"""Device regression on an isolated synthetic journal; preserves existing journals."""
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

SERIAL = os.environ["ADB_SERIAL"]
MARKER = "UI-v127-synthetic-regression"
PACKAGE = "cn.xinqing.journal"
BUILD = Path("build")
RESUME = "--resume-ai" in sys.argv or "--resume-offline" in sys.argv
RESULTS = json.loads((BUILD / "device-v127-results.json").read_text()) if (RESUME or "--ai-only" in sys.argv) and (BUILD / "device-v127-results.json").exists() else {}
CACHE = None


def adb(*args):
    return subprocess.check_output(["adb", "-s", SERIAL, *args], timeout=30)


def nodes():
    global CACHE
    if CACHE is None:
        adb("shell", "uiautomator", "dump", "/data/local/tmp/xinqing-ui.xml")
        CACHE = list(ET.fromstring(adb("shell", "cat", "/data/local/tmp/xinqing-ui.xml")).iter("node"))
        assert any(n.get("package") == PACKAGE for n in CACHE), "App must stay in foreground"
    return CACHE


def bounds(node):
    return list(map(int, re.findall(r"\d+", node.get("bounds"))))


def act(command):
    global CACHE
    adb("shell", command)
    CACHE = None


def scroll(up=False):
    panel = next(n for n in nodes() if n.get("class") == "android.widget.ScrollView")
    x1, y1, x2, y2 = bounds(panel)
    top, bottom = y1+(y2-y1)//6, y2-(y2-y1)//6
    start, end = (top, bottom) if up else (bottom, top)
    act(f"input swipe {(x1+x2)//2} {start} {(x1+x2)//2} {end} 600")


def find(value, key="text", up=False, attempts=12):
    for i in range(attempts):
        match = next((n for n in nodes() if n.get(key) == value or (key == "text" and re.sub(r"\s+", "", n.get(key, "")) == re.sub(r"\s+", "", value))), None)
        if match is not None:
            return match
        if i+1 < attempts:
            scroll(up)
    raise AssertionError(f"Missing UI control: {value}")


def tap(value, key="text", **kwargs):
    x1, y1, x2, y2 = bounds(find(value, key, **kwargs))
    act(f"input tap {(x1+x2)//2} {(y1+y2)//2}")


def text():
    return "\n".join(n.get("text", "") for n in nodes())


def enter(label, value):
    tap(label, "content-desc")
    act("input text " + shlex.quote(value.replace(" ", "%s")))
    act("input keyevent KEYCODE_BACK")


def rate(label, value):
    node = find(label, "content-desc")
    x1, y1, x2, y2 = bounds(node)
    x = x1+1 if value == 0 else x2-1 if value == 100 else x1+56+round((x2-x1-112)*value/100)
    act(f"input tap {x} {(y1+y2)//2}")
    if value == 0:
        caption = find(label)
        assert any(n.get("text") == "0%" and abs(bounds(n)[1]-bounds(caption)[1]) < 24 for n in nodes()), "Zero rating must actually be zero"


def page(expected, total):
    for _ in range(12):
        if f"第 {expected} / {total} 步" in text():
            return
        scroll(up=True)
    raise AssertionError(f"Expected page {expected}/{total}")


def save_method():
    tap("练习操作", "content-desc")
    tap("保存并退出")


def select_method(name):
    tap("探索想法 1", "content-desc")
    tap("继续")
    tap(name)


def screenshot(name):
    (BUILD / name).write_bytes(adb("exec-out", "screencap", "-p"))


def result(name, value=True):
    RESULTS[name] = value
    (BUILD / "device-v127-results.json").write_text(json.dumps(RESULTS, ensure_ascii=False, indent=2))
    print(name, value, flush=True)


def read_ai_distortions():
    allowed = {"非黑即白", "以偏概全", "心理过滤", "否定积极", "读心术", "预言未来", "夸大或缩小", "情绪化推理", "应该思维", "贴标签", "自责或责怪他人"}
    reasons = {}
    for _ in range(12):
        headings = [n.get("text") for n in nodes() if n.get("class") == "android.widget.TextView" and n.get("text")]
        for i, name in enumerate(headings[:-1]):
            if name in allowed and headings[i+1] not in allowed:
                reasons[name] = headings[i+1]
        before = text()
        scroll()
        if text() == before:
            return reasons
    raise AssertionError("Distortion results did not reach the end of the list")


def cleanup():
    tap("保存并退出")
    tap("返回日志列表", "content-desc")
    tap("保存并退出")
    find(MARKER)
    tap(MARKER)
    tap("返回日志列表", "content-desc")
    tap("删除日志")
    tap("删除")
    assert baseline in text()
    result("synthetic_log_removed_and_original_count_preserved")


if RESUME:
    baseline = (BUILD / "device-v127-baseline-count.txt").read_text()
else:
    baseline = next(n.get("text") for n in nodes() if n.get("text", "").startswith("我的记录"))
(BUILD / "device-v127-baseline-count.txt").write_text(baseline)
if not RESUME:
    assert baseline in text(), "Start from the journal list"
    tap("开始一篇情绪日志")
    enter("当时的情境", MARKER)
    tap("下一步")
    tap("下一步")
    tap("添加一个想法")
    enter("想法内容 1", "Because I made one typo I am a worthless failure who will never succeed and everyone must think I am stupid.")
    rate("此时对这句话的相信程度", 70)
    for _ in range(3):
        tap("下一步")
    tap("探索想法 1", "content-desc")

    if "--ai-only" not in sys.argv:
        # Manual multi-selection and help popup preserve the current choice.
        manual = ["非黑即白", "以偏概全", "心理过滤", "否定积极"]
        for name in manual:
            tap(name)
        assert all(find(name).get("checked") == "true" for name in manual)
        tap("查看全部认知歪曲说明", "content-desc", up=True)
        find("自责或责怪他人")
        tap("知道了")
        assert all(find(name, up=True).get("checked") == "true" for name in manual)
        result("manual_four_types_and_help_preserve_selection")

# One real model request on synthetic data, or resume after an external credential failure.
if "--resume-offline" in sys.argv:
    result("real_ai_blocked", "DeepSeek returned HTTP 401 with the saved credential")
else:
    tap("AI：识别认知歪曲", "content-desc", up=True)
    deadline = time.monotonic()+130
    while time.monotonic() < deadline:
        CACHE = None
        state = text()
        if any(word in state for word in ("未写入", "请求超时", "无法获取", "未通过验证", "余额不足", "AI 连接", "未接受请求", "暂时不可用", "安全连接验证失败", "已取消")):
            raise AssertionError("Synthetic AI request failed: " + state)
        pending = any(n.get("content-desc") == "取消 AI：识别认知歪曲" for n in nodes())
        selected = any(n.get("class") == "android.widget.ToggleButton" and n.get("checked") == "true" for n in nodes())
        if not pending and (selected or "AI 识别与理由" in state):
            break
        time.sleep(2)
    else:
        raise AssertionError("Synthetic AI request timed out")
    reasons = read_ai_distortions()
    assert len(reasons) > 3, "Need a real response with more than three types for this regression"
    RESULTS.pop("real_ai_blocked", None)
    result("real_ai_types", sorted(reasons))
    result("real_ai_reasons", reasons)
screenshot("device-distortions-v127.png")
if "--ai-only" in sys.argv:
    tap("保存并退出")
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
    CACHE = None
    tap(MARKER)
    tap("探索想法 1", "content-desc")
    for name in reasons:
        assert find(name, up=True).get("checked") == "true", "AI selection must survive restart"
    assert read_ai_distortions() == reasons, "All AI reasons must survive restart without truncation"
    result("real_ai_results_survive_restart")
    cleanup()
    sys.exit(0)
tap("继续")

catalog = Path("docs/ai-prompts-1.27.md").read_text().split("## 随上下文发送的方法问题\n", 1)[1].split("## 实际上下文结构", 1)[0]
expected_methods = set(re.findall(r"^### ([^\n]+)", catalog, re.M))
seen = set()
for _ in range(16):
    before = text()
    scroll(up=True)
    if text() == before:
        break
for _ in range(16):
    state = text()
    assert "悖论双重标准法" not in state
    labels = {n.get("text") for n in nodes()} | {n.get("content-desc", "").split("，")[0] for n in nodes()}
    seen.update(name for name in expected_methods if name in labels)
    before = state
    scroll()
    if text() == before:
        break
assert seen == expected_methods and len(seen) == 16, f"Missing methods: {expected_methods-seen}"
result("sixteen_methods_without_retired_entry")
tap("保存并退出")

# Check all revised page counts, observation boundaries and draft restoration.
for name, count, practice in [("实验法", 5, 4), ("恐惧幻想", 4, None), ("羞耻攻击练习", 4, 3)]:
    select_method(name)
    for number in range(1, count+1):
        page(number, count+1)
        if number == practice:
            assert not any(n.get("content-desc", "").startswith("AI：") for n in nodes())
            enter("实际记录", f"v127-observation-{number}")
        elif number < count:
            enter("我的思考", f"v127-note-{number}")
        else:
            enter("本次回应", f"v127-response-{count}")
        tap("下一步")
    page(count+1, count+1)
    if practice:
        find("本轮仍是计划。实际练习后，在实际记录页勾选已进行；未实践时可保存草稿。")
        tap("完成本次探索")
        page(count+1, count+1)
        assert not any(n.get("content-desc") == "AI：形成回应" for n in nodes())
        tap("上一步")
        for _ in range(count-practice):
            tap("上一步")
        page(practice, count+1)
        tap("我已实际进行这一步的练习")
        for _ in range(count-practice+1):
            tap("下一步")
    rate("对本次回应的相信程度", 0)
    rate("现在对原想法的相信程度", 90)
    screenshot("device-method-" + str(count) + "-v127.png")
    tap("完成本次探索")
    select_method(name)
    assert "方法记录" in text()
    find(f"v127-response-{count}")
    tap("返回方法列表")
    tap("保存并退出")
    result(name + "_merged_flow_saved_with_zero_response_belief")

select_method("现实暴露")
for number in range(1, 5):
    page(number, 7)
    enter("我的思考", "v127-exposure-note-" + str(number))
    tap("下一步")
page(5, 7)
assert not any(n.get("content-desc", "").startswith("AI：") for n in nodes())
enter("实际记录", "v127-exposure-observation")
save_method()
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
CACHE = None
tap(MARKER)
select_method("现实暴露")
page(5, 7)
assert find("实际记录", "content-desc").get("text") == "v127-exposure-observation"
assert find("我已实际进行这一步的练习").get("checked") == "false"
result("exposure_page_five_draft_survives_restart")
tap("我已实际进行这一步的练习")
rate("练习开始时的焦虑", 60)
rate("练习过程最高的焦虑", 75)
rate("练习结束时的焦虑", 65)
tap("下一步")
page(6, 7)
enter("本次回应", "v127-exposure-response")
tap("下一步")
rate("对本次回应的相信程度", 0)
rate("现在对原想法的相信程度", 90)
tap("保存本轮，继续下一轮练习")
page(1, 7)
assert "第 2 轮" in text()
find("回看已保存的练习轮次，已折叠", "content-desc")
for _ in range(4):
    tap("下一步")
page(5, 7)
assert find("实际记录", "content-desc").get("text") != "v127-exposure-observation"
assert find("我已实际进行这一步的练习").get("checked") == "false"
result("saved_round_retained_and_new_round_cleared")
save_method()

# Delete only the synthetic journal and confirm existing journal count is unchanged.
tap("返回日志列表", "content-desc")
tap("保存并退出")
find(MARKER)
tap(MARKER)
tap("返回日志列表", "content-desc")
tap("删除日志")
tap("删除")
assert baseline in text()
result("synthetic_log_removed_and_original_count_preserved")
