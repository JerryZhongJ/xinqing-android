"""Device check: disclosures preserve input, optional reframing, draft labels."""
import subprocess
import sys


def ui(*args):
    return subprocess.check_output([sys.executable, "tools/device_ui.py", *args], text=True, stderr=subprocess.PIPE, timeout=30)


def reach(*args):
    for _ in range(10):
        try:
            return ui(*args)
        except subprocess.CalledProcessError as error:
            if "StopIteration" not in error.stderr:
                raise
            ui("scroll")
    raise AssertionError("Control not reached: " + str(args))


ui("tap", "开始一篇情绪日志")
reach("input", "当时的情境", "Guidance-test-v13")
ui("up")
ui("tap", "+  看看例子：事情与评价")
assert "老师让你重做一道题" in ui("dump")
ui("tap", "−  看看例子：事情与评价")
assert "Guidance-test-v13" in ui("dump")
ui("tap", "下一步")
ui("tap", "下一步")
reach("tap", "添加一个想法")
reach("input", "想法内容 1", "Thought-v13")
reach("rate", "此时对这句话的相信程度", "60")
ui("tap", "下一步")
assert "想不到可以留空" in ui("dump")
ui("tap", "+  想不到作用或品质时")
assert "你害怕失去什么" in ui("dump")
ui("tap", "−  想不到作用或品质时")
ui("tap", "下一步")
assert "不是现在已经达到" in ui("dump")
ui("tap", "下一步")
ui("desc", "探索想法 1")
reach("tap", "非黑即白")
ui("tap", "稍后继续")
assert "探索中" in ui("dump")
ui("desc", "探索想法 1")
print("Disclosures, preserved input, optional reframing, draft status passed", flush=True)
reach("tap", "+  想不到回应时：换成朋友视角")
ui("tap", "−  想不到回应时：换成朋友视角")
reach("input", "更平衡的回应", "Response-v13")
reach("rate", "对新回应的相信程度", "0")
ui("tap", "完成本次探索")
assert "已探索" in ui("dump")
ui("tap", "下一步")
reach("rate", "现在对原想法的相信程度", "60")
ui("tap", "完成日志")
assert "正向重构" in ui("dump")
assert "认知歪曲" in ui("dump")
ui("tap", "完成，返回日志列表")
ui("tap", "Guidance-test-v13")
reach("tap", "删除日志")
ui("tap", "删除")
assert "Guidance-test-v13" not in ui("dump")
print("Zero belief saved, completion and report labels passed; test record removed", flush=True)
