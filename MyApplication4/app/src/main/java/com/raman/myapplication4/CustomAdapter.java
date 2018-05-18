package com.raman.myapplication4;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private List<String> courseCode, teacherName, daySpan;
    private Context context;

    CustomAdapter(Context context, List<String> courseCode, List<String> teacherName, List<String> daySpan) {
        this.context = context;
        this.courseCode = courseCode;
        this.teacherName = teacherName;
        this.daySpan = daySpan;
    }

    CustomAdapter(Context context, SharedPreferences sharedPref, Util util) {
        this.context = context;
        courseCode = new ArrayList<>();
        teacherName = new ArrayList<>();
        daySpan = new ArrayList<>();
        if (!sharedPref.getString("key_course_codes", "").equals("")) {
            courseCode = util.convertStringArrayToList(sharedPref.getString("key_course_codes", ""));
            teacherName = util.convertStringArrayToList(sharedPref.getString("key_teacher_names", ""));
            daySpan = util.convertStringArrayToList(sharedPref.getString("key_day_spans", ""));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cards_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.tv_card_cc.setText(courseCode.get(position));
        holder.tv_card_name.setText(teacherName.get(position));
        holder.tv_card_name.setBackgroundColor(PreferenceManager.getDefaultSharedPreferences(
                context).getInt(context.getString(R.string.key_color_primary), -15108398));
        holder.tv_card_span.setText(daySpan.get(position));
        holder.tv_card_option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //creating a popup menu_main
                PopupMenu popup = new PopupMenu(view.getContext(), holder.tv_card_option);
                //inflating menu_main from xml resource
                popup.inflate(R.menu.options);
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.card_share_menu:
                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, "*" + holder.tv_card_name.getText() + "* is on leave\n" + "\n*Checked by LPU UnderCover app*\n" + view.getContext().getString(R.string.play_store_link));
                                sendIntent.setType("text/plain");
                                view.getContext().startActivity(Intent.createChooser(sendIntent, view.getContext().getResources().getText(R.string.send_to)));
                                break;
                        }
                        return false;
                    }
                });
                //displaying the popup
                popup.show();

            }
        });
    }

    @Override
    public int getItemCount() {
        return courseCode.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_card_option;
        TextView tv_card_cc;
        TextView tv_card_name;
        TextView tv_card_span;

        ViewHolder(final View itemView) {
            super(itemView);
            tv_card_cc = itemView.findViewById(R.id.card_cc);
            tv_card_name = itemView.findViewById(R.id.card_name);
            tv_card_span = itemView.findViewById(R.id.card_span);
            tv_card_option = itemView.findViewById(R.id.card_option);
        }
    }
}
