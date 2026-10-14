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
            for(ThoughtMethods method:ThoughtMethods.ALL)for(int page=0;page<=method.steps.length;page++){
                attempt.put("method",method.id);
                JSONObject step=AiPrompts.context(page<method.steps.length&&!method.steps[page].response()?"当前问题":"形成回应",entry,thought,attempt,page);
                String question=page<method.steps.length?method.questions[page]:MethodRecords.FINAL_QUESTION;
                if(!step.getString("当前问题").equals(question))throw new AssertionError("Wrong question for method/step");
                if(!step.getString("方法目的").equals(method.purpose))throw new AssertionError("Method purpose lost");
                if(step.getJSONArray("前面的问题与回答").length()!=Math.min(page,method.steps.length))throw new AssertionError("Earlier step context lost");
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
        JSONObject recognition=AiPrompts.context("识别认知歪曲",entry,new JSONObject().put("text","当前要检验的想法"),null,0);
        JSONObject guide=recognition.getJSONObject("认知歪曲参考说明");
        JSONArray candidates=recognition.getJSONArray("候选类型"),definitions=guide.getJSONArray("类型说明");
        if(definitions.length()!=candidates.length())throw new AssertionError("Every candidate needs a sourced explanation");
        java.util.HashSet<String> explained=new java.util.HashSet<>();
        for(int i=0;i<definitions.length();i++){
            JSONObject definition=definitions.getJSONObject(i);String name=definition.getString("name");
            if(!java.util.Arrays.asList(AiSuggestions.DISTORTIONS).contains(name)||!explained.add(name))throw new AssertionError("Explanation must identify one distinct allowed choice");
            if(definition.getString("核心含义").trim().isEmpty()||definition.getString("补充说明").trim().isEmpty())throw new AssertionError("Explanations need content beyond type names");
            JSONArray sources=definition.getJSONArray("来源");if(sources.length()==0)throw new AssertionError("Explanations need a reference");
            for(int j=0;j<sources.length();j++)if(!sources.getString(j).startsWith("https://feelinggood.com/"))throw new AssertionError("Distortion references must use Burns' first-party website");
            checks+=3;
        }
        if(!recognition.getString("当前想法").equals("当前要检验的想法"))throw new AssertionError("Reference material cannot replace the user's thought");
        if(!guide.getString("提醒来源").startsWith("https://feelinggood.com/"))throw new AssertionError("Boundary reminder needs a first-party reference");
        checks+=3;
        for(String task:AiPrompts.TASKS)if(!task.equals("识别认知歪曲")){
            if(AiPrompts.context(task,entry,new JSONObject().put("text","另一个想法"),null,0).has("认知歪曲参考说明"))throw new AssertionError("Detailed distortion reference belongs only in the recognition request");
            checks++;
        }
        System.out.println(checks+" prompt/context checks passed across four scenarios; no model-output quality claim");
    }
}
