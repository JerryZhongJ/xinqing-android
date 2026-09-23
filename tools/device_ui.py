"""Small adb UI helper. Operates only on the foreground XinQing app."""
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *(["-s", os.environ["ADB_SERIAL"]] if os.environ.get("ADB_SERIAL") else []), *args], timeout=20)


def nodes():
    adb("shell", "uiautomator", "dump", "/data/local/tmp/xinqing-ui.xml")
    root = ET.fromstring(adb("shell", "cat", "/data/local/tmp/xinqing-ui.xml"))
    if not any(n.get("package") == "cn.xinqing.journal" for n in root.iter("node")):
        raise RuntimeError("XinQing is not in the foreground")
    return list(root.iter("node"))


def bounds(node):
    return list(map(int, re.findall(r"\d+", node.get("bounds"))))


def tap(node):
    x1, y1, x2, y2 = bounds(node)
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


command = sys.argv[1]
items = nodes()
if command == "dump":
    for n in items:
        if n.get("text") or n.get("content-desc"):
            print(n.get("class").split(".")[-1], n.get("text"), n.get("content-desc"), n.get("bounds"), n.get("checked"))
elif command in ("tap", "desc"):
    key = "text" if command == "tap" else "content-desc"
    tap(next(n for n in items if n.get(key) == sys.argv[2]))
elif command == "input":
    tap(next(n for n in items if n.get("class") == "android.widget.EditText" and n.get("content-desc") == sys.argv[2]))
    adb("shell", "input", "text", sys.argv[3])
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
elif command == "value":
    n=next(n for n in items if n.get("class")=="android.widget.EditText" and n.get("content-desc")==sys.argv[2])
    if n.get("password")=="true":
        raise RuntimeError("Credential fields must not be read")
    print(n.get("text"))
elif command == "rate":
    matches = [n for n in items if n.get("class") == "android.widget.SeekBar" and n.get("content-desc") == sys.argv[2]]
    if not matches:
        raise StopIteration("Rating is outside the visible scroll area")
    n = matches[int(sys.argv[4]) if len(sys.argv)>4 else 0]
    x1, y1, x2, y2 = bounds(n)
    x = x1 + 56 + round((x2-x1-112)*int(sys.argv[3])/100)
    adb("shell", "input", "tap", str(x), str((y1+y2)//2))
elif command in ("scroll", "up"):
    n = next(n for n in items if n.get("class") == "android.widget.ScrollView")
    x1,y1,x2,y2 = bounds(n)
    x = (x1+x2)//2
    top = y1+(y2-y1)//6
    bottom = y2-(y2-y1)//6
    start,end = (bottom,top) if command == "scroll" else (top,bottom)
    adb("shell", "input", "swipe", str(x),str(start),str(x),str(end),"350")
