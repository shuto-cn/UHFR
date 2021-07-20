package cordova.plugin.uhf;

import android.os.Message;
import android.util.Log;
import cn.pda.serialport.Tools;
import com.handheld.uhfr.UHFRManager;
import com.uhf.api.cls.Reader;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;


/**
 * 超高频（UHF）读写卡插件。
 * 因不同的机型使用的so库不同，故无法适配所有机型。
 */
public class UHF extends CordovaPlugin {
    private UHFRManager manager;
    private String selectedEpc = "";
    private byte[] password = Tools.HexString2Bytes("00000000");
    private boolean threadFlag;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        manager = UHFRManager.getInstance();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "readCard":
                this.readCard(callbackContext);
                return true;
            case "searchCard":
                this.searchCard(callbackContext);
                return true;
            case "stopSearchCard":
                this.stopSearchCard(callbackContext);
                return true;
            case "writeCard":
                this.writeCard(args, callbackContext);
                return true;
            case "setPower":
                this.setPower(args, callbackContext);
                return true;
            case "startWork":
                this.startWork(callbackContext);
                return true;
            case "endWork":
                this.endWork(callbackContext);
                return true;
            case "selectCard":
                this.selectCard(args, callbackContext);
                break;
            case "inventoryCard":
                this.inventoryCard(callbackContext);
                return true;
            case "stopInventoryCard":
                this.stopInventoryCard(callbackContext);
                return true;
        }
        return false;
    }


    private void startWork(CallbackContext callbackContext) {
        if (this.manager == null) {
            manager = UHFRManager.getInstance();
        }
        callbackContext.success();
    }

    private void endWork(CallbackContext callbackContext) {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        callbackContext.success();
    }


    private void stopInventoryCard(CallbackContext callbackContext) {
        threadFlag = false;
        manager.asyncStopReading();
        callbackContext.success("停止");
    }

    private void inventoryCard(CallbackContext callbackContext) {
        threadFlag = true;
        manager.asyncStartReading();
        Thread thread = new InventoryThread(callbackContext);
        thread.start();
    }


    /**
     * 巡卡，每执行一次，就扫描一次范围内的卡片，将巡到的卡片信息以json数组的形式返回
     */
    private void searchCard(CallbackContext callbackContext) {
        manager.asyncStartReading();
        try {
            Thread.sleep(100);
            JSONArray ja = onceSearchCard();
            manager.asyncStopReading();
            callbackContext.success(ja);
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }

    private JSONArray onceSearchCard() throws JSONException {
        List<Reader.TAGINFO> list1;
        list1 = manager.tagInventoryRealTime();
        String data;
        JSONArray ja = new JSONArray();
        JSONObject jo;
        if (list1 != null && list1.size() > 0) {
            for (Reader.TAGINFO tfs : list1) {
                jo = new JSONObject();
                byte[] epcdata = tfs.EpcId;
                data = Tools.Bytes2HexString(epcdata, epcdata.length);
                int rssi = tfs.RSSI;
                jo = new JSONObject();
                jo.put("mEpcBytes", data);
                jo.put("mRssi", rssi);
                ja.put(jo);
            }
        }
        return ja;
    }

    private class InventoryThread extends Thread {

        private CallbackContext cb;

        public InventoryThread(CallbackContext cb) {
            super();
            this.cb = cb;
        }

        public InventoryThread() {
            super();
        }

        @Override
        public void run() {
            super.run();
            try {
                while (threadFlag) {
                    JSONArray ja = onceSearchCard();
                    PluginResult pr = new PluginResult(PluginResult.Status.OK, ja);
                    pr.setKeepCallback(true);
                    cb.sendPluginResult(pr);
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                cb.error(e.getMessage());
            }
        }
    }

    private void stopSearchCard(CallbackContext callbackContext) {
        manager.asyncStopReading();
        callbackContext.success("巡卡停止");
    }

    /**
     * 读卡，将USER区的hex字符串转换为ascii读取，因EPC区与TID区不能写入，故暂时只读取USER区，注释掉的部分为适应EPC区与TID区读取的代码
     * 将注释解开后应抛出JSONException
     */
    private void readCard(CallbackContext callbackContext) {
//    JSONObject obj = message.getJSONObject(0);
//    int site = obj.getInt("site");
//    int addr = obj.getInt("addr");
//    if (site == 1) {
//      addr = 2;
//      length = 6;
//    } else if (site == 3) {
//      addr = 0;
//      length = 32;
//    } else if (site == 2) {
//      addr = 0;
//      length = 12;
//    }
//        manager.selectEPC(Tools.HexString2Bytes(this.selectedEpc));
        int length = 32;
//        byte[] data = manager.readFrom6C(3, 0, length, password);
        byte[] data = new byte[length * 2];
        if (this.selectedEpc.length() == 0) {
            manager.getTagData(3, 0, length, data, password, (short) 1000);
        } else {
            data = manager.getTagDataByFilter(3, 0, length, password, (short) 1000, Tools.HexString2Bytes(this.selectedEpc), 1, 0, true);
        }
        if (data != null && data.length >= 1) {
            String msg;
//      if (site == 3) {   // 读取User区的时候，将16进制字符串转换为ascii
            msg = Util.bytes2Str(data);
//      } else { // 因EPC区与TID区不能写入，故读取时不转码
//        msg = Tools.Bytes2HexString(data, data.length);
//      }
            callbackContext.success(msg);
        } else {
            callbackContext.error("读取失败");
        }
    }

    /**
     * 选卡，将选到的卡储存在this.selectedEpc中
     */
    private void selectCard(JSONArray message, CallbackContext callbackContext) throws JSONException {
        JSONObject obj = message.getJSONObject(0);
        String epc = obj.getString("epc");
//        manager.selectEPC(Tools.HexString2Bytes(epc));
        this.selectedEpc = epc;
        callbackContext.success();
    }

    /**
     * 写卡，将ascii转换为bytes并写入
     */
    private void writeCard(JSONArray message, CallbackContext callbackContext) {
//        manager.selectEPC(Tools.HexString2Bytes(this.selectedEpc));
        String _data = null;
        try {
            JSONObject obj = message.getJSONObject(0);
            _data = Util.str2HexStr(obj.getString("data"));
        } catch (JSONException e) {
            callbackContext.error("JSON解析失败");
        }
        // 处理_data，将ascii转换为16进制字符串，超过32 * 4位的16进制字符串报错，小于32 * 4时在16进制字符串末位补0x00作为完结标记，再转换为bytes
        if (_data != null && _data.length() > 32 * 4) {
            callbackContext.error("数据过长");
            return;
        }
        if (_data.length() < 32 * 4) {
            _data += "00";
        }
        byte[] dataBytes = Util.hexStringToBytes(_data);
        boolean writeFlag = false;
        Reader.READER_ERR er = null;
        if (dataBytes != null) {
            if (this.selectedEpc.length() > 0) {
                er = manager.writeTagDataByFilter((char) 3, 0, dataBytes, dataBytes.length / 2, password, (short) 1000, Tools.HexString2Bytes(this.selectedEpc), 1, 0, true);
            } else {
                er = manager.writeTagData((char) 3, 0, dataBytes, dataBytes.length / 2, password, (short) 1000);
            }
//      writeFlag = manager.writeTo6C(password, 3, 0, dataBytes.length / 2, dataBytes);
        }
        if (er == Reader.READER_ERR.MT_OK_ERR) {
            callbackContext.success("写入成功");
        } else {
            callbackContext.error("写入失败");
        }
    }

    private void setPower(JSONArray message, CallbackContext callbackContext) throws JSONException {
        int power = message.getInt(0);
        if (power > 30) {
            power = 30;
        } else if (power < 5) {
            power = 5;
        }
        Reader.READER_ERR err = manager.setPower(power, power);
        if (err == Reader.READER_ERR.MT_OK_ERR) {
            callbackContext.success("设置成功");
        } else {
            callbackContext.error("设置失败");
        }
    }

    @Override
    public void onDestroy() {
        if (this.manager != null) {
            this.manager.close();
        }
    }
}
