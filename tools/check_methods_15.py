"""Regression: one record per method, resume drafts, fail-fast switching."""
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


ui("tap","开始一篇情绪日志")
reach("input","当时的情境","Unique-method-test-v15")
ui("tap","下一步")
ui("tap","下一步")
reach("tap","添加一个想法")
reach("input","想法内容 1","Unique-method-thought")
reach("rate","此时对这句话的相信程度","70")
for _ in range(3):
    ui("tap","下一步")
ui("desc","探索想法 1")
reach("desc","检验证据")
reach("input","我的思考","Evidence-note-one")
ui("tap","下一步")
reach("input","我的思考","Evidence-note-two")
ui("tap","保存并退出")
ui("desc","探索想法 1")
reach("desc","检验证据")
assert "Evidence-note-two" in ui("dump")
assert "第 2 / 4 步" in ui("dump")
ui("tap","换个方法")
assert "暂未奏效" in ui("dump")
reach("desc","检验证据")
history=ui("dump")
assert all(value in history for value in ("Evidence-note-one","Evidence-note-two","暂未奏效","方法记录"))
ui("tap","返回方法列表")
reach("desc","双重标准法")
reach("input","我的思考","Friend-note")
ui("tap","保存并退出")
print("Draft resumed in place; no-help switch opens preserved record",flush=True)
subprocess.run(["adb",*(["-s",os.environ["ADB_SERIAL"]] if os.environ.get("ADB_SERIAL") else []),"shell","am","force-stop","cn.xinqing.journal"],check=True)
subprocess.run(["adb",*(["-s",os.environ["ADB_SERIAL"]] if os.environ.get("ADB_SERIAL") else []),"shell","am","start","-W","-n","cn.xinqing.journal/.MainActivity"],check=True,stdout=subprocess.DEVNULL)
ui("tap","Unique-method-test-v15")
ui("desc","探索想法 1")
reach("desc","双重标准法")
assert "Friend-note" in ui("dump")
for _ in range(3):
    ui("tap","下一步")
reach("input","本次回应","Friend-response")
reach("rate","对本次回应的相信程度","80")
reach("rate","现在对原想法的相信程度","30")
ui("tap","完成本次探索")
ui("desc","探索想法 1")
reach("desc","双重标准法")
assert "方法记录" in ui("dump") and "Friend-response" in ui("dump")
ui("tap","返回方法列表")
ui("tap","保存并退出")
ui("tap","下一步")
ui("tap","完成日志")
report=ui("dump")
assert report.count("检验证据 · 暂未奏效")==1
assert report.count("双重标准法 · 已记录")==1
assert "Evidence-note-one" in report and "Friend-response" in report
ui("tap","完成，返回日志列表")
ui("tap","Unique-method-test-v15")
reach("tap","删除日志")
ui("tap","删除")
assert "Unique-method-test-v15" not in ui("dump")
print("Restart and completion passed; exactly one record per method; test removed",flush=True)
