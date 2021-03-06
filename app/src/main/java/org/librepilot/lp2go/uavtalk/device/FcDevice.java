/*
 * @file   FcDevice.java
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

package org.librepilot.lp2go.uavtalk.device;

import android.content.Context;
import android.widget.Toast;

import org.librepilot.lp2go.MainActivity;
import org.librepilot.lp2go.VisualLog;
import org.librepilot.lp2go.helper.H;
import org.librepilot.lp2go.helper.SettingsHelper;
import org.librepilot.lp2go.uavtalk.UAVTalkDeviceHelper;
import org.librepilot.lp2go.uavtalk.UAVTalkMessage;
import org.librepilot.lp2go.uavtalk.UAVTalkObject;
import org.librepilot.lp2go.uavtalk.UAVTalkObjectTree;
import org.librepilot.lp2go.uavtalk.UAVTalkXMLObject;
import org.librepilot.lp2go.ui.SingleToast;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class FcDevice {

    public static final int GEL_STOPPED = 0;
    public static final byte UAVTALK_CONNECTED = 0x03;
    public static final byte UAVTALK_DISCONNECTED = 0x00;
    public static final byte UAVTALK_HANDSHAKE_ACKNOWLEDGED = 0x02;
    public static final byte UAVTALK_HANDSHAKE_REQUESTED = 0x01;
    public static final int GEL_PAUSED = 1;
    public static final int GEL_RUNNING = -1;
    private static final int MAX_HANDSHAKE_FAILURE_CYCLES = 3;
    public final Set<String> nackedObjects;
    final MainActivity mActivity;
    volatile UAVTalkObjectTree mObjectTree;
    private int mFailedHandshakes = 0;
    private boolean mIsLogging = false;
    private long mLogBytesLoggedOPL = 0;
    private long mLogBytesLoggedUAV = 0;
    private String mLogFileName = "OP-YYYY-MM-DD_HH-MM-SS";
    private long mLogObjectsLogged = 0;
    private FileOutputStream mLogOutputStream;
    private long mLogStartTimeStamp;
    private int mUavTalkConnectionState = 0x00;
    FcDevice(MainActivity mActivity) throws IllegalStateException {
        this.mActivity = mActivity;
        nackedObjects = new HashSet<>();
    }

    public void setGuiEventListener(GuiEventListener gel) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public long getLogBytesLoggedOPL() {
        return mLogBytesLoggedOPL;
    }

    public long getLogBytesLoggedUAV() {
        return mLogBytesLoggedUAV;
    }

    public String getLogFileName() {
        return mLogFileName;
    }

    public long getLogObjectsLogged() {
        return this.mLogObjectsLogged;
    }

    public long getLogStartTimeStamp() {
        return this.mLogStartTimeStamp;
    }

    public UAVTalkObjectTree getObjectTree() {
        return mObjectTree;
    }

    public abstract boolean isConnected();

    public abstract boolean isConnecting();

    public boolean isLogging() {
        return this.mIsLogging;
    }

    public void setLogging(boolean logNow) {
        if (mIsLogging == logNow) { //if we are already logging, and we should start, just return
            return;                 //if we are not logging and should stop, nothing to do as well
        }

        mIsLogging = logNow;

        try {       //anyway, close the current stream
            mLogOutputStream.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }

        if (mIsLogging) {  //if logging should start, create new stream
            //mActivity.deleteFile(mLogFileName); //delete old log
            mLogFileName = H.getLogFilename();

            try {
                mLogOutputStream = mActivity.openFileOutput(mLogFileName, Context.MODE_PRIVATE);
                //outputStream.write(string.getBytes());
                //outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mLogStartTimeStamp = System.currentTimeMillis();  //and set the time offset
            mLogBytesLoggedOPL = 0;
            mLogBytesLoggedUAV = 0;
            mLogObjectsLogged = 0;
        }
    }

    public void log(UAVTalkMessage m) {
        // byte[] type = new byte[1];
        //type[0] = m.getType();
        //VisualLog.d("DGB", "Logging object 0x"+ H.intToHex(m.getObjectId()) +" with messagetype " + H.bytesToHex(type) + " and timestamp " + m.getTimestamp());
        log(m.getRaw(), m.getTimestamp());
    }

    private void log(byte[] b, int timestamp) {
        if (b == null) {
            return;
        }
        try {
            byte[] msg;
            if (SettingsHelper.mLogAsRawUavTalk) {
                msg = b;
            } else {
                long time;

                if (timestamp != -1 && SettingsHelper.mUseTimestampsFromFc) {
                    time = timestamp;
                } else {
                    time = System.currentTimeMillis() - mLogStartTimeStamp;
                }

                long len = b.length;

                //time is long, so reverse8bytes is just fine.

                @SuppressWarnings("ConstantConditions")
                byte[] btime = Arrays.copyOfRange(H.reverse8bytes(H.toBytes(time)), 0, 4);
                byte[] blen = H.reverse8bytes(H.toBytes(len));

                msg = H.concatArray(btime, blen);
                msg = H.concatArray(msg, b);
            }
            mLogOutputStream.write(msg);
            mLogBytesLoggedUAV += b.length;
            mLogBytesLoggedOPL += msg.length;
            mLogObjectsLogged++;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public abstract void start();

    public abstract void stop();

    public boolean sendSettingsObject(String objectName, int instance, String fieldName,
                                      String element, byte[] newFieldData) {
        return sendSettingsObject(
                objectName,
                instance,
                fieldName,
                mObjectTree.getElementIndex(objectName, fieldName, element),
                newFieldData,
                false
        );
    }

    public boolean sendSettingsObject(String objectName, int instance, String fieldName,
                                      int element, byte[] newFieldData) {
        return sendSettingsObject(
                objectName,
                instance,
                fieldName,
                element,
                newFieldData,
                false
        );
    }

    protected abstract boolean writeByteArray(byte[] bytes);

    public abstract boolean sendAck(String objectId, int instance);

    public abstract boolean sendSettingsObject(String objectName, int instance);

    public abstract boolean sendSettingsObject(String objectName, int instance, String fieldName,
                                               int element, byte[] newFieldData,
                                               final boolean block);

    public abstract boolean requestObject(String objectName);

    public boolean requestMetaObject(String objectName) {
        try {
            UAVTalkXMLObject xmlObj = mObjectTree.getXmlObjects().get(objectName);
            if (xmlObj == null) {
                return false;
            }

            if (nackedObjects.contains(xmlObj.getId())) {
                VisualLog.d("NACKED META", xmlObj.getId());
                return false;   //if it was already nacked, don't try to get it again
                //If the original object was nacked, there is no metadata as well
            }

            //metadataid is id +1... yes, this is hacky.
            String metaId = H.intToHex((int) (Long.decode("0x" + xmlObj.getId()) + 1));
            //FIXME:Too hacky....

            byte[] send = UAVTalkObject.getReqMsg((byte) 0x21, metaId, 0);

            writeByteArray(send);
            mActivity.incTxObjects();
            return true;
        } catch (Exception e) {
            VisualLog.d("FcDevice", "Could not request MetaData for " + objectName);
            return false;
        }
    }


    public abstract boolean requestObject(String objectName, int instance);

    public void savePersistent(String saveObjectName) {
        try {
            mObjectTree.getObjectFromName("ObjectPersistence").setWriteBlocked(true);

            byte[] op = {0x02};
            UAVTalkDeviceHelper
                    .updateSettingsObject(mObjectTree, "ObjectPersistence", 0, "Operation", 0, op);

            byte[] sel = {0x00};
            UAVTalkDeviceHelper
                    .updateSettingsObject(mObjectTree, "ObjectPersistence", 0, "Selection", 0, sel);
            String sid = mObjectTree.getXmlObjects().get(saveObjectName).getId();

            byte[] oid = H.reverse4bytes(H.hexStringToByteArray(sid));

            UAVTalkDeviceHelper
                    .updateSettingsObject(mObjectTree, "ObjectPersistence", 0, "ObjectID", 0, oid);

            //for the last things we set, we can just use the sendsettingsobject. It will call updateSettingsObjectDeprecated for the last field.
            byte[] ins = {0x00};
            UAVTalkDeviceHelper
                    .updateSettingsObject(mObjectTree, "ObjectPersistence", 0, "InstanceID", 0, ins);

            sendSettingsObject("ObjectPersistence", 0);

            mObjectTree.getObjectFromName("ObjectPersistence").setWriteBlocked(false);
        } catch (NullPointerException e) {
            VisualLog.e(e);
            if (mActivity != null) {
                SingleToast.show(mActivity, "Persistent save failed.", Toast.LENGTH_LONG);
            }
        }
    }

    public boolean sendMetaObject(byte[] data) {

        if (data != null) {
            mActivity.incTxObjects();

            writeByteArray(data);

            return true;
        } else {
            return false;
        }
    }

    public void handleHandshake(byte flightTelemtryStatusField) {

        //if(SettingsHelper.mSerialModeUsed == MainActivity.SERIAL_LOG_FILE) {
        //    return;
        //}

        if (mFailedHandshakes > MAX_HANDSHAKE_FAILURE_CYCLES) {
            mUavTalkConnectionState = UAVTALK_DISCONNECTED;
            mFailedHandshakes = 0;
            //VisualLog.d("Handshake", "Setting DISCONNECTED " + mUavTalkConnectionState + " " + flightTelemtryStatusField);
        }

        if (mUavTalkConnectionState == UAVTALK_DISCONNECTED) {
            //Send Handshake initiator packet (HANDSHAKE_REQUEST)
            byte[] msg = new byte[1];
            msg[0] = UAVTALK_HANDSHAKE_REQUESTED;
            sendSettingsObject("GCSTelemetryStats", 0, "Status", 0, msg);
            mUavTalkConnectionState = UAVTALK_HANDSHAKE_REQUESTED;
            //VisualLog.d("Handshake", "Setting REQUESTED " + mUavTalkConnectionState + " " + flightTelemtryStatusField);
        } else if (flightTelemtryStatusField == UAVTALK_HANDSHAKE_ACKNOWLEDGED &&
                mUavTalkConnectionState == UAVTALK_HANDSHAKE_REQUESTED) {
            byte[] msg = new byte[1];
            msg[0] = UAVTALK_CONNECTED;
            sendSettingsObject("GCSTelemetryStats", 0, "Status", 0, msg);
            mUavTalkConnectionState = UAVTALK_CONNECTED;
            mFailedHandshakes++;
            //VisualLog.d("Handshake", "Setting CONNECTED " + mUavTalkConnectionState + " " + flightTelemtryStatusField);
        } else if (flightTelemtryStatusField == UAVTALK_CONNECTED &&
                mUavTalkConnectionState == UAVTALK_CONNECTED) {
            //We are connected, that is good.
            mFailedHandshakes = 0;
            //VisualLog.d("Handshake", "We're connected. How nice. " + mUavTalkConnectionState + " " + flightTelemtryStatusField);
        } else if (flightTelemtryStatusField == UAVTALK_CONNECTED) {
            //the fc thinks we are connected.
            mUavTalkConnectionState = UAVTALK_CONNECTED;
            mFailedHandshakes = 0;
            //VisualLog.d("Handshake", "The FC thinks we are connected." + mUavTalkConnectionState + " " + flightTelemtryStatusField);
        } else {
            mFailedHandshakes++;
            //VisualLog.d("Handshake", "Failed " + mUavTalkConnectionState + " " + flightTelemtryStatusField);
        }
        byte[] myb = new byte[1];
        myb[0] = (byte) mUavTalkConnectionState;
        UAVTalkDeviceHelper
                .updateSettingsObject(mObjectTree, "GCSTelemetryStats", 0, "Status", 0, myb);
        requestObject("FlightTelemetryStats");
    }

    public abstract void drawConnectionLogo(boolean blink);

    public interface GuiEventListener {
        void reportState(int i);

        void reportDataSource(String dataSource);

        void reportDataSize(float dataSize);

        void reportObjectCount(int objectCount);

        void reportRuntime(long ms);

        void incObjectsReceived(int objRec);

        void incObjectsSent(int objSent);

        void incObjectsBad(int ObjBad);
    }
}
