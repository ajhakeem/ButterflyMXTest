package com.example.jaseem.butterflymx;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Jaseem on 4/12/18.
 */

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private ArrayList<MessageModel> messagesList;

    public ChatAdapter(ArrayList<MessageModel> messages) {
        this.messagesList = messages;
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {

        public TextView tvMessageSender, tvMessageText;

        public ChatViewHolder(View itemView) {
            super(itemView);
            tvMessageSender = itemView.findViewById(R.id.tvMessageSender);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
        }
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_model, parent, false);
        return new ChatViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        holder.tvMessageSender.setText(messagesList.get(position).messageSender);
        holder.tvMessageText.setText(messagesList.get(position).messageBody);
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }
}
