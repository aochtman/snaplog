package com.example.postoffline.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postoffline.DatabaseHelper;
import com.example.postoffline.EditItemActivity;
import com.example.postoffline.Item;
import com.example.postoffline.R;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private Activity context;
    private List<Item> itemList;
    private DatabaseHelper databaseHelper;
    String lastWeightOfPreviousRecentDate="";

    public MyAdapter(Activity context, List<Item> itemList, DatabaseHelper databaseHelper) {
        this.context = context;
        this.itemList = itemList;
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itemList.get(position);

// Find the first non-empty weight record
        List<String> allDates= databaseHelper.getAllDates();

        Comparator<String> dateStringComparator = new Comparator<String>() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd MMM yyyy");

            @Override
            public int compare(String date1, String date2) {
                try {
                    Date parsedDate1 = dateFormat.parse(date1);
                    Date parsedDate2 = dateFormat.parse(date2);
                    return parsedDate1.compareTo(parsedDate2);
                } catch (ParseException e) {
                    e.printStackTrace();
                    // If parsing fails, return 0 (indicating equal)
                    return 0;
                }
            }
        };

// Sort the list of dates using the custom comparator
        Collections.sort(allDates, dateStringComparator);
        Collections.reverse(allDates);

// Find the index of the most recent date
        int startIndex = -1;
        for (int i = 0; i < allDates.size(); i++) {
            String date = allDates.get(i);
            if (date.equals(item.getDate())) { // Your most recent date

                System.out.println("date which match=="+date+"  index="+i);
                startIndex = i;
                break;
            }
        }


        String secondMostRecentDateString="";
        if (startIndex != -1) {
            for (int i = startIndex + 1; i < allDates.size(); i++) {
                secondMostRecentDateString = allDates.get(i);
                break;

            }
        }

        System.out.println("Second Most recent date: " + secondMostRecentDateString);

        // Now you can use the secondMostRecentDateString to get the last weight of the second most recent date
        lastWeightOfPreviousRecentDate = databaseHelper.getLastWeightOfProvidedDate(secondMostRecentDateString);

        // Now you can use the secondMostRecentDateString to get the last weight of the second most recent date
       String firstNonEmptyWeight = databaseHelper.getFirstNonEmptyWeight(item.getDate());
        String secondNonEmptyWeight = databaseHelper.getSecondNonEmptyWeight(item.getDate());






        SharedPreferences sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String getDateOfFirstRecord = sharedPref.getString("savedDate", "");
        System.out.println("jdkfjkdj=="+getDateOfFirstRecord+"   lastWeightOfPreviousRecentDate="+lastWeightOfPreviousRecentDate);

        if (!item.getImage().isEmpty()) {
            // If image exists, set ImageView and hide weight TextView
            Uri imageUri = Uri.parse(item.getImage());

            holder.imageView.setImageURI(imageUri);
            holder.lv_image.setVisibility(View.VISIBLE);
            holder.lv_weight.setVisibility(View.GONE);

        } else {
            // If image does not exist, show weight TextView and hide ImageView

            if (!item.getWeight().isEmpty()) {
                holder.lv_image.setVisibility(View.GONE);
                holder.lv_weight.setVisibility(View.VISIBLE);

                holder.tvWeight.setText(item.getWeight());

                if (getDateOfFirstRecord.equals(item.getDate()))
                {
                    if (position == 0 && firstNonEmptyWeight != null && secondNonEmptyWeight != null) {
                        if (firstNonEmptyWeight.equals(secondNonEmptyWeight)) {
                            holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
                            //     holder.image_arrow.setImageResource(R.drawable.baseline_arrow_upward_24);
                            holder.tv_weight_dif.setText("--");
                            return; // Exit the method if it's the first record and equal to the second
                        }
                    }

                    if (position == 0 && item.getWeight().equals(firstNonEmptyWeight)) {
                        // If it's the first weight and equal to the first non-empty weight, set orange background
                        holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
                        holder.tv_weight_dif.setText("--");
                        //    holder.image_arrow.setImageResource(R.drawable.baseline_arrow_upward_24);

                        return;
                    }

                    if (position > 0 && isLowerWeight(position)) {
                        // If the weight is lower than the previous weight, set green background
                        holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
                        holder.tv_weight_dif.setText(weightDifference(position));
                        holder.image_arrow.setImageResource(R.drawable.baseline_arrow_downward_24);
                    } else {
                        // Default background color
                        if (weightDifference(position).equals("0.0") || weightDifference(position).equals("--") )
                        {
                            holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));

                            holder.tv_weight_dif.setText(weightDifference(position));
                        }
                        else
                        {
                            holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));

                            holder.tv_weight_dif.setText(weightDifference(position));
                            holder.image_arrow.setImageResource(R.drawable.baseline_arrow_upward_24);
                        }


                    }
                }

                else
                {


                    if (!lastWeightOfPreviousRecentDate.equals("") && databaseHelper.isFirstWeightEntryForDate(item.getDate(),item.getWeight())) {
                        // If it's the first weight and equal to the first non-empty weight, set orange background
                        if (weightDifference2(position,lastWeightOfPreviousRecentDate).equals("0.0") )
                        {
                            System.out.println("due to image2 ==="+item.getWeight());
                            holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));

                            holder.tv_weight_dif.setText(weightDifference2(position, lastWeightOfPreviousRecentDate));
                        }
                        else
                        {
                            System.out.println("this else working");
                            if (isLowerWeight2(position,lastWeightOfPreviousRecentDate)) {
                                // If the weight is lower than the previous weight, set green background
                                holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
                                holder.tv_weight_dif.setText(weightDifference2(position, lastWeightOfPreviousRecentDate));
                                holder.image_arrow.setImageResource(R.drawable.baseline_arrow_downward_24);
                            }
                            else
                            {
                                holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));

                                holder.tv_weight_dif.setText(weightDifference2(position, lastWeightOfPreviousRecentDate));
                                holder.image_arrow.setImageResource(R.drawable.baseline_arrow_upward_24);
                            }


                        }
                        return;

                    }

                    if (position > 0 && isLowerWeight(position)) {
                        // If the weight is lower than the previous weight, set green background
                        holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
                        holder.tv_weight_dif.setText(weightDifference(position));
                        holder.image_arrow.setImageResource(R.drawable.baseline_arrow_downward_24);
                    } else {
                        // Default background color
                        if (weightDifference(position).equals("0.0") || weightDifference(position).equals("--") )
                        {
                            holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));

                            System.out.println("due to image ==="+item.getWeight());
                            holder.tv_weight_dif.setText(weightDifference(position));
                        }
                        else
                        {
                            holder.lv_weight.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));

                            holder.tv_weight_dif.setText(weightDifference(position));
                            holder.image_arrow.setImageResource(R.drawable.baseline_arrow_upward_24);
                        }


                    }
                }
            }
            else
            {
                holder.lv_image.setVisibility(View.GONE);
                holder.lv_weight.setVisibility(View.GONE);


            }

        }


      /*  holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, EditItemActivity.class);
                intent.putExtra("itemId", item.getId());
                intent.putExtra("weight", item.getWeight());

                if (!item.getImage().isEmpty())
                {
                    intent.putExtra("image", "contain");
                }
                intent.putExtra("date", item.getDate());

                context.startActivity(intent);
            }
        });
*/

    }

    private boolean compareWeights(String firstWeight, String lastWeight) {
        if (firstWeight == null || lastWeight == null || firstWeight.isEmpty() || lastWeight.isEmpty()) {
            return false;  // Handle null or empty weights
        }

        try {
            // Convert strings to double and compare
            double currentWeight = Double.parseDouble(firstWeight);
            double previousWeight = Double.parseDouble(lastWeight);
            return currentWeight > previousWeight;
        } catch (NumberFormatException e) {
            // Handle potential number format exception
            Log.e("WeightCheck", "Error parsing weight values: " + e.getMessage());
            return false;
        }
    }

    // Helper method to check if the current weight is lower than the previous weight
    private boolean isLowerWeight(int position) {
        if (position > 0) {
            int index = position - 1;  // Start with the previous item
            while (index >= 0) {
                Item item = itemList.get(index);
                if (!item.getWeight().isEmpty()) {
                    // Found a non-empty weight, use it for comparison
                    String firstNonEmptyWeight = item.getWeight();
                    String lastNonEmptyWeight = itemList.get(position).getWeight();
                    return compareWeights(firstNonEmptyWeight, lastNonEmptyWeight);
                }
                index--;  // Move to the previous item
            }
        }
        return false;  // Default to false if no non-empty weight is found
    }
    private boolean isLowerWeight2(int position, String lastWeightOfPreviousRecentDate) {
        String lastNonEmptyWeight = itemList.get(position).getWeight();

        // Check for null values first
        if (lastWeightOfPreviousRecentDate == null || lastNonEmptyWeight == null) {
            Log.d("WeightCheck2", "One of the weights is null");
            return false;
        }

        // Check for empty strings
        if (lastWeightOfPreviousRecentDate.isEmpty() || lastNonEmptyWeight.isEmpty()) {
            Log.d("WeightCheck2", "One of the weights is empty");
            return false;
        }

        try {
            // Convert strings to double and compare
            double currentWeight = Double.parseDouble(lastWeightOfPreviousRecentDate);
            double previousWeight = Double.parseDouble(lastNonEmptyWeight);

            Log.d("WeightCheck2", "Current weight: " + currentWeight + ", Previous weight: " + previousWeight);
            return currentWeight > previousWeight;
        } catch (NumberFormatException e) {
            // Handle potential number format exception
            Log.e("WeightCheck", "Error parsing weight values: " + e.getMessage());
            return false;
        }
    }

    private String weightDifference(int position) {
        if (position > 0) {

            int index = position - 1;  // Start with the previous item
            while (index >= 0) {
                Item item = itemList.get(index);


                if (!item.getWeight().isEmpty()) {
                    // Found a non-empty weight, use it for comparison
                    System.out.println("weight of the older=="+item.getWeight());
                    String firstNonEmptyWeight = item.getWeight();
                    String lastNonEmptyWeight = itemList.get(position).getWeight();

                    return compareWeights_text(firstNonEmptyWeight, lastNonEmptyWeight);

                }
                index--;  // Move to the previous item
            }



        }
        return "--";
    }

    private String compareWeights_text(String firstNonEmptyWeight, String lastNonEmptyWeight) {




        System.out.println("sdjkdfjkdfhfdhfd=="+firstNonEmptyWeight+"  "+lastNonEmptyWeight);
        // Check for empty strings or null values
        if (firstNonEmptyWeight.isEmpty() || lastNonEmptyWeight.isEmpty()) {
            return "--"; // Return "--" if either weight string is empty
        }

        // Convert strings to double and calculate the absolute difference
        double currentWeight = Double.parseDouble(firstNonEmptyWeight);
        double previousWeight = Double.parseDouble(lastNonEmptyWeight);
        double difference = Math.abs(previousWeight - currentWeight);
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        return decimalFormat.format(difference);
    }

    private String weightDifference2(int position, String firstNonEmptyWeight) {

        String lastNonEmptyWeight = itemList.get(position).getWeight();

        System.out.println("sdjkdfjkdf=="+firstNonEmptyWeight+"  "+lastNonEmptyWeight);
        // Check for empty strings or null values
        if (firstNonEmptyWeight.isEmpty() || lastNonEmptyWeight.isEmpty()) {
            return "--"; // Return "--" if either weight string is empty
        }

        // Convert strings to double and calculate the absolute difference
        double currentWeight = Double.parseDouble(firstNonEmptyWeight);
        double previousWeight = Double.parseDouble(lastNonEmptyWeight);
        double difference = Math.abs(previousWeight - currentWeight);
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        return decimalFormat.format(difference);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView,image_arrow;
        LinearLayout lv_weight,lv_image;


        TextView tvWeight,tv_weight_dif;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            tvWeight = itemView.findViewById(R.id.tv_weight);
            lv_weight = itemView.findViewById(R.id.lv_weight);
            lv_image = itemView.findViewById(R.id.lv_image);
            image_arrow=itemView.findViewById(R.id.image_arrow);
            tv_weight_dif=itemView.findViewById(R.id.tv_weight_dif);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // Handle item click here, for example:
                        Item item = itemList.get(position);
                        // Open the EditItemActivity or perform any other action you need
                        Intent intent = new Intent(context, EditItemActivity.class);
                        intent.putExtra("itemId", item.getId());
                        intent.putExtra("weight", item.getWeight());
                        if (!item.getImage().isEmpty()) {
                            intent.putExtra("image", "contain");
                        }
                        intent.putExtra("date", item.getDate());
                        context.startActivity(intent);
                    }
                }
            });


        }
    }
}
