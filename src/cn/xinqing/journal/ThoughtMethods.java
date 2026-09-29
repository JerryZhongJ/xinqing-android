package cn.xinqing.journal;

/** Short, independently worded prompts for self-directed written exercises. */
final class ThoughtMethods {
    final String id, name, purpose;
    final String[] questions;
    ThoughtMethods(String id,String name,String purpose,String... questions) {
        this.id=id;this.name=name;this.purpose=purpose;this.questions=questions;
    }
    static final ThoughtMethods[] ALL={
        new ThoughtMethods("evidence","检验证据","把事实与推测分开，看看判断是否完整。",
            "哪些具体事实支持这句话？",
            "哪些事实不支持这句话？",
            "根据这些证据，原想法有多准确？"),
        new ThoughtMethods("double_standard","双重标准法","用对待朋友的公平和体谅，重新看待自己。",
            "如果朋友有同样的想法，你会对他说什么？",
            "你对自己和朋友用了不同的标准吗？",
            "你对朋友说的话，哪些也适用于自己？"),
        new ThoughtMethods("gray","灰度思考","在完全成功与彻底失败之间寻找具体程度。",
            "原想法把事情分成了哪两个极端？",
            "两个极端之间还有哪些可能？",
            "怎样描述这次表现更贴近实际？"),
        new ThoughtMethods("semantic","语义法","把僵硬的要求改为准确、有余地的表达。",
            "原想法里有哪些僵硬的要求？",
            "这些要求是事实规则，还是强烈的愿望？",
            "怎样把这些要求说得更准确？"),
        new ThoughtMethods("reattribution","重新归因","梳理多个因素，区分责任与全盘责备。",
            "你现在把问题的原因主要归在谁身上？",
            "还有哪些因素促成了这个问题？",
            "怎样更公平地看待各方的责任？"),
        new ThoughtMethods("define","定义用词","检验笼统标签是否有清楚、可靠的含义。",
            "原想法里哪个标签最刺痛你？",
            "这个标签的定义经得起检验吗？",
            "原想法中的标签还成立吗？")
    };
    static String hint(String id,int step) {
        switch(id){
            case "evidence":
                if(step==0)return "写看到、听到或发生过的事，不只写担心的结果。";
                if(step==1)return "留意原想法可能只部分成立的情况；想不到也可以如实记录。";
                return "把支持和不支持的事实放在一起，区分有依据的部分与推测。";
            case "double_standard":
                if(step==0)return "想象你关心的人经历了同样的事，也对自己说了这句话。";
                if(step==1)return "比较你对朋友说的话和对自己的要求；若标准不同，理由充分吗？";
                return "只留下你觉得真实、也愿意用来对待自己的部分。";
            case "gray":
                if(step==0)return "留意‘完全成功’或‘彻底失败’这类非此即彼的判断。";
                if(step==1)return "看看这次具体做到了什么，哪些尚未做到。";
                return "在两个极端之间找到更贴近事实的说法；只评价这次表现。";
            case "semantic":
                if(step==0)return "找出‘必须、应该、绝不能’等词，写清它要求什么。";
                if(step==1)return "如果没做到，实际后果是什么？也可能确实有需要遵守的规则。";
                return "试着用‘我希望’‘我更愿意’等表达，保留必要的责任和行动。";
            case "reattribution":
                if(step==0)return "先写清具体问题，留意是否把全部责任放在自己或某个人身上。";
                if(step==1)return "考虑环境、信息、他人行为及偶然因素，只写有依据的部分。";
                return "区分自己能负责和能调整的部分，以及无法独自承担的部分。";
            case "define":
                if(step==0)return "例如‘失败者’‘没价值’，先写下你怎样定义这个词。";
                if(step==1)return "它是否清楚、一致，能否用事实验证？一次行为足以定义整个人吗？";
                return "若定义含糊或以偏概全，可以放下标签，只描述具体行为或结果。";
            default:return "";
        }
    }
    static ThoughtMethods find(String id){for(ThoughtMethods m:ALL)if(m.id.equals(id))return m;return null;}
}
