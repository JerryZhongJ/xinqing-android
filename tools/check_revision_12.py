"""Device regression for exit actions, thought dialogs, and completion."""
import subprocess
import os
import sys


def ui(*args):
    result = subprocess.check_output([sys.executable, "tools/device_ui.py", *args], text=True, timeout=30)
    return result


def adb(*args):
    subprocess.run(["adb", *(["-s",os.environ["ADB_SERIAL"]] if os.environ.get("ADB_SERIAL") else []), "shell", *args], check=True, timeout=20)


if "--resume" not in sys.argv:
    ui("tap", "开始一篇情绪日志")
    ui("input", "当时的情境", "UI-test-v1.2")
    ui("desc", "返回日志列表")
    menu = ui("dump")
    assert all(label in menu for label in ("保存并退出", "继续填写", "删除日志"))
    ui("tap", "继续填写")
    assert "UI-test-v1.2" in ui("dump")
    ui("tap", "下一步")
    ui("tap", "下一步")
    ui("tap", "添加一个想法")
    assert "移除想法 1" in ui("dump")
    ui("input", "想法内容 1", "First-v12")
    ui("rate", "当时相信程度", "60")
    ui("tap", "添加一个想法")
    ui("input", "想法内容 2", "Second-v12")
    ui("scroll")
    print("Exit menu and remove icon passed", flush=True)
ui("rate", "当时相信程度", "70", "-1")
ui("tap", "下一步")
ui("input", "这些情绪或想法，有什么好处？", "Benefit-v12")
ui("scroll")
ui("input", "它们反映了哪些珍贵的品质？", "Values-v12")
ui("tap", "下一步")
ui("tap", "下一步")
assert "First-v12" in ui("dump")
ui("desc", "应对想法 1")
ui("tap", "非黑即白")
ui("tap", "完成应对")
assert "完成应对" in ui("dump")
for _ in range(5):
    if "EditText" in ui("dump") and "更平衡的回应" in ui("dump"):
        break
    ui("scroll")
ui("input", "更平衡的回应", "Response-v12")
ui("scroll")
ui("rate", "对新回应的相信程度", "90")
ui("tap", "完成应对")
assert "想法 1  ·  已应对" in ui("dump")
ui("desc", "应对想法 2")
ui("tap", "以偏概全")
ui("tap", "稍后继续")
assert "想法 2  ·  待应对" in ui("dump")
ui("desc", "应对想法 1")
first = ui("dump")
assert any("非黑即白" in line and line.endswith("true") for line in first.splitlines())
ui("tap", "稍后继续")
ui("desc", "应对想法 2")
second = ui("dump")
assert any("以偏概全" in line and line.endswith("true") for line in second.splitlines())
ui("tap", "稍后继续")
print("Thought dialogs, completion, validation, independent drafts passed", flush=True)
adb("input", "keyevent", "KEYCODE_BACK")
ui("tap", "保存并退出")
ui("tap", "UI-test-v1.2")
assert "Second-v12" in ui("dump")
ui("tap", "下一步")
ui("rate", "现在对原想法的相信程度", "30")
ui("tap", "完成日志")
ui("tap", "完成，返回日志列表")
assert "UI-test-v1.2" in ui("dump")
print("Save/resume and completion return passed", flush=True)
ui("tap", "UI-test-v1.2")
ui("scroll")
ui("scroll")
ui("tap", "删除日志")
ui("tap", "删除")
assert "UI-test-v1.2" not in ui("dump")
print("Test record removed; all assertions passed", flush=True)
