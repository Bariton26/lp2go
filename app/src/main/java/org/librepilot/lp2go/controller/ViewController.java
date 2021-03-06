/*
 * @file   ViewController.java
 * @author The LibrePilot Project, http://www.librepilot.org Copyright (C) 2016.
 * @see    The GNU Public License (GPL) Version 3
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.librepilot.lp2go.controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import org.librepilot.lp2go.MainActivity;
import org.librepilot.lp2go.R;
import org.librepilot.lp2go.VisualLog;
import org.librepilot.lp2go.helper.CompatHelper;
import org.librepilot.lp2go.helper.H;
import org.librepilot.lp2go.helper.SettingsHelper;
import org.librepilot.lp2go.uavtalk.UAVTalkMetaData;
import org.librepilot.lp2go.uavtalk.UAVTalkMissingObjectException;
import org.librepilot.lp2go.ui.menu.MenuItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class ViewController {

    public static final int VIEW_3DMAG = 4500;
    public static final int VIEW_ABOUT = 7000;
    public static final int VIEW_DEBUG = 8000;
    public static final int VIEW_LOGS = 5000;
    public static final int VIEW_MAIN = 0;
    public static final int VIEW_MAP = 1000;
    public static final int VIEW_OBJECTS = 2000;
    public static final int VIEW_PID = 3000;
    public static final int VIEW_P_TUNING = 3500;
    public static final int VIEW_SCOPE = 2500;
    public static final int VIEW_SETTINGS = 6000;
    public static final int VIEW_VPID = 4000;
    public static final int VIEW_RESP = 4200;

    protected final HashMap<String, Object> mOffset;
    final private Set<ImageView> mUiImages;
    protected boolean mBlink;
    protected int mFlightSettingsVisible;
    protected int mLocalSettingsVisible;
    protected int mTitle;
    protected Map<Integer, ViewController> mRightMenuItems;
    ViewController mCurrentRightView;
    private MainActivity mActivity;
    private MenuItem mMenuItem;
    private String mPreviousArmedStatus = "";
    private ViewController mFavorite;

    ViewController(MainActivity activity, int title, int icon, int localSettingsVisible,
                   int flightSettingsVisible) {
        this.mActivity = activity;
        mOffset = new HashMap<>();
        mBlink = false;
        this.mLocalSettingsVisible = localSettingsVisible;
        this.mFlightSettingsVisible = flightSettingsVisible;
        this.mTitle = title;
        this.mMenuItem = new MenuItem(getString(mTitle), icon);
        this.mRightMenuItems = new TreeMap<>();
        this.mUiImages = new HashSet<>();
    }

    public String getTitle() {
        return getString(mTitle);
    }

    public abstract int getID();

    protected MainActivity getMainActivity() {
        return this.mActivity;
    }

    public void enter(int view) {
        enter(view, false);
    }

    public ViewController getCurrentRightView() {
        return mCurrentRightView;
    }

    public void setCurrentRightView(ViewController crv) {
        this.mCurrentRightView = crv;
    }

    public void enter(int view, boolean isSubwindow) {

        if (!isSubwindow) {

            mActivity.setContentView(mActivity.mViews.get(view), view);
            //mActivity.setTitle(mActivity.getString(title));
            ActionBar ab = mActivity.getSupportActionBar();
            if (ab != null) {
                ab.setTitle(mTitle);
            }
            mActivity.imgToolbarFlightSettings =
                    (ImageView) findViewById(R.id.imgToolbarFlightSettings);
            if (mActivity.imgToolbarFlightSettings != null) {
                mActivity.imgToolbarFlightSettings.setVisibility(mFlightSettingsVisible);
            }
            mActivity.imgToolbarLocalSettings =
                    (ImageView) findViewById(R.id.imgToolbarLocalSettings);
            if (mActivity.imgToolbarLocalSettings != null) {
                mActivity.imgToolbarLocalSettings.setVisibility(mLocalSettingsVisible);
            }
            mActivity.imgSerial = (ImageView) findViewById(R.id.imgSerial);
            mActivity.imgUavoSanity = (ImageView) findViewById(R.id.imgUavoSanity);

            mActivity.logViewEvent(this);
        }
    }

    protected View findViewById(int res) {
        return mActivity.findViewById(res);
    }

    protected String getString(int res) {
        return mActivity.getString(res);
    }

    protected String getString(int res, int arg) {
        return mActivity.getString(res, arg);
    }

    public void leave() {
        //recycle bitmaps
        for (ImageView iv : mUiImages) {  //unsure if we need this.
            //recycleImageViewSrc(iv);
        }
        mUiImages.clear();
    }

    public void init() {
        //optional method to override
    }

    public void initRightMenu() {
        //optional method to override
    }

    public Map<Integer, ViewController> getMenuRightItems() {
        return mRightMenuItems;
    }

    public MenuItem getMenuItem() {
        return mMenuItem;
    }

    public void update() {
        mBlink = !mBlink;

        //TTS
        if (SettingsHelper.mText2SpeechEnabled) {
            String statusArmed = getData("FlightStatus", "Armed").toString();
            if (!mPreviousArmedStatus.equals(statusArmed)) {
                mPreviousArmedStatus = statusArmed;
                getMainActivity().getTtsHelper().speakFlush(statusArmed);
            }
        }

    }

    public void reset() {
        //optional method to override
    }

    public void onToolbarFlightSettingsClick(View v) {
        //optional method to override
    }

    public void onToolbarLocalSettingsClick(View v) {
        //optional method to override
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //optional method to override
    }

    protected String getFloatData(String obj, String field, int b) {
        try {
            Float f1 = H.stringToFloat(getData(obj, field).toString());
            return String.valueOf(H.round(f1, b));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    protected String getFloatOffsetData(String obj, String field, String soffset) {
        try {
            Float f1 = H.stringToFloat(getData(obj, field).toString());
            Float f2 = (Float) mOffset.get(soffset);
            return String.valueOf(H.round(f1 - f2, 2));
        } catch (NumberFormatException | ClassCastException e) {
            return "";
        }
    }

    protected void setText(TextView t, String text) {
        if (text != null && t != null) {
            t.setText(text);
        }
    }

    protected void setTextBGColor(TextView t, String color) {
        if (color == null || color.equals(getString(R.string.EMPTY_STRING))) {
            return;
        }

        final Context c = mActivity.getApplicationContext();

        switch (color) {
            case "OK":
            case "None":
            case "Connected":
                CompatHelper.setBackground(t, c, R.drawable.rounded_corner_ok);
                break;
            case "Warning":
            case "HandshakeReq":
            case "HandshakeAck":
                CompatHelper.setBackground(t, c, R.drawable.rounded_corner_warning);
                break;
            case "Error":
                CompatHelper.setBackground(t, c, R.drawable.rounded_corner_error);
                break;
            case "Critical":
            case "RebootRequired":
            case "Disconnected":
                CompatHelper.setBackground(t, c, R.drawable.rounded_corner_critical);
                break;
            case "Uninitialised":
                CompatHelper.setBackground(t, c, R.drawable.rounded_corner_unini);
                break;
            case "InProgress":
                CompatHelper.setBackground(t, c, R.drawable.rounded_corner_inprogress);
                break;
            case "Completed":
                CompatHelper.setBackground(t, c, R.drawable.rounded_corner_completed);
                break;
            default: //do nothing
                break;
        }
    }

    Object getData(String objectname, String fieldname, String elementName, boolean request) {
        try {
            if (request) {
                mActivity.mFcDevice.requestObject(objectname);
            }
            return getData(objectname, fieldname, elementName);
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }
        return "";
    }

    boolean requestMetaData(String objectName) {
        try {
            return mActivity.mFcDevice.requestMetaObject(objectName);
        } catch (NullPointerException e) {
            return false;
        }
    }

    boolean sendMetaObject(UAVTalkMetaData o) {
        return mActivity.mFcDevice.sendMetaObject(o.toMessage((byte) 0x22, false));
    }

    UAVTalkMetaData getMetaData(String objectName) throws NullPointerException {
        try {
            String oId = mActivity.mFcDevice.getObjectTree().getXmlObjects().get(objectName).getId();
            String metaId = H.intToHex((int) (Long.decode("0x" + oId) + 1));  //oID + 1
            return new UAVTalkMetaData(metaId, mActivity.mPollThread.
                    mObjectTree.getObjectFromID(metaId).getInstance(0).getData());
        } catch (NullPointerException e) {
            return null;
        }
    }

    Object getData(String objectname, String fieldname, boolean request) {
        try {
            if (request) {
                mActivity.mFcDevice.requestObject(objectname);
            }
            return getData(objectname, fieldname);
        } catch (NullPointerException e) {
            //e.printStackTrace();
        }
        return null;
    }

    Object getData(String objectname, String fieldname) {
        try {
            Object o = mActivity.mPollThread.mObjectTree.getData(objectname, fieldname);
            if (o != null) {
                return o;
            }
        } catch (UAVTalkMissingObjectException e1) {
            try {
                mActivity.mFcDevice.requestObject(e1.getObjectname(), e1.getInstance());
            } catch (NullPointerException e2) {
                //e2.printStackTrace();
            }
        } catch (NullPointerException e3) {
            //e3.printStackTrace();
        }
        return "";
    }

    Object getData(String objectname, String fieldname, int elementindex) {
        Object o = null;
        try {
            o = mActivity.mPollThread.mObjectTree.getData(objectname, 0, fieldname, elementindex);
        } catch (UAVTalkMissingObjectException e1) {
            try {
                mActivity.mFcDevice.requestObject(e1.getObjectname(), e1.getInstance());
            } catch (NullPointerException e2) {
                e2.printStackTrace();
            }
        } catch (NullPointerException e3) {
            VisualLog.e("ERR", "Object Tree not loaded yet.");
        }
        if (o != null) {
            return o;
        } else {
            return "";
        }
    }

    Object getData(String objectname, String fieldname, String elementname) {
        Object o = null;
        try {
            o = mActivity.mPollThread.mObjectTree.getData(objectname, fieldname, elementname);
        } catch (UAVTalkMissingObjectException e1) {
            try {
                mActivity.mFcDevice.requestObject(e1.getObjectname(), e1.getInstance());
            } catch (NullPointerException e2) {
                e2.printStackTrace();
            }
        } catch (NullPointerException e3) {
            VisualLog.e("ERR", "Object Tree not loaded yet.");
        }
        if (o != null) {
            return o;
        } else {
            return "";
        }
    }

    boolean requestObject(@NonNull String obj) {
        if (mActivity != null && mActivity.mFcDevice != null) {
            return mActivity.mFcDevice.requestObject(obj);
        } else {
            return false;
        }
    }

    boolean sendSettingsObjectRevFloat(@NonNull String obj, @NonNull String field, @NonNull String element, float data) {
        if (mActivity != null && mActivity.mFcDevice != null) {
            byte[] newFieldData = H.floatToByteArrayRev(data);
            return mActivity.mFcDevice.sendSettingsObject(obj, 0, field, element, newFieldData);
        } else {
            return false;
        }
    }

    void savePersistent(@NonNull String obj) {
        if (mActivity != null && mActivity.mFcDevice != null) {
            mActivity.mFcDevice.savePersistent(obj);
        }
    }

    protected Float toFloat(Object o) {
        try {
            return (Float) o;
        } catch (ClassCastException e) {
            return .0f;
        }
    }

    public ViewController getFavorite() {
        return mFavorite;
    }

    public void setFavorite(int id) {
        if (this.mFavorite != null) {
            SettingsHelper.mRightMenuFavorites.remove(String.valueOf(this.mFavorite.getID()));
        }
        this.mFavorite = mRightMenuItems.get(id);
        SettingsHelper.mRightMenuFavorites.add(String.valueOf(this.mFavorite.getID()));
        SettingsHelper.saveSettings(getMainActivity(), true);
    }

    public void removeFavorite() {
        this.mFavorite = null;
    }

    public boolean isParent() {
        return false;
    }

    public boolean isChild() {
        return false;
    }

    public void setImageViewSrc(int imageViewRes, final int imgRes, final float aspectRatio) {
        final ImageView imageView = (ImageView) findViewById(imageViewRes);

        //collect all the images so we can easily recycle the bitmaps on leave()
        mUiImages.add(imageView);

        ViewTreeObserver o = imageView.getViewTreeObserver();

        o.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);

                int dph = imageView.getMeasuredHeight();
                int dpw = imageView.getMeasuredWidth();

                final Context c = getMainActivity().getApplicationContext();

                // ar = w / h
                // w = ar * h
                // h = w / ar

                //both aspect ratios are correct
                final int arw = Math.round(aspectRatio * dph);
                final int arh = Math.round(dpw / aspectRatio);

                //which bitmap would be smaller?
                final int arwdph = arw * dph;     //size of image
                final int dpwarh = dpw * arh;     //size of image

                if (arwdph < dpwarh) {
                    dpw = arw;
                } else {
                    dph = arh;
                }

                final Bitmap b = H.drawableToBitmap(imgRes, dpw, dph, c);
                imageView.setImageBitmap(b);

                return true;
            }
        });
    }

    protected void recycleImageViewSrc(int imageViewRes) {
        ImageView iv = (ImageView) findViewById(imageViewRes);
        if (iv != null) {
            recycleImageViewSrc(iv);
        }
    }

    protected void recycleImageViewSrc(@NonNull ImageView iv) {
        Drawable d = iv.getDrawable();
        if (d instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) d;
            Bitmap b = bd.getBitmap();
            b.recycle();
        }
    }

    boolean isConnected() {
        if (mActivity != null && mActivity.getFcDevice() != null) {
            return mActivity.getFcDevice().isConnected();
        } else {
            return false;
        }
    }
}
