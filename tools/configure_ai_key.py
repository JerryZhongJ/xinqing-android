"""Configure the foreground AI connection dialog without saving a key to disk."""
import getpass
import os
import re
import subprocess


def adb(*args):
    return subprocess.check_output(["adb",*(["-s",os.environ["ADB_SERIAL"]] if os.environ.get("ADB_SERIAL") else []),*args],timeout=20)


def center(tag):
    x1,y1,x2,y2=map(int,re.findall(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",tag)[0])
    return str((x1+x2)//2),str((y1+y2)//2)


# Locate controls before a credential is entered; never dump the populated field.
adb("shell","uiautomator","dump","/data/local/tmp/xinqing-key-form.xml")
field=adb("shell", "grep -o 'class=\"android.widget.EditText\"[^>]*' /data/local/tmp/xinqing-key-form.xml").decode()
if 'package="cn.xinqing.journal"' not in field:
    raise SystemExit("AI configuration field not found")
key=getpass.getpass("DeepSeek API Key (hidden): ")
if not re.fullmatch(r"sk-[A-Za-z0-9_-]{12,}",key):
    raise SystemExit("Invalid key format")
adb("shell","input","tap",*center(field))
adb("shell","input","text",key)
key=None
adb("shell","uiautomator","dump","/data/local/tmp/xinqing-key-form.xml")
# Filter on-device: only the save button is returned, never the credential field.
save_tag=adb("shell", "grep -o 'resource-id=\"android:id/button1\"[^>]*' /data/local/tmp/xinqing-key-form.xml").decode()
adb("shell","input","tap",*center(save_tag))
adb("shell","rm","-f","/data/local/tmp/xinqing-key-form.xml","/data/local/tmp/xinqing-key-check.xml")
print("Credential submitted to the app; no local credential file was created.")
