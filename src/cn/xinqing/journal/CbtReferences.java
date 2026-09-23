package cn.xinqing.journal;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Arrays;

/** Mappings from Burns' 2021 workshop toolkit; scoring is an app convention. */
final class CbtReferences {
    static final String METHODS_SOURCE="https://webinars.jackhirose.com/wp-content/uploads/2021/12/May-5-Burns-Handout.pdf";
    static final String REFRAME_SOURCE="https://angelapoch.com/wp-content/uploads/2023/03/2022-03-14_PostiveReframing-handout.pdf";
    static String[] specific(String distortion) {
        switch(distortion){
            case "非黑即白":return new String[]{"gray"};
            case "以偏概全":return new String[]{"define","evidence"};
            case "心理过滤":case "否定积极":case "情绪化推理":return new String[]{"evidence"};
            case "预言未来":return new String[]{"define"};
            case "夸大或缩小":return new String[]{"evidence","semantic"};
            case "贴标签":return new String[]{"semantic","define","gray"};
            case "应该思维":return new String[]{"semantic","double_standard"};
            case "自责或责怪他人":return new String[]{"reattribution"};
            default:return new String[0];
        }
    }
    static int score(String method,JSONObject chosen){
        int result=0;if(chosen==null)return 0;
        for(String d:AiSuggestions.DISTORTIONS)if(chosen.optBoolean(d)){
            if(Arrays.asList(specific(d)).contains(method))result+=2;
            else if(method.equals("double_standard"))result+=1;
        }return result;
    }
    static ArrayList<ThoughtMethods> ranked(JSONObject thought){
        ArrayList<ThoughtMethods> result=new ArrayList<>(Arrays.asList(ThoughtMethods.ALL));
        JSONObject selected=thought.optJSONObject("distortions");
        result.sort((a,b)->Integer.compare(score(b.id,selected),score(a.id,selected)));return result;
    }
    static String[] cue(String emotion){
        switch(emotion){
            case "悲伤":return new String[]{"注意到损失","珍惜在意的事物"};
            case "焦虑":return new String[]{"留意风险和准备","谨慎"};
            case "内疚":case "羞愧":return new String[]{"检查失误","负责、在意行为标准"};
            case "自卑":return new String[]{"留意不足","愿意改进"};
            case "孤独":return new String[]{"觉察联系的需要","重视关系"};
            case "尴尬":return new String[]{"留意自己的举止","在意他人的感受"};
            case "绝望":return new String[]{"降低失望风险","重视实际情况"};
            case "沮丧":return new String[]{"继续找办法","在意目标"};
            case "愤怒":return new String[]{"留意不公平或边界","重视公平"};
            default:return new String[]{"仅依据实际情绪","不硬套品质"};
        }
    }
    static JSONArray reframingCues(JSONObject feelings,String task)throws JSONException{
        JSONArray result=new JSONArray();java.util.Iterator<String> keys=feelings.keys();while(keys.hasNext()){String key=keys.next();if(feelings.getJSONObject(key).optInt("before")>0)result.put(key+"："+cue(key)[task.equals("重构品质")?1:0]);}return result;
    }
}
