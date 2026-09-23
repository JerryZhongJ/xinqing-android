package cn.xinqing.journal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** Wraps variable-width choices without changing their size when selected. */
final class ChipFlowLayout extends ViewGroup {
    ChipFlowLayout(Context context){super(context);}
    @Override protected LayoutParams generateDefaultLayoutParams(){return new MarginLayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);}
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs){return new MarginLayoutParams(getContext(),attrs);}
    @Override protected LayoutParams generateLayoutParams(LayoutParams source){return new MarginLayoutParams(source);}
    @Override protected boolean checkLayoutParams(LayoutParams params){return params instanceof MarginLayoutParams;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int limit=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?Integer.MAX_VALUE/4:MeasureSpec.getSize(widthSpec);
        int x=getPaddingLeft(),y=getPaddingTop(),rowHeight=0,used=getPaddingLeft();
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);if(child.getVisibility()==GONE)continue;
            measureChildWithMargins(child,widthSpec,0,heightSpec,0);MarginLayoutParams p=(MarginLayoutParams)child.getLayoutParams();
            int width=child.getMeasuredWidth()+p.leftMargin+p.rightMargin,height=child.getMeasuredHeight()+p.topMargin+p.bottomMargin;
            if(x>getPaddingLeft()&&x+width>limit-getPaddingRight()){y+=rowHeight;x=getPaddingLeft();rowHeight=0;}
            x+=width;used=Math.max(used,x);rowHeight=Math.max(rowHeight,height);
        }
        setMeasuredDimension(resolveSize(used+getPaddingRight(),widthSpec),resolveSize(y+rowHeight+getPaddingBottom(),heightSpec));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        int x=getPaddingLeft(),y=getPaddingTop(),rowHeight=0;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);if(child.getVisibility()==GONE)continue;
            MarginLayoutParams p=(MarginLayoutParams)child.getLayoutParams();int width=child.getMeasuredWidth()+p.leftMargin+p.rightMargin,height=child.getMeasuredHeight()+p.topMargin+p.bottomMargin;
            if(x>getPaddingLeft()&&x+width>right-left-getPaddingRight()){y+=rowHeight;x=getPaddingLeft();rowHeight=0;}
            child.layout(x+p.leftMargin,y+p.topMargin,x+p.leftMargin+child.getMeasuredWidth(),y+p.topMargin+child.getMeasuredHeight());x+=width;rowHeight=Math.max(rowHeight,height);
        }
    }
}
