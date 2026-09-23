"""Real API regression with a synthetic lost-umbrella scenario."""
import subprocess
import sys

def ui(*args):
    return subprocess.check_output([sys.executable,"tools/device_ui.py",*args],text=True,stderr=subprocess.PIPE,timeout=30)

def reach(*args,up=False):
    for _ in range(18):
        try:return ui(*args)
        except subprocess.CalledProcessError as error:
            if "StopIteration" not in error.stderr:raise
            ui("up" if up else "scroll")
    raise AssertionError(str(args))

def ai(task):
    reach("desc","AI："+task)
    for _ in range(25):
        state=ui("dump")
        assert "TextView AI 补充" not in state,"Unexpected waiting dialog"
        if any(s in state for s in ("未写入","请求超时","无法获取","未通过验证")):raise AssertionError(state)
        if "取消 AI：" not in state:return
    raise AssertionError("AI did not finish")

ui("tap","开始一篇情绪日志")
reach("input","当时的情境","Umbrella-v18-test:-I-lost-my-umbrella.")
ui("tap","下一步")
assert "悲伤 ·" not in ui("dump")
reach("rate","悲伤程度","70")
ui("tap","下一步")
reach("tap","添加一个想法")
reach("input","想法内容 1","I-always-lose-my-things")
reach("rate","此时对这句话的相信程度","70")
ui("tap","下一步")
assert "暂时想不到" not in ui("dump")
ai("重构益处")
benefits=reach("value","可能有什么益处？").strip()
assert benefits and not any(s in benefits for s in ("丢伞让我","不等于","不代表","安顿"))
reach("desc","AI：重构品质")
for _ in range(25):
    state=ui("dump")
    if "取消 AI：" not in state:break
values=reach("value","反映了哪些我重视的品质？").strip()
assert values and not any(s in values for s in ("不等于","不代表","安顿","哪怕","生命"))
print("Benefits:",benefits,"\nValues:",values,flush=True)
ui("tap","下一步")
state=ui("dump")
assert "最初 70" not in state and "70%" not in state
reach("rate","悲伤期望程度","20")
ui("tap","下一步")
ui("desc","探索想法 1")
ui("tap","以偏概全")
reach("tap","贴标签")
ui("tap","继续")
state=ui("dump")
assert "定义用词" in state and "参考匹配" not in state
reach("desc","双重标准法")
assert "开始时对原想法" not in ui("dump")
ai("当前问题")
friend=reach("value","我的思考").strip()
assert "你" in friend and not friend.startswith("我")
print("To friend:",friend,flush=True)
ui("tap","下一步")
ai("当前问题")
comparison=reach("value","我的思考").strip()
assert comparison and "我" in comparison
print("Comparison:",comparison,flush=True)
ui("tap","下一步")
ai("形成回应")
response=reach("value","本次回应").strip()
assert response and "我" in response
print("Response:",response,flush=True)
ui("tap","下一步")
state=ui("dump")
assert "Button 保存并退出" not in state and "Button 换个方法" not in state
assert "练习操作" in state
reach("rate","对本次回应的相信程度","80")
reach("rate","现在对原想法的相信程度","30")
ui("tap","完成本次探索")
state=ui("dump")
assert "30%" in state and "70%" not in state
ui("desc","返回日志列表")
ui("tap","删除日志")
ui("tap","删除")
print("Inline AI, roles, grounded reframing, local ranking, compact actions and new score passed; test removed")
