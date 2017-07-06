package com.example.fahim.podcastsa;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by fahim on 03-Jul-17.
 */

public class PodcastAdapter extends RecyclerView.Adapter<PodcastAdapter.PCViewHolder>{

    private ArrayList<PodcastItem> pcList;
    private ItemClickListener listener;
    private static int playingPos;
    private Context context;

    public PodcastAdapter(ArrayList<PodcastItem> pcList, Context context, ItemClickListener listener) {
        this.pcList = pcList;
        this.context = context;
        this.listener = listener;
        playingPos = -1;
    }

    @Override
    public PCViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.podcast_card, parent, false);
        PCViewHolder wsv = new PCViewHolder(v);
        return wsv;
    }

    @Override
    public void onBindViewHolder(final PCViewHolder holder, final int position) {
        PodcastItem pc = pcList.get(position);
        holder.podcastTitle.setText(pc.getTitle());
        holder.podcastSecTitle.setText(pc.getSecondaryTitle());

        if(pc.getStatus()==1)holder.cv.setCardBackgroundColor(Color.parseColor("#e8e8e8"));
        else holder.cv.setCardBackgroundColor(Color.parseColor("#f8f8f8"));

        if(pc.playing == 1) holder.playing.setVisibility(View.VISIBLE);
        else holder.playing.setVisibility(View.GONE);

        holder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    listener.itemClick(position);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return pcList.size();
    }


    public static class PCViewHolder extends RecyclerView.ViewHolder{
        CardView cv;
        TextView podcastTitle;
        TextView podcastSecTitle;
        ImageView playing;

        public PCViewHolder(View itemView) {
            super(itemView);
            cv = (CardView) itemView.findViewById(R.id.podcastCardView);
            podcastTitle = (TextView) itemView.findViewById(R.id.podcastTitle);
            podcastSecTitle = (TextView) itemView.findViewById(R.id.podcastSecTitle);
            playing = (ImageView) itemView.findViewById(R.id.playingRV);
        }
    }
}
