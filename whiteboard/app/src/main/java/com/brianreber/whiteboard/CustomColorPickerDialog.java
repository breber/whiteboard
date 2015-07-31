package com.brianreber.whiteboard;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

/**
 * Created by breber on 7/30/15.
 */
public class CustomColorPickerDialog extends ColorPickerDialog implements SeekBar.OnSeekBarChangeListener {

    private OnPenSizeChangedListener mSizeListener;

    public interface OnPenSizeChangedListener {
        void onSizeChanged(int size);
    }

    public CustomColorPickerDialog(Context context, int initialColor, int size) {
        super(context, initialColor);

        LinearLayout rightSide = (LinearLayout) mLayout.findViewById(R.id.right_side);

        TextView label = new TextView(context);
        label.setText(R.string.choose_size);
        rightSide.addView(label);

        SeekBar mPenSize = new SeekBar(context);
        mPenSize.setProgress(size);
        mPenSize.setOnSeekBarChangeListener(this);
        rightSide.addView(mPenSize);
    }

    /**
     * Set a OnPenSizeChangedListener to get notified when the color
     * selected by the user has changed.
     * @param listener
     */
    public void setOnPenSizeChangedListener(OnPenSizeChangedListener listener) {
        mSizeListener = listener;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (mSizeListener != null) {
            mSizeListener.onSizeChanged(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
