# 超高频（UHF） RFID 卡读写的 Cordova 插件

## 说明
**使用UHF6_SDK_v2.5**

## 安装
使用 Cordova
<pre>cordova plugin add https://github.com/shuto-cn/UHFR</pre>

使用 Ionic
<pre>ionic cordova plugin add https://github.com/shuto-cn/UHFR</pre>

## 示例程序
https://github.com/shuto-cn/UHFRDEMO

## 目前提供的功能
### 单次询卡 - 读取卡的 EPC
* 调用：
<pre>cordova.plugins.uhf.searchCard(successCallBack, errorCallback);</pre>
* 参数：
* 返回值：
EPC数组，因为可能读到多个。<pre>["30396062C3AE88C00021E2BC"]</pre>

### 连续询卡 - 读取卡的 EPC
* 调用：
<pre>cordova.plugins.uhf.inventoryCard(successCallBack, errorCallback);</pre>
* 参数：
* 返回值：
EPC数组，因为可能读到多个。<pre>["30396062C3AE88C00021E2BC"]</pre>

### 停止询卡 - 读取卡的 EPC
* 调用：
<pre>cordova.plugins.uhf.stopInventoryCard(successCallBack, errorCallback);</pre>

### 写卡
* 调用：
<pre>cordova.plugins.uhf.writeCard(message, successCallBack, errorCallback);</pre>
* 参数：
<pre>
{
  data: "内容"
}
</pre>
* 内容为需要写入user区的数据，支持中英文，上限为128位16进制
* 返回值：“写入成功”或“写入失败”

### 读卡
* 仅读取user区的内容
* 调用：
<pre>cordova.plugins.uhf.readCard(successCallBack, errorCallback);</pre>
* 返回值：
<pre>"内容"</pre>

### 设置功率
* 调用：
<pre>cordova.plugins.uhf.setPower(power, successCallBack, errorCallback);</pre>
* 参数：
<pre>
目前支持的功率值为 16 至 26，超出范围会按最大或最小值处理。
</pre>
* 返回值：“设置成功”或“设置失败”
