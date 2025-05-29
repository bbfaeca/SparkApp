package com.example.sparkapp;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuoteAdapter extends RecyclerView.Adapter<QuoteAdapter.ViewHolder> {
    private List<QuoteIdea> quotes;
    private final OnItemClickListener listener;
    private boolean isManageMode = false;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    public QuoteAdapter(List<QuoteIdea> quotes, OnItemClickListener listener) {
        this.quotes = quotes;
        this.listener = listener;
    }

    public void setManageMode(boolean manageMode) {
        this.isManageMode = manageMode;
        notifyDataSetChanged();
    }

    public void updateQuotes(List<QuoteIdea> newQuotes) {
        this.quotes = newQuotes;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quote, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        QuoteIdea quote = quotes.get(position);
        holder.quoteText.setText(quote.getText());

        // 设置随机颜色背景
        GradientDrawable drawable = (GradientDrawable) holder.itemView.getBackground().mutate();
        drawable.setColor(quote.getColor());
        holder.itemView.setBackground(drawable);

        holder.deleteIcon.setVisibility(isManageMode ? View.VISIBLE : View.GONE);
        holder.deleteIcon.setOnClickListener(v -> listener.onDeleteClick(position));
    }

    @Override
    public int getItemCount() {
        return quotes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView quoteText;
        ImageView deleteIcon;

        ViewHolder(View itemView, OnItemClickListener listener) {
            super(itemView);
            quoteText = itemView.findViewById(R.id.quoteText);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}