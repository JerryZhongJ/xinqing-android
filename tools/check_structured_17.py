"""Continue a synthetic log after the real AI thought-generation check."""
import subprocess
import sys


def ui(*args):
    return subprocess.check_output([sys.executable,"tools/device_ui.py",*args],text=True,stderr=subprocess.PIPE,timeout=30)


def reach(*args,up=False):
    for _ in range(15):
        try:
            return ui(*args)
        except subprocess.CalledProcessError as error:
            if "StopIteration" not in error.stderr:
                raise
            ui("up" if up else "scroll")
    raise AssertionError(str(args))


def wait_ai():
    for _ in range(24):
        state=ui("dump")
        if "AI 补充" not in state:
            return
        if any(s in state for s in ("未写入日志","请求超时","无法获取","未通过验证")):
            raise AssertionError(state)
    raise AssertionError("AI request did not finish")


def finish():
    reach("tap","修改回应")
    assert reach("value","本次回应").strip()
    ui("tap","下一步")
    reach("rate","对本次回应的相信程度","80")
    reach("rate","现在对原想法的相信程度","30")
    ui("tap","完成本次探索")
    ui("desc","返回日志列表")
    ui("tap","删除日志")
    ui("tap","删除")
    print("Reused response remains editable; synthetic log removed; all assertions passed")


if "--finish" in sys.argv:
    finish()
    sys.exit(0)

first=reach("value","想法内容 1").strip()
assert first and not any(s in first for s in ("是否","你可以","请确认"))
reach("rate","此时对这句话的相信程度","60")
ui("scroll")
second=reach("value","想法内容 2").strip()
assert second and second!=first
reach("rate","此时对这句话的相信程度","50","-1")
ui("tap","下一步")
reach("input","可能有什么益处？","Manual-benefit")
reach("desc","AI：正向重构",up=True)
wait_ai()
benefits=reach("value","可能有什么益处？")
assert benefits.startswith("Manual-benefit\n")
assert reach("value","反映了哪些你重视的品质？").strip()
ui("tap","下一步")
ui("tap","下一步")
ui("desc","探索想法 1")
assert "1 · 识别认知歪曲" in ui("dump")
ui("tap","非黑即白")
ui("desc","AI：识别认知歪曲")
wait_ai()
assert any("非黑即白" in line and line.endswith("true") for line in ui("dump").splitlines())
ui("tap","选择方法")
assert "2 · 选择方法" in ui("dump")
ui("desc","AI：选择方法")
wait_ai()
assert "AI 推荐：" in ui("dump")
print("Real AI: thoughts, append-only reframing, distortion selection and method ranking passed",flush=True)
reach("desc","灰度思考")
reach("input","我的思考","My-original-note")
reach("desc","AI：当前问题",up=True)
wait_ai()
note=reach("value","我的思考")
assert note.startswith("My-original-note\n")
ui("tap","下一步")
reach("input","我的思考","Some-parts-were-correct")
ui("tap","下一步")
response=reach("value","本次回应").strip()
assert response and not any(s in response for s in ("你可以","试着","是否贴切"))
ui("tap","下一步")
state=ui("dump")
assert response in state and "EditText" not in state
print("Real gray-method AI generates a response; final step reuses it without retyping",flush=True)
finish()
