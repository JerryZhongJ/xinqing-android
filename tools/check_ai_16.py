"""UI regression on synthetic data; explicitly requests AI and tests cancel."""
import subprocess
import sys


def ui(*args):
    return subprocess.check_output([sys.executable,"tools/device_ui.py",*args],text=True,stderr=subprocess.PIPE,timeout=30)


def reach(*args):
    for _ in range(15):
        try:
            return ui(*args)
        except subprocess.CalledProcessError as error:
            if "StopIteration" not in error.stderr:
                raise
            ui("scroll")
    raise AssertionError(str(args))


ui("tap","AI-test-v16")
reach("tap","AI 帮我想想 · 理清情境")
assert "AI · 理清情境" in ui("dump")
ui("tap","关闭")
assert "AI-test-v16" in ui("dump")
ui("tap","下一步")
ui("tap","下一步")
reach("tap","添加一个想法")
reach("input","想法内容 1","I-always-fail")
reach("rate","此时对这句话的相信程度","60")
ui("tap","下一步")
ui("tap","AI 帮我想想 · 正向重构")
assert "AI · 正向重构" in ui("dump")
ui("tap","关闭")
ui("tap","下一步")
ui("tap","下一步")
ui("desc","探索想法 1")
menu=ui("dump")
assert "AI 帮我想想 · 选择方法" in menu
assert "AI 帮我想想 · 识别认知歪曲" in menu
reach("desc","检验证据")
assert "AI 帮我想想 · 当前问题" in ui("dump")
for _ in range(3):
    ui("tap","下一步")
assert "AI 帮我想想 · 形成回应" in ui("dump")
reach("input","本次回应","My-own-response")
reach("tap","AI 帮我想想 · 形成回应")
ui("tap","关闭")
assert "My-own-response" in ui("dump")
ui("tap","保存并退出")
ui("desc","返回日志列表")
ui("tap","删除日志")
ui("tap","删除")
assert "AI-test-v16" not in ui("dump")
print("Explicit AI entry points, empty reframing, cancel, unchanged user text passed; test removed")
