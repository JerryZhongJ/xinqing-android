package cn.xinqing.journal;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int DANGER = Color.rgb(179,38,30), BLUE = Color.rgb(48,91,139), SOFT = Color.rgb(237,243,247);
    private static final int GREEN = Color.rgb(23,107,91), INK = Color.rgb(32,49,46), MUTED = Color.rgb(104,118,113);
    private static final String[] EMOTIONS = {"悲伤", "焦虑", "内疚", "羞愧", "自卑", "孤独", "尴尬", "绝望", "沮丧", "愤怒"};
    private static final String[] EMOTION_WORDS = {"悲伤  低落  难过  不快乐", "焦虑  担忧  紧张  害怕", "内疚  后悔  自责  过意不去", "羞愧  丢脸  难为情  无地自容", "自卑  不如人  无价值感  无能感", "孤独  寂寞  被冷落  被遗弃", "尴尬  窘迫  不自在  局促", "绝望  无望  悲观  心灰意冷", "沮丧  挫败  受挫  无力", "愤怒  恼火  怨恨  不满"};
    private static final String[] STEPS = {"具体情境", "情绪", "自动想法", "正向重构", "魔法刻度盘", "探索想法", "重新评估"};
    private static final String[] DISTORTIONS = {"非黑即白", "以偏概全", "心理过滤", "否定积极", "读心术", "预言未来", "夸大或缩小", "情绪化推理", "应该思维", "贴标签", "自责或责怪他人"};
    private static final String[] EXPLANATIONS = {"只有完美和失败两个选项。", "把一次经历当作永远如此的规律。", "只留意不顺利的部分，忽略完整事实。", "把积极的经历解释为不算数。", "未经确认就认定别人对自己的看法。", "把尚未发生的坏结果当成事实。", "放大问题，或低估自己的能力与资源。", "因为感觉糟糕，就认为事实一定糟糕。", "用僵硬的必须、应该要求自己或他人。", "用一个负面标签定义整个人。", "把复杂问题全部归咎于自己或别人。"};
    private JSONArray entries = new JSONArray();
    private JSONObject active;
    private LinearLayout root, body;
    private int step;
    private String screen = "home", exportText;
    private boolean storageReadable = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveTask = () -> persist();
    private AiClient.Request aiRequest;
    private View aiSource;
    private Runnable resetAi;
    private int aiGeneration;
    private AlertDialog visibleDialog;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        try { entries = new JSONArray(getPreferences(0).getString("entries", "[]")); }
        catch (JSONException e) { storageReadable = false; toast("日志读取失败，已停止写入以保护原数据，请勿清除应用数据"); }
        for(int i=0;i<entries.length();i++) prepareEntry(entries.optJSONObject(i));
        if (state != null && state.containsKey("active")) {
            for (int i=0;i<entries.length();i++) if (entries.optJSONObject(i).optString("id").equals(state.getString("active"))) active=entries.optJSONObject(i);
            step=state.getInt("step");
        }
        exportText=state==null?null:state.getString("exportText");
        if (active != null) {
            if ("summary".equals(state.getString("screen"))) summary(); else editor();
        } else home();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        persist();
        out.putString("exportText",exportText);
        if (active != null) { out.putString("active",active.optString("id")); out.putInt("step",step); out.putString("screen",screen); }
    }
    @Override protected void onPause() { super.onPause();cancelAi(); handler.removeCallbacks(saveTask); persist(); }
    @Override protected void onDestroy() { cancelAi();handler.removeCallbacksAndMessages(null);super.onDestroy(); }
    @Override public void onConfigurationChanged(Configuration configuration) { super.onConfigurationChanged(configuration); if(visibleDialog!=null&&visibleDialog.isShowing())sizeDialog(visibleDialog); }
    @Override public void onBackPressed() { if (!screen.equals("home")) leavePage(); else super.onBackPressed(); }
    private void leavePage() {
        if(!screen.equals("edit")){persist();home();return;}
        showDialog(new AlertDialog.Builder(this).setCustomTitle(dialogHeader("离开这篇日志？"))
            .setItems(new CharSequence[]{"保存并退出","继续填写",dangerLabel("删除日志")},(dialog,index)->{
                if(index==0){persist();home();}
                else if(index==2)deleteEntry(active);
            }).create());
    }
    private CharSequence dangerLabel(String label) {
        SpannableString text=new SpannableString(label);text.setSpan(new android.text.style.ForegroundColorSpan(DANGER),0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return text;
    }
    private void showDialog(AlertDialog dialog) { dialog.show();visibleDialog=dialog;sizeDialog(dialog); }
    private LinearLayout dialogHeader(String title) {
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20),dp(12),dp(20),dp(8));
        header.addView(styledText(title,TypeRole.DIALOG),new LinearLayout.LayoutParams(0,-2,1));
        return header;
    }
    private void sizeDialog(AlertDialog dialog) {
        int width=getResources().getDisplayMetrics().widthPixels;
        boolean portrait=getResources().getConfiguration().orientation!=Configuration.ORIENTATION_LANDSCAPE;
        dialog.getWindow().setLayout(Math.round(width*(portrait?0.86f:0.68f)),WindowManager.LayoutParams.WRAP_CONTENT);
    }
    private void showDeleteDialog(AlertDialog dialog) { showDialog(dialog);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(DANGER); }
    private void removeThought(int index) {
        showDeleteDialog(new AlertDialog.Builder(this).setCustomTitle(dialogHeader("移除这条想法？")).setMessage("这条想法的评分、歪曲和回应也会移除。").setNegativeButton("取消",null).setPositiveButton("移除",(d,w)->{thoughts().remove(index);put(active,"focusThought",0);changed();editor();}).create());
    }
    private ImageButton iconButton(int icon,String description,Runnable action) {
        ImageButton button=new ImageButton(this);button.setImageResource(icon);button.setImageTintList(ColorStateList.valueOf(icon==R.drawable.ic_delete?DANGER:MUTED));
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int labelSize=icon==R.drawable.ic_more?20:16;
        int iconSize=Math.round(labelSize*getResources().getDisplayMetrics().scaledDensity);
        int inset=Math.max(0,(dp(48)-iconSize)/2);
        button.setPadding(inset,inset,inset,inset);
        button.setBackground(touchBackground(24));button.setContentDescription(description);button.setTooltipText(description);
        button.setOnClickListener(v->action.run());return button;
    }
    private void put(JSONObject object, String key, Object value) {
        try { object.put(key,value); } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
    private void prepareEntry(JSONObject entry) {
        if(entry.optInt("schema")<2) {
            int[] mappedSteps={0,1,3,5,6};
            put(entry,"step",mappedSteps[Math.max(0,Math.min(4,entry.optInt("step")))]);
            put(entry,"schema",2);
        }
        JSONObject ratings=entry.optJSONObject("emotions");
        for(String name:EMOTIONS) {
            JSONObject rating=ratings.optJSONObject(name);
            if(rating==null){rating=new JSONObject();put(ratings,name,rating);}
            if(!rating.has("before"))put(rating,"before",0);
        }
    }
    private void changed() { if(active!=null) put(active,"updated",System.currentTimeMillis()); handler.removeCallbacks(saveTask); handler.postDelayed(saveTask,350); }
    private void persist() { if (storageReadable && !getPreferences(0).edit().putString("entries",entries.toString()).commit()) toast("保存失败，请检查设备空间"); }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private GradientDrawable bg(int color, int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private RippleDrawable touchBackground(int radius) { return new RippleDrawable(ColorStateList.valueOf(0x22176B5B),null,bg(Color.WHITE,radius)); }
    private LinearLayout col() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setLetterSpacing(0); return t; }
    private enum TypeRole { PAGE, DIALOG, SECTION, ITEM, QUESTION, SCORE, PREVIEW, VALUE, BODY, HINT, META }
    private enum ComponentRelation { FIELD_TO_SCORE, SAME_GROUP, NEXT_QUESTION, NEXT_ITEM, NEXT_SECTION }
    private int componentSpace(ComponentRelation relation) {
        switch(relation) {
            case FIELD_TO_SCORE: return 2;
            case SAME_GROUP: return 8;
            case NEXT_QUESTION: return 16;
            case NEXT_ITEM: return 12;
            default: return 22;
        }
    }
    private TextView styledText(String content,TypeRole role) {
        int size;
        int color=INK;
        boolean bold=false;
        switch(role) {
            case PAGE: size=24;bold=true;break;
            case DIALOG: size=20;bold=true;break;
            case SECTION: size=17;bold=true;break;
            case ITEM: size=16;bold=true;break;
            case QUESTION: size=16;bold=true;break;
            case SCORE: size=14;color=MUTED;break;
            case PREVIEW: size=14;break;
            case VALUE: size=16;color=GREEN;bold=true;break;
            case HINT: size=14;color=MUTED;break;
            case META: size=13;color=MUTED;break;
            default: size=16;
        }
        TextView view=text(content,size,color);
        if(bold)view.setTypeface(null,Typeface.BOLD);
        view.setLineSpacing(dp(role==TypeRole.BODY||role==TypeRole.HINT||role==TypeRole.QUESTION?4:2),1);
        return view;
    }
    private int topSpace(TypeRole role) {
        switch(role) {
            case PAGE: return 22;
            case SECTION: return 16;
            case ITEM: return 10;
            case QUESTION: return 12;
            case SCORE: return 6;
            case PREVIEW: return 2;
            case HINT: return 4;
            case META: return 4;
            default: return 0;
        }
    }
    private int bottomSpace(TypeRole role) {
        switch(role) {
            case PAGE: return 4;
            case SECTION: return 4;
            case ITEM: return 4;
            case QUESTION: return 4;
            case SCORE: return 2;
            case PREVIEW: return 2;
            case HINT: return 8;
            case META: return 4;
            default: return 8;
        }
    }
    private void setRoleSpacing(TextView view,TypeRole role) {
        view.setPadding(0,dp(topSpace(role)),0,dp(bottomSpace(role)));
    }
    private String readableGuide(String s) { return s.replaceAll("([。！？])(?=[^\n])","$1\n"); }
    private void styledLabel(LinearLayout parent,String content,TypeRole role) {
        TextView view=styledText(role==TypeRole.HINT?readableGuide(content):content,role);
        setRoleSpacing(view,role);
        parent.addView(view);
    }
    private void styledLabel(LinearLayout parent,String content,TypeRole role,int color) {
        TextView view=styledText(role==TypeRole.HINT?readableGuide(content):content,role);
        view.setTextColor(color);
        setRoleSpacing(view,role);
        parent.addView(view);
    }
    private void sectionLabel(LinearLayout parent,String s) {styledLabel(parent,s,TypeRole.SECTION);}
    private void compactItem(LinearLayout parent,String s,int color) {TextView t=styledText(s,TypeRole.PREVIEW);t.setTextColor(color);setRoleSpacing(t,TypeRole.PREVIEW);parent.addView(t);}
    private View statusPill(String status,int textSize) {
        boolean inactive=status.equals("暂未奏效")||status.equals("已试过方法");
        boolean complete=status.equals("已完成")||status.equals("已记录")||status.startsWith("已探索");
        boolean pending=status.equals("待探索");
        int color=inactive||pending?MUTED:complete?GREEN:BLUE;
        int background=inactive||pending?Color.rgb(239,241,240):complete?Color.rgb(229,244,237):SOFT;
        int icon=inactive?R.drawable.ic_status_no_help:pending?R.drawable.ic_status_pending:complete?R.drawable.ic_status_complete:R.drawable.ic_status_progress;
        LinearLayout badges=new LinearLayout(this);badges.setGravity(Gravity.CENTER_VERTICAL);
        badges.setContentDescription(status);badges.setTooltipText(status);badges.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        statusBadge(badges,icon,color,background,textSize);
        if(status.contains("有草稿"))statusBadge(badges,R.drawable.ic_status_progress,BLUE,SOFT,textSize);
        return badges;
    }
    private void statusBadge(LinearLayout parent,int icon,int color,int background,int textSize) {
        FrameLayout badge=new FrameLayout(this);badge.setBackground(bg(background,14));
        badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ImageView image=new ImageView(this);image.setImageResource(icon);image.setImageTintList(ColorStateList.valueOf(color));
        int size=Math.round(textSize*getResources().getDisplayMetrics().scaledDensity);
        badge.addView(image,new FrameLayout.LayoutParams(size,size,Gravity.CENTER));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(dp(28),dp(28));
        if(parent.getChildCount()>0)layout.leftMargin=dp(4);
        parent.addView(badge,layout);
    }
    private void scoreRow(LinearLayout parent,String name,String score) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(topSpace(TypeRole.SCORE)),0,dp(bottomSpace(TypeRole.SCORE)));
        row.addView(styledText(name,TypeRole.SCORE),new LinearLayout.LayoutParams(0,-2,1));TextView value=styledText(score,TypeRole.VALUE);value.setGravity(Gravity.END);value.setMaxWidth(dp(190));row.addView(value,new LinearLayout.LayoutParams(-2,-2));parent.addView(row);
    }
    private void title(String s,String sub) { TextView t=styledText(s,TypeRole.PAGE);setRoleSpacing(t,TypeRole.PAGE);body.addView(t);styledLabel(body,sub,TypeRole.HINT);gap(body,componentSpace(ComponentRelation.NEXT_ITEM)); }
    private void gap(LinearLayout p,int height) { View v=new View(this); p.addView(v,new LinearLayout.LayoutParams(1,dp(height))); }
    private Button button(String s,boolean primary,Runnable action) {
        Button b=new Button(this); b.setText(s); b.setTextSize(15); b.setAllCaps(false); b.setTextColor(primary?Color.WHITE:GREEN);
        b.setLetterSpacing(0);b.setStateListAnimator(null);b.setElevation(0);b.setPadding(dp(12),0,dp(12),0);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22176B5B),bg(primary?GREEN:Color.rgb(239,245,242),8),null)); b.setMinHeight(dp(50));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52)); lp.topMargin=dp(componentSpace(ComponentRelation.SAME_GROUP)); b.setLayoutParams(lp); b.setOnClickListener(v->action.run()); return b;
    }
    private void shell(String nav) {
        cancelAi();
        root=col(); root.setBackgroundColor(Color.WHITE); root.setPadding(0,0,0,dp(12));
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(screen.equals("home")?dp(22):0,dp(10),dp(22),dp(8));
        if (!screen.equals("home")) {
            ImageButton back=iconButton(R.drawable.ic_arrow_back,"返回日志列表",()->leavePage());back.setPadding(dp(5),dp(10),dp(10),dp(10));
            bar.addView(back,new LinearLayout.LayoutParams(dp(40),dp(48)));
        }
        TextView brand=styledText(nav,TypeRole.ITEM);brand.setTextColor(GREEN);bar.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1)); brand.setGravity(Gravity.CENTER_VERTICAL);
        if (screen.equals("home")) {
            bar.addView(iconButton(R.drawable.ic_settings,"AI 连接",()->configureAi()),new LinearLayout.LayoutParams(dp(48),dp(48)));
        }
        root.addView(bar);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); body=col(); body.setPadding(dp(22),0,dp(22),dp(24)); scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }
    private String date(long millis) { return new SimpleDateFormat("M月d日 HH:mm",Locale.CHINA).format(new Date(millis)); }
    private void home() {
        active=null; screen="home"; shell("心晴  /  情绪日志");
        styledLabel(body,new SimpleDateFormat("yyyy年M月d日  EEEE",Locale.CHINA).format(new Date()),TypeRole.META);
        title("给此刻的自己，\n一点理解。","从一个具体的瞬间开始。");
        body.addView(button("开始一篇情绪日志",true,()->create()));
        int complete=0; for(int i=0;i<entries.length();i++) if(entries.optJSONObject(i).optBoolean("done")) complete++;
        gap(body,componentSpace(ComponentRelation.NEXT_SECTION)); styledLabel(body,"我的记录    "+entries.length()+" 篇    已完成 "+complete+" 篇",TypeRole.SECTION);
        if(entries.length()==0) { gap(body,componentSpace(ComponentRelation.NEXT_ITEM)); styledLabel(body,"还没有日志",TypeRole.SECTION); styledLabel(body,"今天，哪个瞬间让你有些难受？",TypeRole.HINT); gap(body,30); }
        for(int i=entries.length()-1;i>=0;i--) {
            final JSONObject item=entries.optJSONObject(i);
            LinearLayout row=col(); row.setPadding(dp(16),dp(12),dp(16),dp(12)); row.setBackground(bg(Color.rgb(245,248,247),8));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(componentSpace(ComponentRelation.NEXT_ITEM)); body.addView(row,lp);
            LinearLayout stateRow=new LinearLayout(this);stateRow.setGravity(Gravity.CENTER_VERTICAL);stateRow.addView(statusPill(item.optBoolean("done")?"已完成":"进行中",13));TextView when=styledText("  "+date(item.optLong("created")),TypeRole.META);stateRow.addView(when);row.addView(stateRow);
            TextView heading=styledText(item.optString("event","").trim().isEmpty()?"未命名的瞬间":item.optString("event"),TypeRole.ITEM); heading.setMaxLines(2); heading.setEllipsize(TextUtils.TruncateAt.END); row.addView(heading);
            styledLabel(row,item.optBoolean("done")?"查看这次记录":"继续  "+STEPS[item.optInt("step")],TypeRole.META);
            row.setOnClickListener(v->{active=item; if(item.optBoolean("done")) summary(); else {step=item.optInt("step");editor();}});
            row.setOnLongClickListener(v->{deleteEntry(item);return true;});
        }
        gap(body,componentSpace(ComponentRelation.NEXT_SECTION)); body.addView(button("关于方法与隐私",false,()->about()));
    }
    private void create() {
        if(!storageReadable){toast("数据读取异常，暂时无法创建日志");return;}
        active=new JSONObject(); put(active,"id",UUID.randomUUID().toString()); put(active,"created",System.currentTimeMillis()); put(active,"emotions",new JSONObject()); put(active,"thoughts",new JSONArray());
        prepareEntry(active);
        entries.put(active); step=0; persist(); editor();
    }
    private void field(LinearLayout parent,String name,String hint,JSONObject obj,String key,int lines) {
        field(parent,name,hint,obj,key,lines,true);
    }
    private void field(LinearLayout parent,String name,String hint,JSONObject obj,String key,int lines,boolean showLabel) {
        field(parent,name,hint,obj,key,lines,showLabel,ComponentRelation.NEXT_QUESTION);
    }
    private void field(LinearLayout parent,String name,String hint,JSONObject obj,String key,int lines,boolean showLabel,ComponentRelation next) {
        if(showLabel){TextView caption=styledText(name,TypeRole.QUESTION);setRoleSpacing(caption,TypeRole.QUESTION);parent.addView(caption);} EditText input=new EditText(this); input.setTextSize(16); input.setLineSpacing(dp(4),1); input.setTextColor(INK); input.setHintTextColor(MUTED);
        input.setGravity(Gravity.TOP); input.setMinLines(lines); input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setContentDescription(name); input.setSaveEnabled(false);
        GradientDrawable fieldBackground=bg(Color.rgb(247,249,248),8);fieldBackground.setStroke(dp(1),Color.rgb(226,233,229));
        input.setPadding(dp(14),dp(14),dp(14),dp(14)); input.setBackground(fieldBackground); input.setHint(hint); input.setText(obj.optString(key));LinearLayout.LayoutParams fieldLayout=new LinearLayout.LayoutParams(-1,-2);fieldLayout.bottomMargin=dp(componentSpace(next));parent.addView(input,fieldLayout);
        input.addTextChangedListener(new TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){put(obj,key,s.toString());if(key.equals("text"))obj.remove("recommendations");changed();} public void afterTextChanged(Editable e){} });
    }
    private void slider(LinearLayout parent,String name,JSONObject obj,String key,int fallback) {
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setPadding(0,dp(topSpace(TypeRole.SCORE)),0,0);
        TextView caption=styledText(name,TypeRole.SCORE);heading.addView(caption,new LinearLayout.LayoutParams(0,-2,1));
        TextView value=styledText(obj.has(key)?obj.optInt(key)+"%":"未评估",TypeRole.VALUE);value.setGravity(Gravity.END);heading.addView(value,new LinearLayout.LayoutParams(dp(76),-2));parent.addView(heading);
        SeekBar seek=new SeekBar(this); seek.setMax(100); seek.setProgress(obj.optInt(key,fallback)); seek.setProgressTintList(ColorStateList.valueOf(GREEN)); seek.setThumbTintList(ColorStateList.valueOf(GREEN));
        seek.setContentDescription(name); parent.addView(seek,new LinearLayout.LayoutParams(-1,dp(48)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){put(obj,key,s.getProgress());value.setText(s.getProgress()+"%");changed();} public void onProgressChanged(SeekBar s,int p,boolean user){if(user){put(obj,key,p);value.setText(p+"%");changed();}} });
    }
    private JSONObject emotions() { return active.optJSONObject("emotions"); }
    private JSONArray thoughts() { return active.optJSONArray("thoughts"); }
    private boolean hasEmotion(String name) { return emotions().optJSONObject(name).optInt("before")>0; }
    private void guide(LinearLayout parent,String heading,String explanation) {
        LinearLayout section=col();
        TextView toggle=styledText("+  "+heading,TypeRole.HINT);toggle.setTextColor(GREEN);toggle.setGravity(Gravity.CENTER_VERTICAL);toggle.setMinHeight(dp(48));toggle.setFocusable(true);toggle.setBackground(touchBackground(8));
        TextView content=styledText(readableGuide(explanation),TypeRole.HINT);content.setPadding(dp(10),0,dp(10),dp(componentSpace(ComponentRelation.NEXT_QUESTION)));content.setVisibility(View.GONE);
        toggle.setContentDescription(heading+"，已折叠");
        toggle.setOnClickListener(v->{boolean expand=content.getVisibility()!=View.VISIBLE;content.setVisibility(expand?View.VISIBLE:View.GONE);toggle.setText((expand?"−  ":"+  ")+heading);toggle.setContentDescription(heading+(expand?"，已展开":"，已折叠"));});
        section.addView(toggle);section.addView(content);parent.addView(section);
    }
    private void aiHelp(LinearLayout parent,String task,JSONObject thought,JSONObject attempt,int page) {
        aiHelp(parent,task,thought,attempt,page,()->editor());
    }
    private void aiHelp(LinearLayout parent,String task,JSONObject thought,JSONObject attempt,int page,Runnable refresh) {
        String heading=task.equals("寻找想法")?"脑海中的想法":task.equals("识别认知歪曲")?"可能的认知歪曲":task.equals("选择方法")?"可以尝试的方法":"本次回应";
        aiHeading(parent,heading,TypeRole.SECTION,task,thought,attempt,page,refresh);
    }
    private void aiHeading(LinearLayout parent,String heading,TypeRole headingRole,String task,JSONObject thought,JSONObject attempt,int page,Runnable refresh) {
        aiHeading(parent,heading,headingRole,task,thought,attempt,page,refresh,null);
    }
    private void aiHeading(LinearLayout parent,String heading,TypeRole headingRole,String task,JSONObject thought,JSONObject attempt,int page,Runnable refresh,String hint) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        int headingSize=headingRole==TypeRole.SECTION?17:16;
        TextView question=styledText(heading,headingRole);question.setPadding(0,0,dp(8),0);
        if(task.equals("识别认知歪曲")){
            row.addView(question,new LinearLayout.LayoutParams(-2,-2));
            TextView help=text("?",headingSize,GREEN);help.setGravity(Gravity.CENTER);help.setTypeface(null,Typeface.BOLD);help.setContentDescription("查看全部认知歪曲说明");help.setTooltipText("全部认知歪曲说明");help.setFocusable(true);help.setBackground(touchBackground(24));help.setOnClickListener(v->distortionHelp());row.addView(help,new LinearLayout.LayoutParams(dp(32),dp(48)));
            row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
            row.post(()->question.setMaxWidth(Math.max(dp(48),row.getWidth()-dp(80))));
        }else row.addView(question,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout slot=new FrameLayout(this);
        ImageButton ai=iconButton(R.drawable.ic_ai_assist,"AI："+task,()->{});ai.setImageTintList(ColorStateList.valueOf(GREEN));int aiInset=Math.max(0,(dp(48)-Math.round(headingSize*getResources().getDisplayMetrics().scaledDensity))/2);ai.setPadding(aiInset,aiInset,aiInset,aiInset);ai.setTooltipText("AI 补充："+task);
        ai.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22176B5B),null,bg(Color.WHITE,24)));
        ProgressBar spinner=new ProgressBar(this);spinner.setIndeterminateTintList(ColorStateList.valueOf(GREEN));spinner.setVisibility(View.GONE);
        slot.addView(ai,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout.LayoutParams spinParams=new FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER);slot.addView(spinner,spinParams);
        row.addView(slot,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout.LayoutParams rowLayout=new LinearLayout.LayoutParams(-1,-2);rowLayout.topMargin=dp(topSpace(headingRole));parent.addView(row,rowLayout);
        if(hint!=null&&!hint.isEmpty()){
            TextView explanation=styledText(readableGuide(hint),TypeRole.HINT);
            setRoleSpacing(explanation,TypeRole.HINT);
            LinearLayout.LayoutParams hintLayout=new LinearLayout.LayoutParams(-1,-2);hintLayout.rightMargin=dp(48);parent.addView(explanation,hintLayout);
            question.post(()->{
                int slack=Math.max(0,(row.getHeight()-question.getHeight())/2);
                int overlap=Math.min(dp(10),Math.max(0,slack-dp(2)));
                if(hintLayout.topMargin!=-overlap){hintLayout.topMargin=-overlap;explanation.setLayoutParams(hintLayout);}
            });
        }
        TextView status=styledText("",TypeRole.META);status.setVisibility(View.GONE);parent.addView(status);
        Runnable reset=()->{spinner.setVisibility(View.GONE);ai.setVisibility(View.VISIBLE);slot.setContentDescription(null);slot.setClickable(false);};
        ai.setOnClickListener(v->{
            if(!AiKeyStore.configured(this)){configureAi();toast("先保存密钥，再点击 AI 补充");return;}
            final String key;try{key=AiKeyStore.read(this);}catch(Exception e){toast("无法读取密钥，请重新设置");return;}
            cancelAi();final int generation=aiGeneration;final JSONObject owner=active;
            final String context;
            try{context=AiPrompts.context(task,owner,thought,attempt,page).toString();}catch(Exception e){status.setText("当前内容无法发送，请重试");status.setVisibility(View.VISIBLE);return;}
            aiSource=slot;resetAi=reset;ai.setVisibility(View.INVISIBLE);spinner.setVisibility(View.VISIBLE);status.setVisibility(View.GONE);
            slot.setContentDescription("取消 AI："+task);slot.setOnClickListener(w->{cancelAi();status.setText("已取消");status.setVisibility(View.VISIBLE);});
            aiRequest=new AiClient.Request(key,AiPrompts.instruction(task),context,(reply,error)->handler.post(()->{
                if(isDestroyed()||generation!=aiGeneration||owner!=active||!slot.isAttachedToWindow())return;
                String failure=error;
                if(failure==null)try{
                    JSONObject data=AiSuggestions.parse(task,reply);
                    if(data.has("support")){reset.run();aiSource=null;aiRequest=null;resetAi=null;status.setText(data.getString("support"));status.setVisibility(View.VISIBLE);return;}
                    int count=AiSuggestions.apply(task,data,owner,thought,attempt,page);
                    JSONArray notes=owner.optJSONArray("aiNotes");if(notes==null){notes=new JSONArray();put(owner,"aiNotes",notes);}
                    JSONObject note=new JSONObject();put(note,"task",task);put(note,"data",data);put(note,"answer",AiSuggestions.describe(task,data));put(note,"created",System.currentTimeMillis());notes.put(note);
                    reset.run();aiSource=null;aiRequest=null;resetAi=null;changed();persist();refresh.run();if(count==0)toast("暂无可补充内容");return;
                }catch(Exception invalid){failure="AI 格式不正确，未写入；可再点 AI 重试";}
                reset.run();aiSource=null;aiRequest=null;resetAi=null;status.setText(failure);status.setVisibility(View.VISIBLE);
            }));
            aiRequest.start();
        });
        slot.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View view){}
            public void onViewDetachedFromWindow(View view){if(aiSource==slot)cancelAi();}
        });
    }
    private void cancelAi() {
        aiGeneration++;if(aiRequest!=null)aiRequest.cancel();if(resetAi!=null)resetAi.run();aiRequest=null;aiSource=null;resetAi=null;
    }
    private void configureAi() {
        LinearLayout content=col();content.setPadding(dp(20),dp(8),dp(20),dp(16));
        styledLabel(content,"DeepSeek  "+AiClient.MODEL,TypeRole.SECTION);
        styledLabel(content,"只有点击 AI 求助才会发送当前情境及相关内容给 DeepSeek。不点击即可完整使用离线日志。",TypeRole.HINT);
        styledLabel(content,AiKeyStore.configured(this)?"密钥已保存；输入可替换。":"填写 API Key 后即可主动求助。",TypeRole.HINT);
        EditText key=new EditText(this);key.setHint("API Key");key.setContentDescription("DeepSeek API Key");key.setSingleLine(true);key.setInputType(129);key.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);key.setSaveEnabled(false);key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);content.addView(key);
        AlertDialog dialog=new AlertDialog.Builder(this).setCustomTitle(dialogHeader("AI 连接")).setView(content).setPositiveButton("保存",null).setNegativeButton("取消",null)
            .setNeutralButton("移除密钥",(d,w)->{try{AiKeyStore.clear(this);toast("密钥已移除");}catch(Exception e){toast("移除失败，请重试");}}).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String secret=key.getText().toString().trim();
            if(!secret.matches("sk-[A-Za-z0-9_-]{12,}")){key.setError("请填写有效的 DeepSeek API Key");return;}
            try{AiKeyStore.save(this,secret);key.setText("");toast("密钥已加密保存");dialog.dismiss();}catch(Exception e){toast("保存失败，请重试");}
        }));showDialog(dialog);dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(DANGER);dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    private String thoughtStatus(JSONObject thought) {
        JSONArray records=thought.optJSONArray("attempts");boolean draft=false;
        if(records!=null)for(int i=0;i<records.length();i++)if(!records.optJSONObject(i).optBoolean("done")&&!records.optJSONObject(i).optBoolean("noHelp"))draft=true;
        if(isHandled(thought))return draft?"已探索  有草稿":"已探索";
        if(records!=null&&records.length()>0)return draft?"探索中":"已试过方法";
        JSONObject ds=thought.optJSONObject("distortions");boolean hasChoice=false;
        if(ds!=null)for(String name:DISTORTIONS)if(ds.optBoolean(name))hasChoice=true;
        return hasChoice||!thought.optString("response").trim().isEmpty()||!thought.optString("evidence").trim().isEmpty()||thought.has("responseBelief")?"探索中":"待探索";
    }
    private void editor() {
        screen="edit"; put(active,"step",step); changed(); shell("心晴  /  正在记录");
        styledLabel(body,"第 "+(step+1)+" / "+STEPS.length+" 步    "+STEPS[step],TypeRole.META,GREEN);
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(STEPS.length); progress.setProgress(step+1); progress.setProgressTintList(ColorStateList.valueOf(GREEN)); body.addView(progress,new LinearLayout.LayoutParams(-1,dp(4)));
        if(step==0) {
            title("发生了什么？","选一个让你难受的瞬间，先写发生的事，把对自己的评价留到后面的想法里。可以先记一部分，之后继续。");
            field(body,"当时的情境","先描述你看到、听到或遇到的事情。",active,"event",5);
            field(body,"时间、地点与在场的人（可选）","把当时的场景留在这里",active,"context",2);
        } else if(step==1) {
            title("此刻有哪些感受？","回想刚写下的情境，评估你现在的感受。0 是没有，100 是非常强烈；每组估一个程度即可，不必精确。");
            guide(body,"分不清感受时","看看哪组词最贴近你，按这一组的整体感受评分，不必每个词都符合。没有的感受可以保持 0。\n\n这里记下的是开始练习时回想这件事的感受，结束时会再次回想同一情境比较。");
            for(int i=0;i<EMOTIONS.length;i++) {
                styledLabel(body,EMOTION_WORDS[i],TypeRole.ITEM);
                slider(body,EMOTIONS[i]+"程度",emotions().optJSONObject(EMOTIONS[i]),"before",0);
                View divider=new View(this);divider.setBackgroundColor(Color.rgb(233,238,235));body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));gap(body,componentSpace(ComponentRelation.SAME_GROUP));
            }
        } else if(step==2) {
            title("脑海里出现了什么想法？","回想这件事时，哪些判断让你难受？先原样记下至少一条，每条一个意思，现在不用纠正它。");
            styledLabel(body,"相信程度：0 是完全不相信这句话，100 是完全相信；它与难受的强度不同。",TypeRole.HINT);
            guide(body,"想不到具体想法时","这件事对你意味着什么？你担心它说明了自己的什么问题，或意味着怎样的结果？先记录脑中真实出现的判断，不用写成标准答案。");
            aiHelp(body,"寻找想法",null,null,0);
            for(int i=0;i<thoughts().length();i++) {
                final int index=i; JSONObject t=thoughts().optJSONObject(i);
                if(i>0){gap(body,componentSpace(ComponentRelation.NEXT_ITEM));View divider=new View(this);divider.setBackgroundColor(Color.rgb(218,228,223));body.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));gap(body,componentSpace(ComponentRelation.NEXT_QUESTION));}
                LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
                TextView thoughtTitle=styledText("想法 "+(i+1),TypeRole.ITEM);heading.addView(thoughtTitle,new LinearLayout.LayoutParams(0,-2,1));
                heading.addView(iconButton(R.drawable.ic_delete,"移除想法 "+(i+1),()->removeThought(index)),new LinearLayout.LayoutParams(dp(48),dp(48)));body.addView(heading);
                field(body,"想法内容 "+(i+1),"写下脑海中出现的判断，每条一个意思。",t,"text",2,false,ComponentRelation.FIELD_TO_SCORE);
                slider(body,"此时对这句话的相信程度",t,"before",50);
                gap(body,componentSpace(ComponentRelation.NEXT_ITEM));
            }
            body.addView(button("添加一个想法",false,()->{thoughts().put(new JSONObject());changed();editor();}));
        } else if(step==3) {
            title("正向重构","看看这些情绪和想法为何可以理解、可能起什么作用、体现什么可贵之处。想不到可以留空，之后再补。");
            guide(body,"想不到作用或品质时","先挑一种感受或一句想法：如果它消失，你担心自己会忽略什么？这可以是线索，答案仍要回到具体的感受或想法。\n\n只留下符合自己的解释，不必认定原想法为真，也不必证明痛苦一定有好处。");
            sectionLabel(body,"刚才记录的情绪");
            boolean any=false;
            for(int i=0;i<EMOTIONS.length;i++)if(hasEmotion(EMOTIONS[i])){compactItem(body,EMOTION_WORDS[i],INK);any=true;}
            if(!any)compactItem(body,"没有标记出明显的情绪",MUTED);
            sectionLabel(body,"刚才记录的想法");
            for(int i=0;i<thoughts().length();i++){JSONObject t=thoughts().optJSONObject(i);compactItem(body,(i+1)+". "+t.optString("text"),INK);}
            gap(body,componentSpace(ComponentRelation.NEXT_ITEM));
            TextView understanding=styledText("为什么这份感受或想法可以理解？（可选）",TypeRole.QUESTION);
            LinearLayout.LayoutParams understandingLayout=new LinearLayout.LayoutParams(-1,-2);understandingLayout.topMargin=dp(topSpace(TypeRole.QUESTION));body.addView(understanding,understandingLayout);
            styledLabel(body,"结合当时的处境，想想为何会有这种反应；不必认定原想法为真。",TypeRole.HINT);
            field(body,"为什么这份感受或想法可以理解？（可选）","写下你的理解",active,"understanding",3,false);
            aiHeading(body,"这份感受或想法可能起什么作用？",TypeRole.QUESTION,"重构益处",null,null,0,()->editor(),"它可能在提醒、保护或推动你做什么？答案要针对感受或想法，而非事件本身。");
            field(body,"这份感受或想法可能起什么作用？","写下你想到的作用",active,"benefits",3,false);
            aiHeading(body,"这份感受或想法可能体现出我的什么可贵之处？",TypeRole.QUESTION,"重构品质",null,null,0,()->editor(),"想想它可能反映的品质或核心价值；事件目标和担心的结果只是线索。");
            field(body,"这份感受或想法可能体现出我的什么可贵之处？","写下可能体现的可贵之处",active,"values",3,false);
        } else if(step==4) {
            title("魔法刻度盘","想象眼前的问题暂时没变，但有一个刻度盘能调整你的感受。如果仍能保留你珍视的品质，你愿意让每种情绪停在多少？");
            styledLabel(body,"这是希望达到的目标，不是现在已经达到的程度。可以保留一些情绪，也可以暂时不改变。",TypeRole.HINT);
            String dialHelp="有没有一个程度，让你仍能认真面对这件事，又不至于被感受淹没？先估一个愿意尝试的目标，以后可以调整。\n\n不是要猜正确数字，也不要求滑动后立刻感觉好转。调整感受不妨碍你继续解决现实问题。";
            if(!active.optString("values").trim().isEmpty())dialHelp+="\n\n刚才写下的可贵之处：\n"+active.optString("values");
            guide(body,"目标强度怎么选？",dialHelp);
            boolean any=false;
            for(String name:EMOTIONS) if(hasEmotion(name)) {
                JSONObject e=emotions().optJSONObject(name);
                slider(body,name+"期望程度",e,"target",0);any=true;
            }
            if(!any)styledLabel(body,"目前各组情绪均为 0%，可以直接继续。",TypeRole.HINT);
        } else if(step==5) {
            title("探索想法","点开一句你愿意看看的想法，检查它的依据，再尝试自己认可的回应。可以探索多条，也可以这次先不探索，直接进入下一步。");
            styledLabel(body,"划线只表示结束了本次探索，不代表想法已消失或必须感觉好转。",TypeRole.HINT);
            for(int i=0;i<thoughts().length();i++) {
                final int index=i;JSONObject thought=thoughts().optJSONObject(i);boolean handled=isHandled(thought);
                LinearLayout row=col();row.setPadding(dp(14),dp(10),dp(14),dp(14));row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22176B5B),bg(Color.rgb(245,248,246),8),null));
                LinearLayout stateRow=new LinearLayout(this);stateRow.setGravity(Gravity.CENTER_VERTICAL);TextView thoughtLabel=styledText("想法 "+(i+1),TypeRole.ITEM);stateRow.addView(thoughtLabel);LinearLayout.LayoutParams stateMargin=new LinearLayout.LayoutParams(-2,-2);stateMargin.leftMargin=dp(10);stateRow.addView(statusPill(thoughtStatus(thought),16),stateMargin);row.addView(stateRow);
                if(handled&&thought.has("after"))scoreRow(row,"当前相信程度",percent(thought,"after"));
                TextView content=styledText(thought.optString("text"),TypeRole.BODY);content.setTextColor(handled?MUTED:INK);
                if(handled)content.setPaintFlags(content.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                row.addView(content);row.setFocusable(true);row.setContentDescription("探索想法 "+(i+1));row.setOnClickListener(v->thoughtDialog(index));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(componentSpace(ComponentRelation.NEXT_ITEM));body.addView(row,lp);
            }
        } else {
            title("现在，感觉如何？","再回想最初的情境，看看现在的感受。分数可以降低、不变或升高，不用凑到目标值。");
            guide(body,"变化不明显时","新回应有没有哪部分你还不相信，或没有触及真正的担心？可以回去把它改得更具体，稍后换个角度再试，也可以先停在这里。\n\n没有变化不等于练习失败，更不用为了完成日志把分数调低。");
            for(String name:EMOTIONS) if(hasEmotion(name)) {
                JSONObject e=emotions().optJSONObject(name);slider(body,name+"现在的强度",e,"after",0);
            }
            field(body,"接下来，给自己的一小步（可选）","一件具体、可行的小事",active,"action",3);
        }
        LinearLayout footer=new LinearLayout(this); footer.setGravity(Gravity.CENTER_VERTICAL);footer.setPadding(dp(22),dp(10),dp(22),0);
        if(step>0) { Button prev=button("上一步",false,()->{step--;editor();}); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1);lp.rightMargin=dp(10);footer.addView(prev,lp); }
        Button next=button(step==STEPS.length-1?"完成日志":"下一步",true,()->{if(!validate())return; if(step<STEPS.length-1){step++;editor();}else{put(active,"done",true);persist();summary();}}); footer.addView(next,new LinearLayout.LayoutParams(0,dp(52),2)); root.addView(footer);
    }
    private boolean isHandled(JSONObject thought) {
        return thought.optBoolean("handled",isSelected(thought))&&!thought.optString("response").trim().isEmpty()&&thought.has("responseBelief");
    }
    private void thoughtDialog(int index) {
        JSONObject thought=thoughts().optJSONObject(index);if(!thought.has("distortions"))put(thought,"distortions",new JSONObject());
        LinearLayout detail=col();detail.setPadding(dp(20),dp(8),dp(20),dp(16));styledLabel(detail,thought.optString("text"),TypeRole.ITEM);
        styledLabel(detail,"先识别这句话可能包含的歪曲，再结合它选择方法。可以多选；没有发现时也可以继续。",TypeRole.HINT);
        final AlertDialog[] host={null};
        aiHelp(detail,"识别认知歪曲",thought,null,0,()->{host[0].dismiss();thoughtDialog(index);});distortions(detail,thought);
        ScrollView scroll=new ScrollView(this);scroll.addView(detail);
        host[0]=new AlertDialog.Builder(this).setCustomTitle(dialogHeader("1  识别认知歪曲")).setView(dialogFrame(scroll))
            .setPositiveButton("继续",(d,w)->methodDialog(index)).setNegativeButton("保存并退出",null).create();
        host[0].setOnDismissListener(d->{changed();editor();});showDialog(host[0]);
    }
    private void methodDialog(int index) {
        JSONObject thought=thoughts().optJSONObject(index);
        LinearLayout detail=col();detail.setPadding(dp(20),dp(8),dp(20),dp(16));
        styledLabel(detail,thought.optString("text"),TypeRole.ITEM);
        styledLabel(detail,"已识别："+distortionNames(thought),TypeRole.HINT,GREEN);
        styledLabel(detail,"选一种方法试试看。没有帮助就换一种，不必在同一种方法上反复用力。每种方法只有一份记录，点回去可继续草稿或查看结果。",TypeRole.HINT);
        final AlertDialog[] host={null};
        aiHelp(detail,"选择方法",thought,null,0,()->{host[0].dismiss();methodDialog(index);});
        ScrollView scroll=new ScrollView(this);scroll.addView(detail);
        AlertDialog chooser=new AlertDialog.Builder(this).setCustomTitle(dialogHeader("2  选择方法")).setView(dialogFrame(scroll))
            .setNegativeButton("保存并退出",null)
            .setNeutralButton("识别歪曲",(d,w)->thoughtDialog(index)).create();host[0]=chooser;
        JSONArray records=attempts(thought);
        ArrayList<ThoughtMethods> ordered=new ArrayList<>();JSONObject reasons=new JSONObject();JSONArray recommendations=thought.optJSONArray("recommendations");
        if(recommendations!=null)for(int i=0;i<recommendations.length();i++){JSONObject recommendation=recommendations.optJSONObject(i);ThoughtMethods method=ThoughtMethods.find(recommendation.optString("id"));
            if(method!=null&&findMethodRecord(records,method.id)==null&&!ordered.contains(method)){ordered.add(method);put(reasons,method.id,recommendation.optString("reason"));}}
        for(ThoughtMethods method:CbtReferences.ranked(thought))if(!ordered.contains(method))ordered.add(method);
        for(ThoughtMethods method:ordered){
            JSONObject existing=findMethodRecord(records,method.id);
            String status=existing==null?"未使用":methodStatus(existing);
            String reason=reasons.optString(method.id);
            detail.addView(methodChoice(method.name,status,method.purpose,reason,()->{
                JSONObject attempt=findMethodRecord(records,method.id);
                if(attempt==null){
                    attempt=new JSONObject();put(attempt,"id",UUID.randomUUID().toString());put(attempt,"created",System.currentTimeMillis());put(attempt,"method",method.id);put(attempt,"name",method.name);put(attempt,"step",0);
                    put(attempt,"thought",thought.optString("text"));put(attempt,"before",thought.optInt("after",thought.optInt("before")));records.put(attempt);
                }
                changed();chooser.dismiss();
                if(attempt.optBoolean("done")||attempt.optBoolean("noHelp"))attemptHistory(index,attempt);else attemptDialog(index,attempt);
            }));
        }
        boolean archiveHeading=false;
        for(int i=records.length()-1;i>=0;i--){final JSONObject old=records.optJSONObject(i);
            if(ThoughtMethods.find(old.optString("method"))==null||findMethodRecord(records,old.optString("method"))!=old){
                if(!archiveHeading){styledLabel(detail,"旧版保留记录",TypeRole.SECTION);archiveHeading=true;}
                detail.addView(methodRow(old.optString("name")+"  旧记录 "+(i+1),"查看原有内容",()->{chooser.dismiss();attemptHistory(index,old);}));
            }
        }
        chooser.setOnDismissListener(d->{changed();editor();});showDialog(chooser);
    }
    private JSONObject findMethodRecord(JSONArray records,String method) {
        for(int i=records.length()-1;i>=0;i--){JSONObject record=records.optJSONObject(i);if(method.equals(record.optString("method")))return record;}return null;
    }
    private String methodStatus(JSONObject record) {
        if(record.optBoolean("noHelp"))return "暂未奏效";
        return record.optBoolean("done")?"已记录":"填写中";
    }
    private android.widget.FrameLayout dialogFrame(View content) {
        android.widget.FrameLayout frame=new android.widget.FrameLayout(this) {
            @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
                int limit=dp(440);
                if(View.MeasureSpec.getMode(heightMeasureSpec)!=View.MeasureSpec.UNSPECIFIED)
                    limit=Math.min(limit,View.MeasureSpec.getSize(heightMeasureSpec));
                super.onMeasure(widthMeasureSpec,View.MeasureSpec.makeMeasureSpec(limit,View.MeasureSpec.AT_MOST));
            }
        };
        if(content instanceof ScrollView){ScrollView scroll=(ScrollView)content;scroll.setFillViewport(false);scroll.setSmoothScrollingEnabled(true);}
        frame.addView(content,new android.widget.FrameLayout.LayoutParams(-1,-2));
        return frame;
    }
    private View methodChoice(String heading,String state,String purpose,String reason,Runnable action) {
        LinearLayout card=col();card.setPadding(dp(14),dp(12),dp(14),dp(12));
        GradientDrawable background=bg(Color.rgb(247,249,248),10);background.setStroke(dp(1),Color.rgb(223,233,228));
        card.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22176B5B),background,null));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=styledText(heading,TypeRole.ITEM);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        if(!state.equals("未使用"))top.addView(statusPill(state,17));card.addView(top);
        TextView description=styledText(purpose,TypeRole.HINT);description.setPadding(0,dp(5),0,0);card.addView(description);
        if(!reason.isEmpty()){
            TextView recommendation=styledText("AI 推荐  "+reason,TypeRole.META);recommendation.setTextColor(BLUE);recommendation.setTypeface(null,Typeface.BOLD);recommendation.setPadding(dp(10),dp(8),dp(10),dp(8));
            LinearLayout.LayoutParams suggestLayout=new LinearLayout.LayoutParams(-1,-2);suggestLayout.topMargin=dp(componentSpace(ComponentRelation.SAME_GROUP));
            recommendation.setBackground(bg(SOFT,7));card.addView(recommendation,suggestLayout);
        }
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.bottomMargin=dp(componentSpace(ComponentRelation.NEXT_ITEM));
        card.setFocusable(true);card.setContentDescription(heading+(state.equals("未使用")?"":"，"+state));card.setOnClickListener(v->action.run());card.setLayoutParams(layout);return card;
    }
    private View methodRow(String heading,String subtitle,Runnable action) {
        LinearLayout row=col();row.setPadding(dp(8),dp(8),dp(8),dp(8));styledLabel(row,heading,TypeRole.ITEM);styledLabel(row,subtitle,TypeRole.META);
        row.setBackground(touchBackground(8));row.setFocusable(true);row.setContentDescription(heading);row.setOnClickListener(v->action.run());return row;
    }
    private JSONArray attempts(JSONObject thought) {
        if(!thought.has("attempts")) {
            JSONArray records=new JSONArray();
            if(!thought.optString("response").isEmpty()||!thought.optString("evidence").isEmpty()||thought.has("responseBelief")) {
                JSONObject old=new JSONObject();put(old,"name","此前的自由探索");put(old,"method","legacy");put(old,"thought",thought.optString("text"));
                for(String key:new String[]{"response","evidence","before","after","responseBelief"})if(thought.has(key))put(old,key,thought.opt(key));
                put(old,"done",isHandled(thought));records.put(old);
            }
            put(thought,"attempts",records);
        }
        return thought.optJSONArray("attempts");
    }
    private void attemptDialog(int index,JSONObject attempt) {
        JSONObject thought=thoughts().optJSONObject(index);ThoughtMethods method=ThoughtMethods.find(attempt.optString("method"));
        int page=Math.max(0,Math.min(3,attempt.optInt("step")));
        if(page>=2&&attempt.optString("response").isEmpty()&&!attempt.optString("note2").isEmpty())put(attempt,"response",attempt.optString("note2"));
        final AlertDialog[] host={null};Runnable refresh=()->{host[0].dismiss();attemptDialog(index,attempt);};
        LinearLayout detail=col();detail.setPadding(dp(20),dp(8),dp(20),dp(16));
        styledLabel(detail,"第 "+(page+1)+" / 4 步",TypeRole.META,GREEN);styledLabel(detail,attempt.optString("thought"),TypeRole.ITEM);
        if(page==0)styledLabel(detail,"右上角可保存并退出或换方法；已写内容会保留。",TypeRole.HINT);
        if(page<3) {
            if(page==2){
                guide(detail,"回看前两步",attempt.optString("note0")+"\n\n"+attempt.optString("note1"));
                styledLabel(detail,"把前面的发现汇成一段你认可的回应。这段内容会直接带到最后，无需重写。",TypeRole.HINT);
            }
            String questionHint=ThoughtMethods.hint(method.id,page);
            aiHeading(detail,method.questions[page],TypeRole.QUESTION,page==2?"形成回应":"当前问题",thought,attempt,page,refresh,questionHint);
            if(page==2)field(detail,"本次回应","综合前面的发现，写下更准确的理解。",attempt,"response",4,false);
            else field(detail,"我的思考","想到什么就先写下来；想不到可留空继续。",attempt,"note"+page,4,false);
        } else {
            styledLabel(detail,"把这次发现整理成自己认可的回应。真实困难可以保留，不必勉强说服自己。",TypeRole.HINT);
            StringBuilder notes=new StringBuilder();for(int i=0;i<3;i++)notes.append(method.questions[i]).append("\n").append(attempt.optString("note"+i)).append("\n\n");
            guide(detail,"回看本次思考",notes.toString());
            aiHelp(detail,"形成回应",thought,attempt,page,refresh);styledLabel(detail,attempt.optString("response").isEmpty()?"还没有回应，可以返回上一步填写或请求 AI 补充。":attempt.optString("response"),TypeRole.BODY);
            detail.addView(textAction("修改回应",GREEN,()->{put(attempt,"step",2);host[0].dismiss();attemptDialog(index,attempt);}));
            slider(detail,"对本次回应的相信程度",attempt,"responseBelief",50);
            slider(detail,"现在对原想法的相信程度",attempt,"after",attempt.optInt("before"));
            styledLabel(detail,"新旧想法分别评分，不必相加等于 100。分数不变、升高或对新回应仍不相信，都可以如实保存。",TypeRole.HINT);
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(detail);
        LinearLayout header=dialogHeader(method.name);header.setPadding(dp(20),dp(12),dp(8),dp(8));
        ImageButton more=iconButton(R.drawable.ic_more,"练习操作",()->{});header.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));
        AlertDialog.Builder dialogBuilder=new AlertDialog.Builder(this).setCustomTitle(header).setView(dialogFrame(scroll))
            .setPositiveButton(page==3?"完成本次探索":"下一步",null);
        if(page>0)dialogBuilder.setNegativeButton("上一步",null);
        AlertDialog dialog=dialogBuilder.create();host[0]=dialog;
        more.setOnClickListener(v->showAttemptMenu(more,dialog,index,attempt));
        dialog.setOnShowListener(d->{
            if(page>0){
                Button previous=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                Button next=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                LinearLayout controls=(LinearLayout)previous.getParent();
                for(int i=0;i<controls.getChildCount();i++){
                    View child=controls.getChildAt(i);
                    if(child!=previous&&child!=next)child.setVisibility(View.GONE);
                }
                previous.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));
                next.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));
                controls.setGravity(Gravity.FILL_HORIZONTAL);
                previous.setOnClickListener(v->{put(attempt,"step",page-1);dialog.dismiss();attemptDialog(index,attempt);});
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(page<3){put(attempt,"step",page+1);dialog.dismiss();attemptDialog(index,attempt);return;}
            if(attempt.optString("response").trim().isEmpty()||!attempt.has("responseBelief")||!attempt.has("after")){toast("请记录回应与两项评分；也可以保存退出，或没帮助时换个方法");return;}
            put(attempt,"done",true);put(attempt,"finished",System.currentTimeMillis());put(attempt,"note2",attempt.optString("response"));
            for(String key:new String[]{"response","responseBelief","after"})put(thought,key,attempt.opt(key));
            put(thought,"evidence",attempt.optString("note0")+"\n"+attempt.optString("note1")+"\n"+attempt.optString("note2"));
            put(thought,"handled",true);put(thought,"selected",true);changed();dialog.dismiss();
            });
        });
        dialog.setOnDismissListener(d->{changed();editor();});
        showDialog(dialog);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void showAttemptMenu(ImageButton anchor,AlertDialog dialog,int index,JSONObject attempt) {
        LinearLayout items=col();items.setPadding(dp(4),dp(4),dp(4),dp(4));
        GradientDrawable surface=bg(Color.WHITE,12);surface.setStroke(dp(1),Color.rgb(223,233,228));
        items.setBackground(surface);items.setClipToOutline(true);
        PopupWindow popup=new PopupWindow(items,dp(176),ViewGroup.LayoutParams.WRAP_CONTENT,true);
        popup.setBackgroundDrawable(surface);popup.setElevation(dp(8));popup.setOutsideTouchable(true);
        TextView save=text("保存并退出",16,INK);save.setGravity(Gravity.CENTER_VERTICAL);save.setPadding(dp(14),0,dp(14),0);save.setMinHeight(dp(48));save.setBackground(touchBackground(8));
        save.setOnClickListener(v->{popup.dismiss();dialog.dismiss();});items.addView(save);
        TextView change=text("换个方法",16,INK);change.setGravity(Gravity.CENTER_VERTICAL);change.setPadding(dp(14),0,dp(14),0);change.setMinHeight(dp(48));change.setBackground(touchBackground(8));
        change.setOnClickListener(v->{popup.dismiss();put(attempt,"noHelp",true);put(attempt,"finished",System.currentTimeMillis());changed();dialog.dismiss();methodDialog(index);});items.addView(change);
        popup.showAsDropDown(anchor,0,0,Gravity.END);
    }
    private String attemptReport(JSONObject attempt) {
        StringBuilder text=new StringBuilder(attempt.optString("name")+"  "+methodStatus(attempt)+"\n原想法："+attempt.optString("thought")+"\n");
        ThoughtMethods method=ThoughtMethods.find(attempt.optString("method"));
        if(method!=null)for(int i=0;i<3;i++)text.append("\n").append(method.questions[i]).append("\n").append(attempt.optString("note"+i)).append("\n");
        else text.append("\n事实：").append(attempt.optString("evidence")).append("\n");
        return text.append("\n回应：").append(attempt.optString("response")).append("\n回应相信程度：").append(percent(attempt,"responseBelief")).append("\n原想法相信程度：").append(percent(attempt,"before")).append(" → ").append(percent(attempt,"after")).toString();
    }
    private void attemptHistory(int index,JSONObject attempt) {
        LinearLayout detail=col();detail.setPadding(dp(20),dp(8),dp(20),dp(16));renderAttempt(detail,attempt);
        ScrollView scroll=new ScrollView(this);scroll.addView(detail);
        showDialog(new AlertDialog.Builder(this).setCustomTitle(dialogHeader("方法记录")).setView(dialogFrame(scroll)).setPositiveButton("返回方法列表",(d,w)->methodDialog(index)).create());
    }
    private TextView textAction(String name,int color,Runnable action) {
        TextView view=text(name,14,color);view.setGravity(Gravity.CENTER);view.setMinHeight(dp(48));view.setBackground(touchBackground(8));view.setFocusable(true);view.setOnClickListener(v->action.run());return view;
    }
    private boolean validate() {
        String error=null;
        if(step==0 && active.optString("event").trim().isEmpty()) error="请先记录一个具体的情境";
        if(step==2) {
            if(thoughts().length()==0) error="请添加至少一个自动想法";
            for(int i=0;i<thoughts().length();i++){JSONObject t=thoughts().optJSONObject(i);if(t.optString("text").trim().isEmpty()||!t.has("before"))error="请填写每条想法，并评估相信程度";}
        }
        if(step==4)for(String name:EMOTIONS)if(hasEmotion(name)&&!emotions().optJSONObject(name).has("target"))error="请评估各项情绪的目标强度";
        if(step==5) for(int i=0;i<thoughts().length();i++){JSONObject t=thoughts().optJSONObject(i);if(isSelected(t)&&(t.optString("response").trim().isEmpty()||!t.has("responseBelief"))){error="请点开想法 "+(i+1)+"，补充回应和相信程度，或保存为稍后继续";break;}}
        if(step==6)for(String name:EMOTIONS)if(hasEmotion(name)&&!emotions().optJSONObject(name).has("after"))error="请评估每项情绪现在的强度";
        if(error!=null){toast(error);return false;}return true;
    }
    private boolean isSelected(JSONObject thought) { return thought.optBoolean("selected",!thought.optString("response").trim().isEmpty()); }
    private String percent(JSONObject obj,String key) { return obj.has(key)?obj.optInt(key)+"%":"未评估"; }
    private void deleteEntry(JSONObject item) {
        showDeleteDialog(new AlertDialog.Builder(this).setCustomTitle(dialogHeader("删除这篇日志？")).setMessage("删除后无法恢复。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{
            for(int i=0;i<entries.length();i++)if(entries.optJSONObject(i)==item){entries.remove(i);break;}
            persist();home();
        }).create());
    }
    private String distortionNames(JSONObject t) {
        JSONObject d=t.optJSONObject("distortions"); ArrayList<String> names=new ArrayList<>();
        if(d!=null)for(String n:DISTORTIONS)if(d.optBoolean(n))names.add(n);
        return names.isEmpty()?"暂未选择":TextUtils.join("、",names);
    }
    private void distortions(LinearLayout parent,JSONObject thought) {
        JSONObject selected=thought.optJSONObject("distortions");
        ChipFlowLayout choices=new ChipFlowLayout(this);parent.addView(choices,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout reasons=col();parent.addView(reasons);
        for(String name:DISTORTIONS){
            ToggleButton chip=new ToggleButton(this);chip.setTextOn(name);chip.setTextOff(name);chip.setText(name);chip.setTextSize(14);chip.setAllCaps(false);chip.setLetterSpacing(0);chip.setGravity(Gravity.CENTER);
            chip.setButtonDrawable((android.graphics.drawable.Drawable)null);chip.setIncludeFontPadding(false);chip.setPadding(dp(12),dp(4),dp(12),dp(4));chip.setMinWidth(0);chip.setMinimumWidth(0);chip.setMinHeight(dp(32));chip.setMinimumHeight(dp(32));chip.setStateListAnimator(null);chip.setElevation(0);
            android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
            states.addState(new int[]{android.R.attr.state_checked},bg(GREEN,16));
            GradientDrawable normal=bg(Color.rgb(244,247,245),16);normal.setStroke(dp(1),Color.rgb(213,225,219));states.addState(new int[]{},normal);
            chip.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33176B5B),states,null));
            chip.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Color.WHITE,INK}));
            chip.setContentDescription(name);chip.setChecked(selected.optBoolean(name));
            chip.setOnCheckedChangeListener((button,checked)->{put(selected,name,checked);thought.remove("recommendations");changed();distortionReasons(reasons,thought);});
            ViewGroup.MarginLayoutParams layout=new ViewGroup.MarginLayoutParams(-2,-2);layout.setMargins(0,0,dp(8),dp(8));choices.addView(chip,layout);
        }
        distortionReasons(reasons,thought);
    }
    private void distortionReasons(LinearLayout container,JSONObject thought) {
        container.removeAllViews();JSONObject reasons=thought.optJSONObject("distortionReasons");if(reasons==null)return;
        boolean heading=false;JSONObject selected=thought.optJSONObject("distortions");
        for(String name:DISTORTIONS)if(!reasons.optString(name).isEmpty()){
            if(!heading){gap(container,componentSpace(ComponentRelation.NEXT_ITEM));styledLabel(container,"AI 识别与理由",TypeRole.SECTION,BLUE);heading=true;}
            TextView title=styledText(name+(selected.optBoolean(name)?"":"（未选中）"),TypeRole.ITEM);setRoleSpacing(title,TypeRole.ITEM);container.addView(title);
            styledLabel(container,reasons.optString(name),TypeRole.HINT);
        }
    }
    private void distortionHelp() {
        LinearLayout content=col();content.setPadding(dp(20),dp(8),dp(20),dp(16));
        for(int i=0;i<DISTORTIONS.length;i++){
            TextView title=styledText(DISTORTIONS[i],TypeRole.ITEM);setRoleSpacing(title,TypeRole.ITEM);content.addView(title);
            styledLabel(content,EXPLANATIONS[i],TypeRole.HINT);
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        showDialog(new AlertDialog.Builder(this).setCustomTitle(dialogHeader("认知歪曲说明")).setView(dialogFrame(scroll)).setPositiveButton("知道了",null).create());
    }
    private String report() {
        StringBuilder b=new StringBuilder("心晴  情绪日志\n");b.append(date(active.optLong("created"))).append("\n\n情境\n").append(active.optString("event")).append("\n").append(active.optString("context")).append("\n\n情绪（最初 / 目标 / 现在）\n");
        for(String n:EMOTIONS)if(hasEmotion(n)){JSONObject e=emotions().optJSONObject(n);b.append(n).append("  ").append(percent(e,"before")).append(" / ").append(percent(e,"target")).append(" / ").append(percent(e,"after")).append("\n");}
        b.append("\n正向重构\n可以理解的原因：").append(active.optString("understanding")).append("\n益处：").append(active.optString("benefits")).append("\n品质：").append(active.optString("values")).append("\n");
        for(int i=0;i<thoughts().length();i++){JSONObject t=thoughts().optJSONObject(i);b.append("\n想法：").append(t.optString("text")).append("\n").append(thoughtStatus(t)).append("\n原想法相信程度：").append(percent(t,"before")).append(" → ").append(percent(t,"after")).append("\n认知歪曲：").append(distortionNames(t)).append("\n事实：").append(t.optString("evidence")).append("\n回应：").append(t.optString("response")).append("\n回应相信程度：").append(percent(t,"responseBelief")).append("\n");}
        for(int i=0;i<thoughts().length();i++){
            JSONArray records=thoughts().optJSONObject(i).optJSONArray("attempts");
            if(records!=null&&records.length()>0){b.append("\n想法 ").append(i+1).append(" 的方法记录\n");for(int j=0;j<records.length();j++)b.append("\n记录 ").append(j+1).append("\n").append(attemptReport(records.optJSONObject(j))).append("\n");}
        }
        JSONArray advice=active.optJSONArray("aiNotes");if(advice!=null){b.append("\nAI 辅助记录\n");for(int i=0;i<advice.length();i++){JSONObject note=advice.optJSONObject(i);b.append(note.optString("task")).append("\n").append(note.optString("answer")).append("\n\n");}}
        return b.append("\n下一小步\n").append(active.optString("action")).toString();
    }
    private void summary() {
        screen="summary";shell("心晴  /  回顾");title("留给自己的一份理解",date(active.optLong("created")));
        styledLabel(body,"情绪的变化",TypeRole.SECTION);styledLabel(body,"最初  →  现在  /  目标",TypeRole.META);
        for(String n:EMOTIONS)if(hasEmotion(n)){JSONObject e=emotions().optJSONObject(n);scoreRow(body,n,percent(e,"before")+" → "+percent(e,"after")+" / "+percent(e,"target"));}
        sectionLabel(body,"情境");summaryValue(body,active.optString("event"));
        if(!active.optString("context").trim().isEmpty())summaryValue(body,active.optString("context"));
        sectionLabel(body,"正向重构");
        summaryField(body,"可能的益处",active.optString("benefits"));
        summaryField(body,"可能体现的可贵之处",active.optString("values"));
        if(!active.optString("understanding").trim().isEmpty())summaryField(body,"可以理解的原因",active.optString("understanding"));
        if(thoughts().length()>0)sectionLabel(body,"想法");
        for(int i=0;i<thoughts().length();i++){
            JSONObject thought=thoughts().optJSONObject(i);
            styledLabel(body,"想法 "+(i+1),TypeRole.ITEM);
            summaryValue(body,thought.optString("text"));
            body.addView(statusPill(thoughtStatus(thought),16),new LinearLayout.LayoutParams(-2,-2));
            scoreRow(body,"原想法相信程度",percent(thought,"before")+" → "+percent(thought,"after"));
            summaryField(body,"认知歪曲",distortionNames(thought));
            if(!thought.optString("evidence").trim().isEmpty())summaryField(body,"事实",thought.optString("evidence"));
            if(!thought.optString("response").trim().isEmpty())summaryField(body,"回应",thought.optString("response"));
            if(thought.has("responseBelief"))scoreRow(body,"回应相信程度",percent(thought,"responseBelief"));
            JSONArray records=thought.optJSONArray("attempts");
            if(records!=null&&records.length()>0){
                sectionLabel(body,"方法记录");
                for(int j=0;j<records.length();j++)renderAttempt(body,records.optJSONObject(j));
            }
        }
        JSONArray advice=active.optJSONArray("aiNotes");
        if(advice!=null&&advice.length()>0){
            sectionLabel(body,"AI 辅助记录");
            for(int i=0;i<advice.length();i++){JSONObject note=advice.optJSONObject(i);summaryField(body,note.optString("task"),note.optString("answer"));}
        }
        if(!active.optString("action").trim().isEmpty()){sectionLabel(body,"下一小步");summaryValue(body,active.optString("action"));}
        gap(body,componentSpace(ComponentRelation.NEXT_SECTION));
        body.addView(button("导出文本",true,()->{exportText=report();Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TITLE,"心晴日志-"+new SimpleDateFormat("yyyyMMdd-HHmm",Locale.ROOT).format(new Date(active.optLong("created")))+".txt");startActivityForResult(intent,42);}));
        body.addView(button("编辑这篇日志",false,()->{put(active,"done",false);step=0;editor();}));
        Button delete=button("删除日志",false,()->deleteEntry(active));delete.setTextColor(DANGER);delete.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22B3261E),bg(Color.rgb(252,238,238),8),null));body.addView(delete);
        Button done=button("完成，返回日志列表",true,()->{persist();home();});LinearLayout.LayoutParams doneLayout=(LinearLayout.LayoutParams)done.getLayoutParams();doneLayout.leftMargin=dp(22);doneLayout.rightMargin=dp(22);root.addView(done,doneLayout);
    }
    private void summaryField(LinearLayout parent,String heading,String value) {
        TextView title=styledText(heading,TypeRole.SCORE);
        setRoleSpacing(title,TypeRole.SCORE);parent.addView(title);
        summaryValue(parent,value);
    }
    private void summaryValue(LinearLayout parent,String value) {
        TextView text=styledText(value,TypeRole.BODY);text.setTextIsSelectable(true);
        setRoleSpacing(text,TypeRole.BODY);parent.addView(text);
    }
    private void renderAttempt(LinearLayout parent,JSONObject attempt) {
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=styledText(attempt.optString("name","方法"),TypeRole.ITEM);setRoleSpacing(name,TypeRole.ITEM);
        titleRow.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        titleRow.addView(statusPill(methodStatus(attempt),16));parent.addView(titleRow);
        if(!attempt.optString("thought").trim().isEmpty())summaryField(parent,"原想法",attempt.optString("thought"));
        ThoughtMethods method=ThoughtMethods.find(attempt.optString("method"));
        if(method!=null){
            for(int i=0;i<3;i++)if(!attempt.optString("note"+i).trim().isEmpty())summaryField(parent,method.questions[i],attempt.optString("note"+i));
        }else if(!attempt.optString("evidence").trim().isEmpty())summaryField(parent,"事实",attempt.optString("evidence"));
        if(!attempt.optString("response").trim().isEmpty())summaryField(parent,"回应",attempt.optString("response"));
        if(attempt.has("responseBelief"))scoreRow(parent,"回应相信程度",percent(attempt,"responseBelief"));
        if(attempt.has("after"))scoreRow(parent,"原想法相信程度",percent(attempt,"before")+" → "+percent(attempt,"after"));
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==42&&result==RESULT_OK&&data!=null&&data.getData()!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){String content=exportText!=null?exportText:report();out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));toast("日志已导出");}catch(Exception e){toast("导出失败，请重试");}}
    }
    private void about() {
        screen="about";shell("心晴  /  关于");title("理解，而非苛责","一个私人的情绪记录空间。");
        styledLabel(body,"方法来源",TypeRole.SECTION);styledLabel(body,"参考 David D. Burns《Feeling Great》及作者公开介绍的 Daily Mood Log：记录情境、情绪和想法，探索情绪的积极意义，设置目标，检视思维并重新评估。提示语为独立编写，非官方产品。",TypeRole.BODY);
        body.addView(button("查看作者的公开资料",false,()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://feelinggood.com/daily-mood-journal/")))));
        styledLabel(body,"你的数据",TypeRole.SECTION);styledLabel(body,"日志保存在此设备。只有主动点击 AI 图标时，当前情境及相关内容才会发送给 DeepSeek；不上传其他日志，不接入统计服务。AI 内容校验后追加到对应字段，保留原文，不填写评分。密钥通过 Android Keystore 加密保存。卸载或清除数据会删除本地记录，导出的文本未加密。",TypeRole.BODY);
        body.addView(button("AI 连接",false,()->configureAi()));
        styledLabel(body,"使用边界",TypeRole.SECTION);styledLabel(body,"这是自助记录工具，不能替代专业诊断或治疗。如果情绪持续影响生活，可以寻求心理健康专业人员的支持。若有即时伤害自己的风险，请立即联系当地急救服务或可信赖的人。",TypeRole.BODY);
    }
}
