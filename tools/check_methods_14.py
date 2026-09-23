"""Regression: separate method attempts, resume, immutable history, report."""
import subprocess
import os
import sys


def ui(*args):
    return subprocess.check_output([sys.executable,"tools/device_ui.py",*args],text=True,stderr=subprocess.PIPE,timeout=30)


def reach(*args):
    for _ in range(20):
        try:
            return ui(*args)
        except subprocess.CalledProcessError as error:
            if "StopIteration" not in error.stderr:
                raise
            ui("scroll")
    raise AssertionError(str(args))


if "--resume" not in sys.argv:
    ui("tap","开始一篇情绪日志")
    reach("input","当时的情境","Methods-test-v14")
    ui("tap","下一步")
    ui("tap","下一步")
    reach("tap","添加一个想法")
    reach("input","想法内容 1","Method-test-thought")
    reach("rate","此时对这句话的相信程度","70")
    ui("tap","下一步")
    ui("tap","下一步")
    ui("tap","下一步")
    ui("desc","探索想法 1")
    reach("desc","检验证据")
    for note in ("Evidence-first","Evidence-second","Evidence-third"):
        reach("input","我的思考",note)
        ui("tap","继续")
    reach("input","本次回应","First-response")
    reach("rate","对本次回应的相信程度","90")
reach("rate","现在对原想法的相信程度","40")
ui("tap","完成本次探索")
assert "已探索" in ui("dump")
print("Evidence method completed",flush=True)
ui("desc","探索想法 1")
reach("desc","双重标准法")
reach("input","我的思考","Friend-draft")
ui("tap","换个方法")
reach("desc","语义法")
reach("input","我的思考","Semantic-draft")
ui("tap","稍后继续")
assert "有草稿" in ui("dump")
subprocess.run(["adb",*(["-s",os.environ["ADB_SERIAL"]] if os.environ.get("ADB_SERIAL") else []),"shell","am","force-stop","cn.xinqing.journal"],check=True)
subprocess.run(["adb",*(["-s",os.environ["ADB_SERIAL"]] if os.environ.get("ADB_SERIAL") else []),"shell","am","start","-W","-n","cn.xinqing.journal/.MainActivity"],check=True,stdout=subprocess.DEVNULL)
ui("tap","Methods-test-v14")
ui("desc","探索想法 1")
reach("desc","尝试 2 · 双重标准法")
assert "Friend-draft" in ui("dump")
ui("tap","继续")
reach("input","我的思考","Friend-second")
ui("tap","继续")
reach("input","我的思考","Friend-third")
ui("tap","继续")
reach("input","本次回应","Second-response")
reach("rate","对本次回应的相信程度","0")
reach("rate","现在对原想法的相信程度","20")
ui("tap","完成本次探索")
ui("desc","探索想法 1")
reach("desc","尝试 1 · 检验证据")
history=ui("dump")
assert "First-response" in history and "Evidence-first" in history and "Second-response" not in history
ui("tap","返回方法列表")
reach("desc","尝试 3 · 语义法")
assert "Semantic-draft" in ui("dump")
ui("tap","稍后继续")
print("Switch, restart, resume and independent history passed",flush=True)
ui("tap","下一步")
ui("tap","完成日志")
report=ui("dump")
assert all(value in report for value in ("First-response","Second-response","Friend-draft","Semantic-draft","尝试记录"))
ui("tap","完成，返回日志列表")
ui("tap","Methods-test-v14")
reach("tap","删除日志")
ui("tap","删除")
assert "Methods-test-v14" not in ui("dump")
print("Report contains all attempts; test record removed; all assertions passed",flush=True)
