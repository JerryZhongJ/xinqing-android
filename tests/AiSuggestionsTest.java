package cn.xinqing.journal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public final class AiSuggestionsTest {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
    private static JSONObject result(String task,String json)throws Exception{return AiSuggestions.parse(task,json);}
    private static void invalid(String task,String json)throws Exception{try{result(task,json);throw new AssertionError("Accepted invalid result");}catch(JSONException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        JSONObject entry=new JSONObject().put("thoughts",new JSONArray().put(new JSONObject().put("text","已有想法").put("before",65))).put("benefits","我的原文  ").put("values","我在意认真");
        JSONObject thoughts=result("寻找想法","{\"thoughts\":[\"我怕做不好\",\"我会被否定\"],\"before\":100}");
        AiSuggestions.apply("寻找想法",thoughts,entry,null,null,0);
        check(entry.getJSONArray("thoughts").length()==3,"append thoughts");
        check(entry.getJSONArray("thoughts").getJSONObject(0).getInt("before")==65,"existing score preserved");
        check(!entry.getJSONArray("thoughts").getJSONObject(1).has("before"),"AI cannot rate");
        AiSuggestions.apply("寻找想法",thoughts,entry,null,null,0);check(entry.getJSONArray("thoughts").length()==3,"deduplicate thoughts");
        JSONObject reframe=result("正向重构","{\"benefits\":[\"我会提前准备\"],\"values\":[\"我在意认真\",\"我愿意负责\"]}");
        AiSuggestions.apply("正向重构",reframe,entry,null,null,0);
        check(entry.getString("benefits").equals("我的原文  \n我会提前准备"),"preserve original whitespace and append");
        check(entry.getString("values").equals("我在意认真\n我愿意负责"),"deduplicate existing lines");
        JSONObject thought=new JSONObject().put("distortions",new JSONObject().put("读心术",true));
        JSONObject distort=result("识别认知歪曲","{\"distortions\":[{\"name\":\"非黑即白\",\"reason\":\"把结果分成两个极端\"}]}");
        AiSuggestions.apply("识别认知歪曲",distort,entry,thought,null,0);
        check(thought.getJSONObject("distortions").getBoolean("读心术"),"manual distortion retained");
        check(thought.getJSONObject("distortions").getBoolean("非黑即白"),"AI distortion added");
        thought.put("attempts",new JSONArray().put(new JSONObject().put("method","evidence").put("noHelp",true)));
        JSONObject methods=result("选择方法","{\"methods\":[{\"id\":\"gray\",\"reason\":\"适合两个极端的判断\"},{\"id\":\"evidence\",\"reason\":\"检查事实\"}]}");
        AiSuggestions.apply("选择方法",methods,entry,thought,null,0);
        check(thought.getJSONArray("recommendations").length()==1,"used methods filtered");
        check(thought.getJSONArray("recommendations").getJSONObject(0).getString("id").equals("gray"),"ranking preserved");
        JSONObject attempt=new JSONObject().put("note0","我的笔记").put("response","我的回应").put("after",60);
        JSONObject note=result("当前问题","{\"note\":\"有其他中间结果\",\"response\":\"我可以有不足而非全盘失败\",\"after\":0}");
        AiSuggestions.apply("当前问题",note,entry,thought,attempt,0);
        check(attempt.getString("note0").equals("我的笔记\n有其他中间结果"),"notes appended");
        check(attempt.getString("response").equals("我的回应"),"current question cannot fill a future response");
        AiSuggestions.apply("形成回应",result("形成回应","{\"response\":\"我可以有不足而非全盘失败\"}"),entry,thought,attempt,2);
        check(attempt.getString("response").equals("我的回应\n我可以有不足而非全盘失败"),"final response appended");
        check(attempt.getInt("after")==60,"rating untouched");
        String before=entry.toString();
        invalid("寻找想法","not json");invalid("寻找想法","{\"thoughts\":[3]}");invalid("正向重构","{\"benefits\":[\"abc\"]}");
        invalid("识别认知歪曲","{\"distortions\":[{\"name\":\"非黑即白\",\"reason\":\"ok\"},{\"name\":\"未知类型\",\"reason\":\"bad\"}]}");
        invalid("选择方法","{\"methods\":[{\"id\":\"invented\",\"reason\":\"bad\"}]}");
        invalid("形成回应","{\"response\":\"\"}");
        invalid("形成回应","{\"response\":\"abc\"} extra text");
        check(before.equals(entry.toString()),"invalid results do not mutate records");
        JSONObject positive=result("重构益处","{\"benefits\":[\"难过提醒我留意遗失的东西\"],\"values\":[\"不能写入另一个字段\"]}");
        String oldValues=entry.getString("values");AiSuggestions.apply("重构益处",positive,entry,null,null,0);
        check(entry.getString("values").equals(oldValues),"benefit request cannot change values");
        JSONObject feeling=new JSONObject().put("悲伤",new JSONObject().put("before",70));entry.put("emotions",feeling).put("event","我丢了伞");
        JSONObject previous=new JSONObject().put("method","double_standard").put("thought","我总是丢三落四").put("note0","你只是忘了拿伞，可以回去找找。").put("note1","我对自己比对朋友苛刻").put("response","我可以承认失误，不给自己贴标签");
        JSONObject context=AiPrompts.context("当前问题",entry,thought,previous,1);
        check(context.getJSONArray("前面的问题与回答").getJSONObject(0).getString("回答").equals(previous.getString("note0")),"previous friend answer included verbatim");
        check(context.getJSONArray("前面的问题与回答").getJSONObject(0).has("问题"),"answers paired with questions");
        check(!context.has("相信程度"),"no unnecessary scores in context");
        check(context.getString("当前问题").equals(ThoughtMethods.find("double_standard").questions[1]),"current question determines the step and role");
        check(context.getString("方法目的").equals(ThoughtMethods.find("double_standard").purpose),"method purpose included with shared instruction");
        JSONObject all=AiPrompts.context("形成回应",entry,thought,previous,3);
        check(all.getJSONArray("前面的问题与回答").length()==3,"all earlier steps included at final");
        check(AiPrompts.context("重构益处",entry,null,null,0).getJSONArray("情绪参考线索").length()==1,"selected emotion reference included");
        JSONObject chosen=new JSONObject().put("非黑即白",true);JSONObject ranked=new JSONObject().put("distortions",chosen);
        check(CbtReferences.ranked(ranked).get(0).id.equals("gray"),"gray leads all-or-nothing default ranking");
        check(CbtReferences.score("evidence",new JSONObject().put("心理过滤",true).put("情绪化推理",true))==4,"matching scores accumulate");
        System.out.println(checks+" structured AI assertions passed");
    }
}
