package com.example.snaplog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snaplog.Adapter.MyAdapter;

import java.util.List;
public class GroupedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Activity context;
    private List<DateGroup> dateGroups;
    private DatabaseHelper databaseHelper;

    public GroupedAdapter(Activity context, List<DateGroup> dateGroups) {
        this.context = context;
        this.dateGroups = dateGroups;
        System.out.println("size of the post=="+dateGroups.size());
        databaseHelper = new DatabaseHelper(context);
    }

    @Override
    public int getItemCount() {
        return dateGroups.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_date_header, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateViewHolder) {
            DateViewHolder dateHolder = (DateViewHolder) holder;
            DateGroup dateGroup = dateGroups.get(position);
            String date = dateGroup.getDate();
            dateHolder.dateTextView.setText(date);

            // Initialize and set adapter for item RecyclerView
            List<Item> itemList = dateGroup.getItems();
            MyAdapter itemAdapter = new MyAdapter(context, itemList,databaseHelper);
            dateHolder.itemRecyclerView.setLayoutManager(new GridLayoutManager(context, 5));

            dateHolder.itemRecyclerView.setAdapter(itemAdapter);
        }
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        RecyclerView itemRecyclerView;

        DateViewHolder(View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.date_text_view);
            itemRecyclerView = itemView.findViewById(R.id.item_recycler_view);
        }
    }
}
