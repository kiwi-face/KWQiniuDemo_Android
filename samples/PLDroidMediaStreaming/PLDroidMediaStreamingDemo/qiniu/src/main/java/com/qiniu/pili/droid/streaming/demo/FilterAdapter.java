package com.qiniu.pili.droid.streaming.demo;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

/**
 * https://github.com/CymChad/BaseRecyclerViewAdapterHelper
 */
public class FilterAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    List<String> filterNameList;

    private int selectedIndex;

    public FilterAdapter(List<String> filterNameList) {
        super( R.layout.item_text_view, filterNameList);
        this.filterNameList = filterNameList;
    }



    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.name, item).setVisible(R.id.selected,helper.getAdapterPosition() == this.selectedIndex);
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
}
