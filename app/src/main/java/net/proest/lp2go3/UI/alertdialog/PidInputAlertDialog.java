/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package net.proest.lp2go3.UI.alertdialog;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import net.proest.lp2go3.H;
import net.proest.lp2go3.R;
import net.proest.lp2go3.UAVTalk.UAVTalkXMLObject;
import net.proest.lp2go3.UI.PidTextView;
import net.proest.lp2go3.UI.SingleToast;

import java.text.DecimalFormat;

public class PidInputAlertDialog extends InputAlertDialog implements SeekBar.OnSeekBarChangeListener {

    int mStep;
    int mDenom;
    int mValueMax;
    String mDecimalFormatString;
    EditText mEditText;
    PidTextView mPidTextView;


    public PidInputAlertDialog(Context parent) {
        super(parent);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int p = seekBar.getProgress();

            if (p % mStep != 0) {
                p = p - p % mStep;
            }
            mEditText.setText(getDecimalString(p / (float) mDenom));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public PidInputAlertDialog withPidTextView(PidTextView pidTextView) {
        this.mPidTextView = pidTextView;
        return this;
    }

    public PidInputAlertDialog withValueMax(int max) {
        this.mValueMax = max;
        return this;
    }

    public PidInputAlertDialog withStep(int step) {
        this.mStep = step;
        return this;
    }

    public PidInputAlertDialog withDecimalFormat(String df) {
        this.mDecimalFormatString = df;
        return this;
    }

    public PidInputAlertDialog withDenominator(int denom) {
        this.mDenom = denom;
        return this;
    }

    public void show() {
        if (mUavTalkDevice == null) {
            SingleToast.makeText(getContext(), getContext().getString(R.string.NOT_CONNECTED),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Builder dialogBuilder = new Builder(getContext());
        dialogBuilder.setTitle(mTitle);

        final View alertView = View.inflate(getContext(), mLayout, null);
        dialogBuilder.setView(alertView);

        mEditText = (EditText) alertView.findViewById(R.id.etxInput);
        mEditText.setText(mText);
        mEditText.setSelection(mText.length());
        mEditText.requestFocus();
        //input.setFilters(new InputFilter[]{new InputFilterMinMax(getContext(), 0, (float)mValueMax/mDenom)});


        final SeekBar seekbar = (SeekBar) alertView.findViewById(R.id.seekBar);
        seekbar.setMax(mValueMax);
        seekbar.setOnSeekBarChangeListener(this);

        float fs = H.stringToFloat(mEditText.getText().toString());

        seekbar.setProgress(Math.round(fs * mDenom));

        dialogBuilder.setPositiveButton(R.string.OK_BUTTON,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //process(input.getText().toString());
                        mPidTextView.setText(mEditText.getText());
                        //mUavTalkDevice.savePersistent(mObject);

                        dialog.dismiss();
                    }
                });

        dialogBuilder.setNegativeButton(R.string.CANCEL_BUTTON,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        dialog.dismiss();
                    }
                });

        alertView.findViewById(R.id.imgPidDialogForward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    float f = H.stringToFloat(mEditText.getText().toString());
                    f += (float) mStep / mDenom;
                    if (f > (float) mValueMax / mDenom) f = (float) mValueMax / mDenom;
                    mEditText.setText(getDecimalString(f));
                    seekbar.setProgress(Math.round(f * mDenom));
                } catch (NumberFormatException e) {
                    Log.w("PidInputAlertDialog", "NPE Forward");
                }
            }
        });

        alertView.findViewById(R.id.imgPidDialogFastForward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    float f = H.stringToFloat(mEditText.getText().toString());
                    f += ((float) mStep / mDenom) * 10;
                    if (f > (float) mValueMax / mDenom) f = (float) mValueMax / mDenom;
                    mEditText.setText(getDecimalString(f));
                    seekbar.setProgress(Math.round(f * mDenom));
                } catch (NumberFormatException e) {
                    Log.w("PidInputAlertDialog", "NPE Fast Forward");
                }
            }
        });

        alertView.findViewById(R.id.imgPidDialogBackward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    float f = H.stringToFloat(mEditText.getText().toString());
                    f -= (float) mStep / mDenom;
                    if (f < 0) f = 0;
                    mEditText.setText(getDecimalString(f));
                    seekbar.setProgress(Math.round(f * mDenom));
                } catch (NumberFormatException e) {
                    Log.w("PidInputAlertDialog", "NPE Backward");
                }
            }
        });

        alertView.findViewById(R.id.imgPidDialogFastBackward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    float f = H.stringToFloat(mEditText.getText().toString());
                    f -= ((float) mStep / mDenom) * 10;
                    if (f < 0) f = 0;
                    mEditText.setText(getDecimalString(f));
                    seekbar.setProgress(Math.round(f * mDenom));
                } catch (NumberFormatException e) {
                    Log.w("PidInputAlertDialog", "NPE Fast Backward");
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {

            private String lastTextOk = mText;

            public void afterTextChanged(Editable s) {
                try {
                    float f = H.stringToFloat(s.toString());
                    if (f < 0 || f > (float) mValueMax / mDenom) {
                        s.clear();
                        s.append(lastTextOk);
                        f = H.stringToFloat(s.toString());
                    }
                    seekbar.setProgress(Math.round(f * mDenom));
                } catch (NumberFormatException e) {
                    Log.w("PidInputAlertDialog", "Edit Text Listener");
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    float f = H.stringToFloat(s.toString());
                    if (f >= 0 && f <= (float) mValueMax / mDenom) {
                        lastTextOk = s.toString();
                    } else {
                        SingleToast.makeText(getContext(),
                                "Values 0 - " + getDecimalString((float) mValueMax / mDenom) + " allowed.",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Log.w("PidInputAertDialog", "onTextChanged");
                }

            }
        });

        dialogBuilder.show();
    }

    private void process(String input) {

        byte[] data;
        try {
            switch (mFieldType) {
                case UAVTalkXMLObject.FIELDTYPE_UINT8:
                    data = new byte[1];
                    data[0] = H.toBytes(Integer.parseInt(input))[3]; //want the lsb
                    break;
                case UAVTalkXMLObject.FIELDTYPE_UINT32:
                    data = H.toBytes(Integer.parseInt(input));
                    if (data.length == 4) {
                        data = H.reverse4bytes(data);
                    } else {
                        data = H.toBytes(0);
                    }
                    break;
                default:
                    Log.e("PidUnputAlertDialog", "Type not implemented!");
                    data = H.toBytes(0);
                    break;
            }

        } catch (NumberFormatException e) {
            data = H.toBytes(0);
        }

        if (mUavTalkDevice != null) {
            mUavTalkDevice.sendSettingsObject(mObject, 0, mField, 0, data);
        }
    }

    public String getDecimalString(float v) {
        DecimalFormat df = new DecimalFormat(mDecimalFormatString);
        return df.format(v);
    }
}