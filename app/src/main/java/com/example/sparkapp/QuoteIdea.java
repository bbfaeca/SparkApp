package com.example.sparkapp;

import android.graphics.Color;

public class QuoteIdea {
    private final String text;
    private final int color;

    public QuoteIdea(String text, int color) {
        this.text = text;
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public int getColor() {
        return color;
    }
}