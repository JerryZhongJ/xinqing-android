package cn.xinqing.journal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Persistence and AI boundaries for variable workflows, legacy journals and practice rounds. */
public final class MethodRecordsTest {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
    private static JSONObject record(String id)throws Exception{
        JSONObject a=new JSONObject().put("method",id).put("thought","我担心出现不好的结果").put("before",70).put("step",0);
        MethodRecords.prepare(a,ThoughtMethods.find(id));return a;
    }
    private static JSONObject entry()throws Exception{return new JSONObject().put("event","测试情境").put("thoughts",new JSONArray()).put("emotions",new JSONObject());}
    private static void rejected(String task,JSONObject a,int page)throws Exception{
        String before=a.toString();
        try{AiSuggestions.apply(task,new JSONObject().put("note","编造的实际结果").put("response","编造的回应"),entry(),new JSONObject(),a,page);throw new AssertionError("AI must not fill observations or premature conclusions");}
        catch(JSONException expected){check(a.toString().equals(before),"rejection cannot mutate notes, response or scores");}
    }
    public static void main(String[] args)throws Exception{
        JSONObject old=new JSONObject().put("method","evidence").put("step",2).put("note0","支持事实").put("note1","遗漏事实").put("note2","旧版第三步内容");
        MethodRecords.prepare(old,ThoughtMethods.find("evidence"));
        check(old.getString("response").equals("旧版第三步内容"),"legacy third answer becomes editable response");
        check(old.getString("note2").equals("旧版第三步内容"),"legacy third note remains intact");
        check(MethodRecords.page(old)==2,"legacy draft resumes at original page");
        JSONObject completed=new JSONObject().put("method","double_standard").put("done",true).put("note2","旧版已完成回应");
        JSONObject unknown=new JSONObject().put("method","legacy").put("evidence","自由探索的原有事实");String unknownBefore=unknown.toString();
        JSONArray retiredWorkflow=new JSONArray();
        for(String question:new String[]{"朋友的负面判断","对朋友的回应","朋友的疑问","进一步回应","带回自己"})
            retiredWorkflow.put(new JSONObject().put("question",question).put("hint","").put("kind",retiredWorkflow.length()==4?"response":"reflection"));
        JSONObject retired=new JSONObject().put("method","paradoxical_double_standard").put("name","悖论双重标准法")
            .put("workflow",retiredWorkflow).put("workflowVersion",2).put("step",3).put("note3","此前对朋友的进一步回应").put("response","适用于自己的理解");
        retired.put("rounds",new JSONArray().put(new JSONObject(retired.toString())));String retiredBefore=retired.toString();
        JSONObject legacyEntry=new JSONObject().put("thoughts",new JSONArray().put(new JSONObject().put("attempts",new JSONArray().put(completed).put(unknown).put(retired))));
        MethodRecords.prepareEntry(legacyEntry);
        check(completed.getString("response").equals("旧版已完成回应")&&completed.has("workflow"),"completed legacy records are migrated before history rendering");
        check(unknown.toString().equals(unknownBefore),"unknown legacy records are preserved without conversion");
        check(retired.toString().equals(retiredBefore),"withdrawing a method preserves its draft, response and saved rounds");
        JSONObject restoredRetired=new JSONObject(retired.toString());
        check(MethodRecords.notes(restoredRetired).contains("此前对朋友的进一步回应")&&MethodRecords.notes(restoredRetired).contains("适用于自己的理解"),"retired method history still renders from its saved workflow");
        check(MethodRecords.answer(restoredRetired.getJSONArray("rounds").getJSONObject(0),3).equals("此前对朋友的进一步回应"),"retired practice rounds remain readable after restart");

        old.put("workflow",new JSONArray().put(new JSONObject().put("question","记录时的问题").put("hint","记录时的说明").put("kind","response")));
        MethodRecords.prepare(old,ThoughtMethods.find("evidence"));
        check(MethodRecords.question(old,0).equals("记录时的问题"),"saved wording not replaced by later source definitions");
        JSONObject previousExperiment=record("experiment");JSONArray previousWorkflow=new JSONArray();
        for(int i=0;i<4;i++)previousWorkflow.put(previousExperiment.getJSONArray("workflow").getJSONObject(i));
        previousWorkflow.put(new JSONObject().put("question","把实际结果与原预测比较，你发现了什么？").put("hint","按事先标准比较。").put("kind","reflection"));
        previousWorkflow.put(new JSONObject().put("question","根据这次实验，你现在怎样理解原想法？").put("hint","保留不确定的部分。").put("kind","response"));
        previousExperiment.put("workflow",previousWorkflow).put("step",5).put("note4","合并前的比较记录").put("response","合并前的最终回应").put("performed3",true);
        String previousBefore=previousExperiment.toString();MethodRecords.prepare(previousExperiment,ThoughtMethods.find("experiment"));
        check(previousExperiment.toString().equals(previousBefore),"shortening a method does not rewrite a saved six-step draft");
        JSONObject restoredExperiment=new JSONObject(previousExperiment.toString());
        check(MethodRecords.page(restoredExperiment)==5&&MethodRecords.responsePage(restoredExperiment)==5,"saved response keeps its original page after questions merge");
        check(MethodRecords.notes(restoredExperiment).contains("合并前的比较记录")&&MethodRecords.answer(restoredExperiment,5).equals("合并前的最终回应"),"merged questions do not conflate historical reflection and response");
        JSONObject longer=record("experiment");longer.put("note2","事先约定的观察标准").put("step",4);
        MethodRecords.prepare(longer,ThoughtMethods.find("experiment"));
        check(!longer.has("response"),"a long method's third note is not a response");
        check(MethodRecords.page(new JSONObject(longer.toString()))==4,"long draft can resume beyond the former third-step limit");
        check(!MethodRecords.finish(new JSONObject(),longer),"plan alone cannot finish");
        longer.put("performed3",true);
        check(!MethodRecords.aiAllowed("形成回应",longer,MethodRecords.responsePage(longer)),"a checkbox alone cannot supply missing real observations to AI");
        longer.put("note3","实际观察");check(MethodRecords.aiAllowed("形成回应",longer,MethodRecords.responsePage(longer)),"confirmed and written observations enable a grounded response");


        int maximum=0,minimum=100;
        for(ThoughtMethods method:ThoughtMethods.ALL){
            JSONObject a=record(method.id);ThoughtMethods.Step[] steps=MethodRecords.steps(a);
            maximum=Math.max(maximum,steps.length);minimum=Math.min(minimum,steps.length);
            JSONObject thought=new JSONObject().put("before",90).put("response","此前已完成的回应");
            for(int i=0;i<steps.length;i++){
                a.put("step",i).put(MethodRecords.key(a,i),"第"+i+"步实际原文");
                JSONObject restored=new JSONObject(a.toString());
                check(MethodRecords.page(restored)==i,"serialized draft resumes at each step");
                check(MethodRecords.answer(restored,i).equals("第"+i+"步实际原文"),"each dynamic field survives persistence");
                if(steps[i].observation()||steps[i].kind.equals("balance"))rejected("当前问题",a,i);
                if(steps[i].kind.equals("practice")){
                    check(!MethodRecords.aiAllowed("形成回应",a,steps.length),"AI cannot invent a behavioral conclusion before practice");
                    rejected("形成回应",a,steps.length);
                    a.put("performed"+i,true);
                }
            }
            a.put("step",steps.length).put("responseBelief",0).put("after",85);
            check(MethodRecords.finish(thought,a),"zero belief and increased original belief can be saved");
            check(thought.getInt("before")==90,"journal's original belief is not overwritten");
            check(thought.getInt("after")==85,"actual rating retained without lowering requirement");
            check(thought.getString("evidence").contains("第0步实际原文"),"all method discoveries propagated");
            if(steps.length>4)check(thought.getString("evidence").contains("第3步实际原文"),"steps beyond old three are exported into discovery text");
            check(!MethodRecords.nextRound(a),"completed method stays read-only");
            rejected("形成回应",a,steps.length);

        }
        check(maximum>minimum,"method workflows genuinely have different lengths");
        JSONObject a=record("exposure");int size=MethodRecords.steps(a).length;
        a.put("response","本轮真实发现").put("responseBelief",40).put("after",55).put("anxietyBefore",60).put("anxietyPeak",75).put("anxietyAfter",65).put("note4","实际接近的经历");
        check(!MethodRecords.nextRound(a),"cannot archive an unperformed exposure");
        for(int i=0;i<size;i++)if(MethodRecords.steps(a)[i].kind.equals("practice"))a.put("performed"+i,true);
        check(MethodRecords.nextRound(a),"completed practice can continue inside the same method record");
        check(a.getJSONArray("rounds").length()==1,"one practice round retained");
        JSONObject saved=a.getJSONArray("rounds").getJSONObject(0);
        check(saved.getString("note4").equals("实际接近的经历")&&saved.getInt("anxietyAfter")==65,"round retains observations and independently rated anxiety");
        check(saved.getString("response").equals("本轮真实发现"),"round retains its response");
        check(!a.has("note4")&&!a.has("response")&&!a.has("anxietyAfter")&&!MethodRecords.practiceComplete(a),"new round cannot inherit previous observations or completion");
        check(a.getInt("before")==55&&a.getInt("initialBefore")==70,"next round starts from latest belief while preserving initial value");
        a=new JSONObject(a.toString());check(a.getJSONArray("rounds").length()==1,"round history survives restart");
        for(String id:new String[]{"survey","feared_fantasy","shame_attacking","smile_hello","self_disclosure"})check(CbtReferences.score(id,new JSONObject().put("读心术",true))>0,"social anxiety methods participate in ranking");
        for(ThoughtMethods method:CbtReferences.ranked(new JSONObject().put("distortions",new JSONObject().put("预言未来",true))))
            check(!method.id.equals("paradoxical_double_standard"),"withdrawn method cannot appear in method choices or AI candidates");
        JSONObject cba=record("cost_benefit");cba.put("benefitWeight",35);
        JSONObject ctx=AiPrompts.context("形成回应",entry(),null,cba,MethodRecords.responsePage(cba));
        check(ctx.getJSONObject("用户的权衡").getInt("代价")==65,"manual cost-benefit weight reaches response context");
        check(!MethodRecords.aiAllowed("当前问题",cba,3),"AI cannot set a person's cost-benefit weights");
        check(MethodRecords.notes(cba).contains("好处 35 / 代价 65"),"manual weights included in textual record");
        System.out.println(checks+" method persistence, practice and AI boundary assertions passed");
    }
}
