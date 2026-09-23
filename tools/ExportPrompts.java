package cn.xinqing.journal;

import org.json.JSONObject;
import org.json.JSONArray;

/** Generated from the same strings and context builder the Android app uses. */
public final class ExportPrompts {
    public static void main(String[] args)throws Exception{
        System.out.println("# AI 提示词全文（1.24）\n\n此文件由运行时代码导出，不含密钥。每次实际请求由 system、下方任务 prompt 和当前日志的 JSON 材料组成。通用指令没有内置具体生活案例或措辞黑名单；文末上下文使用占位符说明字段，运行时只填入当前日志的数据。\n");
        System.out.println("## System\n\n```text\n"+AiPrompts.SYSTEM+"\n```\n");
        for(String task:AiPrompts.TASKS)System.out.println("## "+task+"\n\n```text\n"+AiPrompts.instruction(task)+"\n```\n");
        System.out.println("## 方法与角色\n\n所有方法共用上述当前问题和形成回应指令，没有按方法或步骤选择的人称专用 prompt。具体方法目的、当前问题及此前问答放在动态上下文中，用于判断角色和人称。\n");
        System.out.println("## 随上下文发送的方法问题\n");
        for(ThoughtMethods method:ThoughtMethods.ALL){System.out.println("### "+method.name+"\n");for(int i=0;i<3;i++){System.out.println((i+1)+". "+method.questions[i]);if(!ThoughtMethods.hint(method.id,i).isEmpty())System.out.println("   补充说明："+ThoughtMethods.hint(method.id,i));}System.out.println();}
        JSONObject entry=new JSONObject().put("event","<用户记录的事件>").put("context","<用户填写的背景>").put("benefits","<已经填写的益处>").put("values","<已经填写的品质>").put("emotions",new JSONObject().put("悲伤",new JSONObject().put("before",70))).put("thoughts",new JSONArray().put(new JSONObject().put("text","<用户当前的想法>")));
        JSONObject thought=entry.getJSONArray("thoughts").getJSONObject(0);thought.put("distortions",new JSONObject().put("以偏概全",true).put("贴标签",true));
        JSONObject attempt=new JSONObject().put("method","double_standard").put("thought",thought.getString("text")).put("note0","<第1步：用户对假想朋友说的话>").put("note1","<第2步：用户对两套标准的比较>").put("response","<用户已有的回应>");
        System.out.println("## 实际上下文结构（占位符说明）\n\n除事件、情绪、想法外，方法练习包含每个已完成步骤的问题与回答，以及当前已写内容。以下由实际 context() 生成。此处的悲伤、已选歪曲和方法仅用于显示结构，运行时按用户选择替换。\n");
        for(String task:AiPrompts.TASKS){int page=task.equals("形成回应")?2:1;JSONObject context=AiPrompts.context(task,entry,thought,(task.equals("当前问题")||task.equals("形成回应"))?attempt:null,page);System.out.println("### "+task+"\n\n```json\n"+context.toString(2)+"\n```\n");}
        System.out.println("## 参考资料及排序约定\n\n情绪重构线索是对公开培训资料的短摘要，按当前情绪选择发送，不是原书整表复制。\n\n"+CbtReferences.REFRAME_SOURCE+"\n\n方法对应关系依据 Burns 2021 讲义的 Toolkit 表（PDF第56–58页），只纳入本应用已有的方法。它不是已逐项核对的《Feeling Great》第106–107页原表。\n\n"+CbtReferences.METHODS_SOURCE+"\n\n排序按专门匹配每项2分、通用双重标准法每项1分、未匹配0分累加，同分保留原顺序；这是应用的排序约定，不是疗效概率。AI 推荐仅在点击后覆盖优先顺序。合并的自责/责怪选项采用两者共有的重新归因对应；悖论双重标准法不冒充本应用的普通双重标准法。");
    }
}
