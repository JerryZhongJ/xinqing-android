package cn.xinqing.journal;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Arrays;

/** First-party Burns references, independently summarized in Chinese; scoring is an app convention. */
final class CbtReferences {
    static final String METHODS_SOURCE="https://webinars.jackhirose.com/wp-content/uploads/2021/12/May-5-Burns-Handout.pdf";
    static final String REFRAME_SOURCE="https://angelapoch.com/wp-content/uploads/2023/03/2022-03-14_PostiveReframing-handout.pdf";
    static final String DISTORTIONS_SOURCE="https://feelinggood.com/wp-content/uploads/2017/04/41656-distortions-v-18.pdf";
    static final String DISTORTION_BOUNDARIES_SOURCE="https://feelinggood.com/2023/12/04/373-why-therapy-fails/";
    private static JSONObject distortion(String name,String meaning,String detail,String... sources)throws JSONException{
        return new JSONObject().put("name",name).put("核心含义",meaning).put("补充说明",detail).put("来源",new JSONArray(Arrays.asList(sources)));
    }
    static JSONObject distortionGuide()throws JSONException{
        String filtering="https://feelinggood.com/2020/05/18/191-how-to-crush-negative-thoughts-mental-filter-discounting-the-positive/";
        String conclusions="https://feelinggood.com/2020/06/08/194-jumping-to-conclusions/";
        JSONArray types=new JSONArray()
            .put(distortion("非黑即白","用非此即彼的极端类别评价自己或事情，忽略中间程度。",
                "把未完全成功视为彻底失败；Burns 指出多数表现处于两个极端之间，而不是只有满分或零分。",
                "https://feelinggood.com/2020/05/04/189-how-to-crush-negative-thoughts-all-or-nothing-thinking/"))
            .put(distortion("以偏概全","由具体的负面事件或不足，扩展为对整个人或未来的笼统结论。",
                "Burns 区分两种形式：从某项失败推成整个自己是失败者；从眼前经历推成总是如此、永远不会改变。他也将贴标签称为以偏概全的极端形式。",
                "https://feelinggood.com/2020/05/11/190-how-to-crush-negative-thoughts-overgeneralization/",
                "https://feelinggood.com/2014/01/06/secrets-of-self-esteem-2-negative-and-positive-distortions/"))
            .put(distortion("心理过滤","把注意力集中在负面细节上，忽略积极信息，使整体评价变得片面。",
                "重点是关注和遗漏了哪些信息；否定积极则进一步把已有的积极信息判为不算数。",
                filtering))
            .put(distortion("否定积极","把自己的积极品质或其他积极信息解释成不算数，从而维持全盘负面的看法。",
                "与心理过滤相比，这里不只是没有留意积极信息，而是将它的意义或有效性否定。",
                filtering))
            .put(distortion("读心术","在缺乏事实支持时，认定自己知道别人正在怎样想，尤其是对自己的负面评价。",
                "属于跳跃结论；对象是他人的想法或意图。Burns 也举出擅自认定别人故意不讲理，或误以为别人很欣赏自己的情形。",
                conclusions))
            .put(distortion("预言未来","在证据不足时，预断事情会出现坏结果。",
                "同属跳跃结论；对象是未来结果，包括灾难性预测，以及认定问题永远不会改善。",
                conclusions))
            .put(distortion("夸大或缩小","把事情的重要性、困难或危险放大到失去比例，或把积极品质和成就缩得过小。",
                "Burns 也提到拖延时夸大任务难度、低估开始做一点的价值；核心是程度或分量失真。",
                "https://feelinggood.com/2020/06/15/195-how-to-crush-negative-thoughts-magnification-and-minimization/"))
            .put(distortion("情绪化推理","从自己的感受推断外部事实或对自己的评价必然成立。",
                "判断关键是把感受当成事实依据。Burns 同时肯定表达感受对亲密关系的价值，并提醒感受有时会误导对现实的判断。",
                "https://feelinggood.com/2020/08/10/203-how-to-crush-negative-thoughts-emotional-reasoning/"))
            .put(distortion("应该思维","用僵硬的应该、必须等要求责备自己、他人或世界，也可能隐含这种要求。",
                "Burns 明确保留道德、法律、自然规律三类有效的应该；不能仅凭出现应该或必须就判为歪曲。",
                "https://feelinggood.com/2020/08/24/205-how-to-crush-negative-thoughts-should-statements/"))
            .put(distortion("贴标签","把具体的错误或不足上升为对自己或他人的整个人格、价值或角色的负面定义。",
                "Burns 区分自我标签与他人标签，也区分整体自我标签与角色标签；具体描述一次行为不同于用一个标签概括整个人。",
                "https://feelinggood.com/2020/07/25/199-how-to-crush-negative-thoughts-labeling-2/"))
            .put(distortion("自责或责怪他人","自责是承担并非完全由自己负责的问题；责怪他人是归咎于别人，并忽略自己对问题的贡献。",
                "Burns 将两者分开讨论，并指出它们常分别伴随针对自己或他人的应该思维。本应用将两者合为一个候选类型。",
                DISTORTIONS_SOURCE,"https://feelinggood.com/2020/08/31/206-how-to-crush-negative-thoughts-blame/"));
        return new JSONObject().put("资料说明","依据 David D. Burns 官网清单与专题文章独立编写的中文摘要，并非官方中文译文。只使用下列候选名称。")
            .put("判断提醒","Burns 提醒负面想法可能符合事实，也存在健康的负面情绪；不能预设用户的想法必有歪曲。")
            .put("提醒来源",DISTORTION_BOUNDARIES_SOURCE).put("类型说明",types);
    }
    static String[] specific(String distortion) {
        switch(distortion){
            case "非黑即白":return new String[]{"gray"};
            case "以偏概全":return new String[]{"define","evidence"};
            case "心理过滤":case "否定积极":case "情绪化推理":return new String[]{"evidence"};
            case "读心术":return new String[]{"survey","feared_fantasy","shame_attacking","smile_hello","self_disclosure"};
            case "预言未来":return new String[]{"cost_benefit","define","experiment","exposure","cognitive_exposure","response_prevention"};
            case "夸大或缩小":return new String[]{"experiment","evidence","semantic"};
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
