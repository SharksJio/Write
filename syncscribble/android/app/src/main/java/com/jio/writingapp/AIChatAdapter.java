package com.jio.writingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for AI chat messages
 */
public class AIChatAdapter extends RecyclerView.Adapter<AIChatAdapter.MessageViewHolder> {
    
    private List<AIChatActivity.AIChatMessage> messages;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    public AIChatAdapter(List<AIChatActivity.AIChatMessage> messages) {
        this.messages = messages;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        AIChatActivity.AIChatMessage message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView messageCard;
        private TextView messageText;
        private TextView timeText;
        private View userMessageLayout;
        private View assistantMessageLayout;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.message_card);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            userMessageLayout = itemView.findViewById(R.id.user_message_layout);
            assistantMessageLayout = itemView.findViewById(R.id.assistant_message_layout);
        }
        
        public void bind(AIChatActivity.AIChatMessage message) {
            messageText.setText(message.content);
            timeText.setText(timeFormat.format(new Date(message.timestamp)));
            
            // Configure layout based on message type
            if (message.isUser) {
                // User message - align right, blue background
                userMessageLayout.setVisibility(View.VISIBLE);
                assistantMessageLayout.setVisibility(View.GONE);
                messageCard.setCardBackgroundColor(
                    itemView.getContext().getColor(R.color.user_message_background));
            } else {
                // Assistant message - align left, gray background
                userMessageLayout.setVisibility(View.GONE);
                assistantMessageLayout.setVisibility(View.VISIBLE);
                
                if (message.isSystem) {
                    messageCard.setCardBackgroundColor(
                        itemView.getContext().getColor(R.color.system_message_background));
                } else {
                    messageCard.setCardBackgroundColor(
                        itemView.getContext().getColor(R.color.assistant_message_background));
                }
            }
        }
    }
}