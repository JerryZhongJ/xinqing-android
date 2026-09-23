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
            "有哪些具体事实，让你相信这句话？",
            "有没有遗漏的事实，说明这句话并非全部成立？",
            "把两边的事实放在一起：哪些部分有依据，哪些是推测或说得过头了？"),
        new ThoughtMethods("double_standard","双重标准法","用对待朋友的公平和体谅，重新看待自己。",
            "想象一个你关心的人经历了同样的事，也对自己说这句话。你会对他说什么？",
            "你对自己和对他的标准有什么不同？是否有充分理由对自己更苛刻？",
            "你刚才对他说的哪些话，也真实地适用于你？"),
        new ThoughtMethods("gray","灰度思考","在完全成功与彻底失败之间寻找具体程度。",
            "这句话把事情分成了哪两个极端？",
            "两个极端之间还有哪些可能？这次哪些地方做到了，哪些地方没做到？",
            "按这些具体表现，这件事处在什么位置？怎样描述比全好或全坏更贴近事实？"),
        new ThoughtMethods("semantic","语义法","把僵硬的要求改为准确、有余地的表达。",
            "找出“必须、应该、绝不能”等要求。它要求自己或别人做到什么？",
            "这是真实规则，还是强烈的愿望？不满足它的实际后果是什么？",
            "试着用“我希望”“我更愿意”“我可以努力”怎样重新表达？"),
        new ThoughtMethods("reattribution","重新归因","梳理多个因素，区分责任与全盘责备。",
            "具体问题是什么？你现在把原因全部归在谁身上？",
            "除了这个人，还有哪些环境、信息、他人行为或偶然因素？哪些有事实支持？",
            "哪些在你的控制之内？能承担什么责任、做什么调整？哪些无需全盘归咎于自己或别人？"),
        new ThoughtMethods("define","定义用词","把笼统标签拆成可以核对的具体描述。",
            "这句话中最刺痛你的标签是什么？你怎样定义这个词？",
            "这个定义是否清楚、一致、能用事实检验？一次行为能证明一个人的全部价值吗？",
            "不用这个标签，怎样描述实际发生的行为、结果或困难？")
    };
    static String hint(String id,int step) {
        if(id.equals("evidence")&&step==0)return "尽量写看到、听到或发生过的事情。";
        if(id.equals("evidence")&&step==1)return "想不到也可以如实记录。";
        if(id.equals("gray")&&step==0)return "只评价这次具体表现，不评价整个人的价值。";
        if(id.equals("semantic")&&step==2)return "保留必要责任和实际行动。";
        if(id.equals("define")&&step==2)return "把人和这一次经历分开。";
        return "";
    }
    static ThoughtMethods find(String id){for(ThoughtMethods m:ALL)if(m.id.equals(id))return m;return null;}
}
