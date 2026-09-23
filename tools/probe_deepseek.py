"""One synthetic API request; key is read without echo and never saved."""
import getpass
import json
import urllib.request
import urllib.error

key=getpass.getpass("DeepSeek API Key (hidden): ")
payload={"model":"deepseek-v4-flash","thinking":{"type":"enabled"},"reasoning_effort":"low","max_tokens":8192,"stream":False,
         "messages":[{"role":"system","content":"请用中文简短回答，最多两句话。"},
                     {"role":"user","content":"这是虚构的连通性测试：我想写情绪日志但不知道从哪里开始，请给我一个起步问题。"}]}
request=urllib.request.Request("https://api.deepseek.com/chat/completions",data=json.dumps(payload).encode(),
    headers={"Authorization":"Bearer "+key,"Content-Type":"application/json"})
try:
    with urllib.request.urlopen(request,timeout=120) as response:
        result=json.load(response)
    text=result["choices"][0]["message"]["content"]
    print("HTTP 200; model:",result.get("model"),"; response characters:",len(text))
    print(text)
except urllib.error.HTTPError as error:
    print("HTTP error:",error.code)
except Exception as error:
    print("Connection error:",type(error).__name__)
finally:
    key=None
