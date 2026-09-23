package cn.xinqing.journal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONTokener;
import java.util.Arrays;
import java.util.HashSet;

/** Validate a complete model result before applying any changes to a journal. */
final class AiSuggestions {
    static final String[] DISTORTIONS={"非黑即白","以偏概全","心理过滤","否定积极","读心术","预言未来","夸大或缩小","情绪化推理","应该思维","贴标签","自责或责怪他人"};
    static String instruction(String task) { return AiPrompts.instruction(task); }
    private static String text(JSONObject object,String key,int limit) throws JSONException {
        Object value=object.get(key);if(!(value instanceof String))throw new JSONException("Expected text");
        String result=((String)value).trim();if(result.isEmpty()||result.length()>limit)throw new JSONException("Invalid text length");return result;
    }
    private static JSONArray strings(JSONObject object,String key,int count,int length) throws JSONException {
        JSONArray input=object.getJSONArray(key),result=new JSONArray();if(input.length()>count)throw new JSONException("Too many items");
        HashSet<String> seen=new HashSet<>();for(int i=0;i<input.length();i++){Object value=input.get(i);if(!(value instanceof String))throw new JSONException("Expected text item");String s=((String)value).trim();if(s.isEmpty()||s.length()>length)throw new JSONException("Invalid item");if(seen.add(s))result.put(s);}return result;
    }
    static JSONObject parse(String task,String raw) throws JSONException {
        if(raw==null||raw.length()>7000)throw new JSONException("Invalid result size");
        JSONTokener tokens=new JSONTokener(raw);Object root=tokens.nextValue();
        if(!(root instanceof JSONObject)||tokens.nextClean()!=0)throw new JSONException("Expected one JSON object");
        JSONObject input=(JSONObject)root,result=new JSONObject();
        if(input.has("support"))return result.put("support",text(input,"support",300));
        if(task.equals("寻找想法"))result.put("thoughts",strings(input,"thoughts",2,100));
        else if(task.equals("重构益处"))result.put("benefits",strings(input,"benefits",2,100));
        else if(task.equals("重构品质"))result.put("values",strings(input,"values",2,80));
        else if(task.equals("正向重构")){result.put("benefits",strings(input,"benefits",2,120));result.put("values",strings(input,"values",2,120));}
        else if(task.equals("识别认知歪曲")||task.equals("选择方法")){
            boolean distort=task.equals("识别认知歪曲");String key=distort?"distortions":"methods",id=distort?"name":"id";
            JSONArray inputItems=input.getJSONArray(key),items=new JSONArray();if(inputItems.length()>3)throw new JSONException("Too many choices");
            HashSet<String> seen=new HashSet<>();for(int i=0;i<inputItems.length();i++){
                JSONObject item=inputItems.getJSONObject(i);String name=text(item,id,50),reason=text(item,"reason",150);
                if(distort?!Arrays.asList(DISTORTIONS).contains(name):ThoughtMethods.find(name)==null)throw new JSONException("Unknown choice");
                if(seen.add(name))items.put(new JSONObject().put(id,name).put("reason",reason));
            }result.put(key,items);
        } else if(task.equals("当前问题"))result.put("note",text(input,"note",240));
        else if(task.equals("形成回应"))result.put("response",text(input,"response",300));
        else throw new JSONException("Unknown task");
        return result;
    }
    static void append(JSONObject target,String key,String addition) throws JSONException {
        String previous=target.optString(key,"");String extra=addition.trim();if(extra.isEmpty())return;
        for(String line:previous.split("\\n"))if(line.trim().equals(extra))return;
        target.put(key,previous.isEmpty()?extra:previous+(previous.endsWith("\n")?"":"\n")+extra);
    }
    static String describe(String task,JSONObject data) throws JSONException {
        StringBuilder result=new StringBuilder();
        if(task.equals("寻找想法")){JSONArray a=data.getJSONArray("thoughts");for(int i=0;i<a.length();i++)result.append(a.getString(i)).append("\n");}
        else if(task.equals("重构益处")||task.equals("重构品质")){JSONArray a=data.getJSONArray(task.equals("重构益处")?"benefits":"values");for(int i=0;i<a.length();i++)result.append(a.getString(i)).append("\n");}
        else if(task.equals("正向重构")){for(String key:new String[]{"benefits","values"}){result.append(key.equals("benefits")?"益处\n":"品质\n");JSONArray a=data.getJSONArray(key);for(int i=0;i<a.length();i++)result.append(a.getString(i)).append("\n");}}
        else if(task.equals("选择方法")||task.equals("识别认知歪曲")){boolean method=task.equals("选择方法");JSONArray a=data.getJSONArray(method?"methods":"distortions");for(int i=0;i<a.length();i++){JSONObject item=a.getJSONObject(i);result.append(method?ThoughtMethods.find(item.getString("id")).name:item.getString("name")).append("：").append(item.getString("reason")).append("\n");}}
        else{if(data.has("note"))result.append(data.getString("note"));if(data.has("response"))result.append(data.getString("response"));}
        return result.toString().trim();
    }
    static int apply(String task,JSONObject data,JSONObject entry,JSONObject thought,JSONObject attempt,int page) throws JSONException {
        int changes=0;
        if(task.equals("寻找想法")){
            JSONArray existing=entry.getJSONArray("thoughts"),items=data.getJSONArray("thoughts");
            for(int i=0;i<items.length();i++){String value=items.getString(i);boolean found=false;for(int j=0;j<existing.length();j++)if(existing.getJSONObject(j).optString("text").trim().equals(value))found=true;
                if(!found){existing.put(new JSONObject().put("text",value).put("source","ai"));changes++;}}
        } else if(task.equals("重构益处")||task.equals("重构品质")){
            String key=task.equals("重构益处")?"benefits":"values";JSONArray items=data.getJSONArray(key);for(int i=0;i<items.length();i++){append(entry,key,items.getString(i));changes++;}
        } else if(task.equals("正向重构")){
            for(String key:new String[]{"benefits","values"}){JSONArray items=data.getJSONArray(key);String before=entry.optString(key);for(int i=0;i<items.length();i++)append(entry,key,items.getString(i));if(!before.equals(entry.optString(key))){entry.remove(key+"Deferred");changes++;}}
        } else if(task.equals("识别认知歪曲")){
            JSONObject selected=thought.optJSONObject("distortions"),reasons=thought.optJSONObject("distortionReasons");
            if(selected==null){selected=new JSONObject();thought.put("distortions",selected);}if(reasons==null){reasons=new JSONObject();thought.put("distortionReasons",reasons);}
            JSONArray items=data.getJSONArray("distortions");for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);String name=item.getString("name");selected.put(name,true);append(reasons,name,item.getString("reason"));changes++;}
            if(changes>0)thought.remove("recommendations");
        } else if(task.equals("选择方法")){
            JSONArray input=data.getJSONArray("methods"),kept=new JSONArray(),used=thought.optJSONArray("attempts");
            for(int i=0;i<input.length();i++){JSONObject item=input.getJSONObject(i);boolean tried=false;if(used!=null)for(int j=0;j<used.length();j++)if(item.getString("id").equals(used.getJSONObject(j).optString("method")))tried=true;if(!tried)kept.put(item);}
            thought.put("recommendations",kept);changes=kept.length();
        } else {
            if(task.equals("当前问题")){append(attempt,"note"+page,data.getString("note"));changes++;}
            else {append(attempt,"response",data.getString("response"));changes++;}
        }
        return changes;
    }
}
