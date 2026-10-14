package cn.xinqing.journal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** One method record per thought, with a saved workflow and optional practice rounds. */
final class MethodRecords {
    static final String FINAL_QUESTION="整合前面回答形成最终回应";
    static void prepare(JSONObject attempt,ThoughtMethods method)throws JSONException{
        if(attempt.has("workflow"))return;
        JSONArray workflow=new JSONArray();
        for(ThoughtMethods.Step s:method.steps)workflow.put(new JSONObject().put("question",s.question).put("hint",s.hint).put("kind",s.kind));
        attempt.put("workflow",workflow).put("workflowVersion",2).put("purpose",method.purpose).put("introduction",method.introduction).put("repeatable",method.repeatable);
        // Original exercises stored their third answer both as note2 and response.
        if(method.steps.length==3&&attempt.optString("response").isEmpty()&&!attempt.optString("note2").isEmpty())attempt.put("response",attempt.optString("note2"));
    }
    static void prepareEntry(JSONObject entry)throws JSONException{
        JSONArray thoughts=entry.optJSONArray("thoughts");if(thoughts==null)return;
        for(int i=0;i<thoughts.length();i++){
            JSONObject thought=thoughts.optJSONObject(i);if(thought==null)continue;
            JSONArray records=thought.optJSONArray("attempts");if(records==null)continue;
            for(int j=0;j<records.length();j++){
                JSONObject record=records.optJSONObject(j);if(record==null)continue;
                ThoughtMethods method=ThoughtMethods.find(record.optString("method"));if(method!=null)prepare(record,method);
            }
        }
    }
    static ThoughtMethods.Step[] steps(JSONObject attempt){
        JSONArray saved=attempt.optJSONArray("workflow");
        if(saved!=null&&saved.length()>0){
            ThoughtMethods.Step[] result=new ThoughtMethods.Step[saved.length()];
            for(int i=0;i<saved.length();i++){JSONObject s=saved.optJSONObject(i);result[i]=new ThoughtMethods.Step(s.optString("question"),s.optString("hint"),s.optString("kind","reflection"));}
            return result;
        }
        ThoughtMethods method=ThoughtMethods.find(attempt.optString("method"));
        return method==null?new ThoughtMethods.Step[0]:method.steps;
    }
    static int page(JSONObject attempt){return Math.max(0,Math.min(steps(attempt).length,attempt.optInt("step")));}
    static String key(JSONObject attempt,int page){ThoughtMethods.Step[] steps=steps(attempt);return page>=steps.length||steps[page].response()?"response":"note"+page;}
    static String answer(JSONObject attempt,int page){
        String key=key(attempt,page);
        if(key.equals("response"))return attempt.optString("response",attempt.optString("note"+page));
        return attempt.optString(key);
    }
    static String question(JSONObject attempt,int page){ThoughtMethods.Step[] steps=steps(attempt);return page<steps.length?steps[page].question:FINAL_QUESTION;}
    static int responsePage(JSONObject attempt){ThoughtMethods.Step[] steps=steps(attempt);for(int i=0;i<steps.length;i++)if(steps[i].response())return i;return steps.length;}
    static boolean aiAllowed(String task,JSONObject attempt,int page){
        if(attempt==null)return true;
        ThoughtMethods.Step[] steps=steps(attempt);
        if(attempt.optBoolean("done")||attempt.optBoolean("noHelp")||page<0||page>steps.length)return false;
        if(task.equals("当前问题"))return page<steps.length&&!steps[page].response()&&steps[page].aiAllowed();
        if(task.equals("形成回应")){
            if(page<steps.length&&!steps[page].response())return false;
            if(!practiceComplete(attempt))return false;
            for(int i=0;i<steps.length;i++)if(steps[i].kind.equals("practice")&&answer(attempt,i).trim().isEmpty())return false;
            return true;
        }
        return true;
    }
    static boolean practiceComplete(JSONObject attempt){
        ThoughtMethods.Step[] steps=steps(attempt);
        for(int i=0;i<steps.length;i++)if(steps[i].kind.equals("practice")&&!attempt.optBoolean("performed"+i))return false;
        return true;
    }
    static String notes(JSONObject attempt){
        StringBuilder out=new StringBuilder();ThoughtMethods.Step[] steps=steps(attempt);
        for(int i=0;i<steps.length;i++){
            out.append(steps[i].question).append("\n").append(answer(attempt,i));
            if(steps[i].kind.equals("balance")&&attempt.has("benefitWeight"))out.append("\n好处 ").append(attempt.optInt("benefitWeight")).append(" / 代价 ").append(100-attempt.optInt("benefitWeight"));
            out.append("\n\n");
        }
        return out.toString();
    }
    static boolean finish(JSONObject thought,JSONObject attempt)throws JSONException{
        if(attempt.optBoolean("done")||attempt.optBoolean("noHelp")||!practiceComplete(attempt)||attempt.optString("response").trim().isEmpty()||!attempt.has("responseBelief")||!attempt.has("after"))return false;
        attempt.put("done",true).put("finished",System.currentTimeMillis());
        for(String key:new String[]{"response","responseBelief","after"})thought.put(key,attempt.get(key));
        thought.put("evidence",notes(attempt)).put("handled",true).put("selected",true);
        return true;
    }
    static boolean nextRound(JSONObject attempt)throws JSONException{
        if(attempt.optBoolean("done")||attempt.optBoolean("noHelp")||!attempt.optBoolean("repeatable")||!practiceComplete(attempt)||attempt.optString("response").trim().isEmpty()||!attempt.has("responseBelief")||!attempt.has("after"))return false;
        JSONObject round=new JSONObject(attempt.toString());round.remove("rounds");round.put("finished",System.currentTimeMillis());
        JSONArray rounds=attempt.optJSONArray("rounds");if(rounds==null){rounds=new JSONArray();attempt.put("rounds",rounds);}rounds.put(round);
        ThoughtMethods.Step[] steps=steps(attempt);
        for(int i=0;i<steps.length;i++){attempt.remove("note"+i);attempt.remove("performed"+i);}
        for(String key:new String[]{"response","responseBelief","after","anxietyBefore","anxietyPeak","anxietyAfter","benefitWeight"})attempt.remove(key);
        if(!attempt.has("initialBefore"))attempt.put("initialBefore",attempt.optInt("before"));
        attempt.put("before",round.optInt("after")).put("step",0).put("roundStarted",System.currentTimeMillis());
        return true;
    }
}
