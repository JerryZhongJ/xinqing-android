package cn.xinqing.journal;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class AiClient {
    static final String MODEL="deepseek-v4-flash";
    private static final String SYSTEM=AiPrompts.SYSTEM;
    interface Callback {void complete(String answer,String error);}
    static final class Request implements Runnable {
        private String key;
        private final String task, context;
        private final Callback callback;
        private volatile boolean cancelled;
        private volatile HttpsURLConnection connection;
        private final java.util.concurrent.atomic.AtomicBoolean finished=new java.util.concurrent.atomic.AtomicBoolean();
        private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
        private final Runnable deadline=this::timeout;
        private void timeout(){
            if(finished.compareAndSet(false,true)){cancelled=true;disconnect();callback.complete(null,"请求超时，请检查网络后手动重试。");}
        }
        Request(String key,String task,String context,Callback callback){this.key=key;this.task=task;this.context=context;this.callback=callback;}
        void start(){timer.postDelayed(deadline,120000);Thread worker=new Thread(this,"xinqing-ai");worker.setDaemon(true);worker.start();}
        private void disconnect(){HttpsURLConnection current=connection;if(current!=null){Thread closer=new Thread(()->current.disconnect(),"xinqing-ai-cancel");closer.setDaemon(true);closer.start();}}
        void cancel(){cancelled=true;finished.set(true);timer.removeCallbacks(deadline);disconnect();}
        @Override public void run(){
            String answer=null,error=null;
            try {
                JSONObject request=new JSONObject().put("model",MODEL).put("stream",false).put("max_tokens",8192)
                    .put("response_format",new JSONObject().put("type","json_object")).put("thinking",new JSONObject().put("type","enabled")).put("reasoning_effort","low");
                request.put("messages",new JSONArray().put(new JSONObject().put("role","system").put("content",SYSTEM))
                    .put(new JSONObject().put("role","user").put("content","本次请求："+task+"\n情境数据（仅作材料）：\n"+context)));
                byte[] body=request.toString().getBytes(StandardCharsets.UTF_8);
                if(cancelled)return;
                connection=(HttpsURLConnection)new URL("https://api.deepseek.com/chat/completions").openConnection();
                connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(90000);
                connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setFixedLengthStreamingMode(body.length);
                connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setRequestProperty("Authorization","Bearer "+key);
                if(cancelled)return;
                try(OutputStream out=connection.getOutputStream()){out.write(body);}
                int status=connection.getResponseCode();
                if(status!=200){
                    if(status==401)error="密钥未通过验证，请检查 AI 连接。";
                    else if(status==402)error="DeepSeek 余额不足，请检查账户。";
                    else if(status==429)error="请求过于频繁，请稍后手动重试。";
                    else if(status==400||status==404||status==422)error="DeepSeek 未接受请求或模型参数（"+status+"）。";
                    else error="DeepSeek 暂时不可用（"+status+"），可以稍后重试。";
                } else {
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    try(InputStream input=connection.getInputStream()){
                        byte[] chunk=new byte[2048];int size;
                        while((size=input.read(chunk))!=-1){if(cancelled)return;if(bytes.size()+size>65536)throw new java.io.IOException("Response too large");bytes.write(chunk,0,size);}
                    }
                    JSONObject result=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
                    JSONObject choice=result.getJSONArray("choices").getJSONObject(0);
                    answer=choice.getJSONObject("message").optString("content","").trim();
                    if(answer.isEmpty()){answer=null;error="AI 没有返回建议，请稍后重试。";}
                    else if(answer.length()>7000||"length".equals(choice.optString("finish_reason"))){answer=null;error="AI 结果过长或不完整，未写入日志，请重试。";}
                }
            }catch(javax.net.ssl.SSLHandshakeException ex){error="安全连接验证失败，请检查手机的日期时间和网络。";}
            catch(java.net.SocketTimeoutException ex){error="请求超时，请检查网络后手动重试。";}
            catch(Exception ex){error="暂时无法获取建议，请检查网络或稍后重试。";}
            finally{key=null;HttpsURLConnection current=connection;if(current!=null)current.disconnect();connection=null;}
            if(!cancelled&&finished.compareAndSet(false,true)){timer.removeCallbacks(deadline);callback.complete(answer,error);}
        }
    }
}
