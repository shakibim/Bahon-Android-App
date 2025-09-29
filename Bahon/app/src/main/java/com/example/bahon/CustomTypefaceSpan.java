package com.example.bahon;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;

public class CustomTypefaceSpan extends TypefaceSpan {
    private final Typeface newTypeface;

    public CustomTypefaceSpan(Typeface typeface) {
        super("");
        newTypeface = typeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        applyCustomTypeFace(paint, newTypeface);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        applyCustomTypeFace(paint, newTypeface);
    }

    private void applyCustomTypeFace(Paint paint, Typeface tf) {
        paint.setTypeface(tf);
    }
}
