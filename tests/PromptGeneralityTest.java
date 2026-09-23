package cn.xinqing.journal;

import org.json.JSONArray;
import org.json.JSONObject;

/** Cases belong to tests, not to shared instructions sent with every request. */
public final class PromptGeneralityTest {
    static final String[][] CASES={
        {"我丢了伞","悲伤","我总是丢三落四"},
        {"报告里有一个数字填错了，同事让我修改","焦虑","同事一定觉得我能力不行"},
        {"朋友临时取消了约定","愤怒","他根本不重视我"},
        {"一位亲人去世了","悲伤","我以后不会再快乐了"}
    };
    public static void main(String[] args)throws Exception{
        int checks=0;
        for(String task:AiPrompts.TASKS){
            String prompt=AiPrompts.SYSTEM+AiPrompts.instruction(task);
            for(String[] example:CASES){if(prompt.contains(example[0])||prompt.contains(example[2]))throw new AssertionError("Case leaked into instruction");checks++;}
            for(String blacklist:new String[]{"哪怕只是","安顿生活","热爱生命","把自己定死"}){if(prompt.contains(blacklist))throw new AssertionError("Lexical blacklist in shared instructions");checks++;}
        }
        if(!AiPrompts.instruction("重构品质").contains("品质本身"))throw new AssertionError("Value answer must name a quality");
        if(ThoughtMethods.find("double_standard").questions[2].contains("不认同"))throw new AssertionError("Instruction leaked into question");
        checks+=2;
        for(String[] example:CASES){
            JSONObject thought=new JSONObject().put("text",example[2]).put("distortions",new JSONObject());
            JSONObject entry=new JSONObject().put("event",example[0]).put("thoughts",new JSONArray().put(thought))
                .put("emotions",new JSONObject().put(example[1],new JSONObject().put("before",60)));
            JSONObject attempt=new JSONObject().put("method","double_standard").put("thought",example[2])
                .put("note0","用户第1步的原文").put("note1","用户第2步的原文");
            JSONObject context=AiPrompts.context("形成回应",entry,thought,attempt,2);
            if(!context.getString("事件背景").equals(example[0]))throw new AssertionError("Wrong event");
            if(!context.getJSONArray("前面的问题与回答").getJSONObject(0).getString("回答").equals("用户第1步的原文"))throw new AssertionError("Earlier answer lost");
            if(!context.getJSONArray("前面的问题与回答").getJSONObject(1).getString("回答").equals("用户第2步的原文"))throw new AssertionError("Earlier answer lost");
            checks+=3;
            for(ThoughtMethods method:ThoughtMethods.ALL)for(int page=0;page<4;page++){
                attempt.put("method",method.id);
                JSONObject step=AiPrompts.context(page<2?"当前问题":"形成回应",entry,thought,attempt,page);
                String question=page<3?method.questions[page]:"整合前面回答形成最终回应";
                if(!step.getString("当前问题").equals(question))throw new AssertionError("Wrong question for method/step");
                if(!step.getString("方法目的").equals(method.purpose))throw new AssertionError("Method purpose lost");
                if(step.getJSONArray("前面的问题与回答").length()!=Math.min(page,3))throw new AssertionError("Earlier step context lost");
                checks+=3;
            }
        }
        JSONObject entry=new JSONObject().put("event","一件事").put("thoughts",new JSONArray())
            .put("emotions",new JSONObject());
        JSONObject attempt=new JSONObject().put("method","evidence").put("thought","一个想法");
        JSONObject step=AiPrompts.context("当前问题",entry,null,attempt,0);
        if(!step.getString("当前问题").equals(ThoughtMethods.find("evidence").questions[0]))throw new AssertionError("Question changed");
        if(!step.getString("当前问题补充说明").equals(ThoughtMethods.hint("evidence",0)))throw new AssertionError("Hint missing");
        checks+=2;
        System.out.println(checks+" prompt/context checks passed across four scenarios; no model-output quality claim");
    }
}
