package com.jamalsafwat.wear2test.adapters;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jamalsafwat.wear2test.pojo.IconData;
import com.jamalsafwat.wear2test.R;

import java.util.Random;

/**
 * Created by jamal.safwat on 7/27/2017.
 */

public class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {



    private IconData[] data;

    public IconAdapter(IconData[] data) {
        this.data = data;
    }


    @Override
    public IconAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.wearable_recycler_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(IconAdapter.ViewHolder holder, int position) {

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        Random r = new Random();
        int i1 = r.nextInt(255 - 1) + 1;
        int i2 = r.nextInt(255 - 1) + 1;
        int i3 = r.nextInt(255 - 1) + 1;


        holder.textView.setText(data[position].getDescription());
        holder.imageView.setImageResource(data[position].getImgId());
        holder.imageView.setColorFilter(Color.argb(255, i1, i2, i3)); // White Tint
    }

    @Override
    public int getItemCount() {
        return data.length;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.imageView);

            this.textView = (TextView) itemView.findViewById(R.id.textView);
        }
    }

}
