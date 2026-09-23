"""Synthetic-device check for wrapping multi-select chips and grouped AI reasons."""
import subprocess

def ui(*args):
    return subprocess.check_output(["python3","tools/device_ui.py",*args],text=True,stderr=subprocess.PIPE,timeout=30)

def reach(*args,up=False):
    for _ in range(16):
        try:return ui(*args)
        except subprocess.CalledProcessError as error:
            if "StopIteration" not in error.stderr:raise
            ui("up" if up else "scroll")
    raise AssertionError(str(args))

def checked(name,state):
    return any(line.startswith("ToggleButton "+name+" ") and line.endswith("true") for line in state.splitlines())

ui("tap","开始一篇情绪日志")
reach("input","当时的情境","Chip-test-v114:-My-colleague-pointed-out-an-error.")
ui("tap","下一步")
ui("tap","下一步")
reach("tap","添加一个想法")
reach("input","想法内容 1","I-always-fail-and-my-colleague-must-think-I-am-useless.")
reach("rate","此时对这句话的相信程度","60")
ui("tap","下一步")
reach("input","可能有什么益处？","Test-benefit")
reach("input","反映了哪些我重视的品质？","Test-value")
ui("tap","下一步")
ui("tap","下一步")
ui("desc","探索想法 1")
assert "CheckBox" not in ui("dump")
ui("tap","非黑即白")
ui("tap","以偏概全")
state=ui("dump")
assert checked("非黑即白",state) and checked("以偏概全",state)
ui("tap","非黑即白")
state=ui("dump")
assert not checked("非黑即白",state) and checked("以偏概全",state)
ui("desc","查看全部认知歪曲说明")
assert "只有完美和失败两个选项" in ui("dump")
reach("tap","自责或责怪他人")
assert "把复杂问题全部归咎" in ui("dump")
ui("tap","知道了")
assert checked("以偏概全",ui("dump"))
ui("desc","AI：识别认知歪曲")
for _ in range(65):
    state=ui("dump")
    if "取消 AI：" not in state:break
    if any(error in state for error in ("未写入","请求超时","无法获取")):raise AssertionError(state)
reach("tap","AI 识别与理由")
state=ui("dump")
assert "AI 识别与理由" in state
assert any(name in state for name in ("读心术","以偏概全","贴标签"))
print("Multi-select/deselect, combined help and grouped real AI reasons passed",flush=True)
