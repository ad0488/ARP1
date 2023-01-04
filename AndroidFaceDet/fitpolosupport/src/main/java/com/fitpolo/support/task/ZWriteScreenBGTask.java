package com.fitpolo.support.task;

import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.callback.MokoOrderTaskCallback;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderType;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.utils.DigitalConver;

/**
 * @Date 2019/4/1
 * @Author wenzheng.liu
 * @Description 设置屏幕背景
 * @ClassPath com.fitpolo.support.task.ZWriteScreenBGTask
 */
public class ZWriteScreenBGTask extends OrderTask {
    private static final int ORDERDATA_LENGTH = 8;

    private byte[] orderData;

    public ZWriteScreenBGTask(MokoOrderTaskCallback callback, int fileSize, int index) {
        super(OrderType.WRITE_CHARACTER, OrderEnum.Z_WRITE_SCREEN_BG, callback, OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE);
        orderData = new byte[ORDERDATA_LENGTH];
        orderData[0] = (byte) MokoConstants.HEADER_WRITE_SEND;
        orderData[1] = (byte) order.getOrderHeader();
        orderData[2] = (byte) 0x05;
        byte[] arrays = DigitalConver.int2ByteArr(fileSize, 4);
        for (int i = 0; i < arrays.length; i++) {
            orderData[3 + i] = arrays[i];
        }
        orderData[7] = (byte) index;
    }

    @Override
    public byte[] assemble() {
        return orderData;
    }

    @Override
    public void parseValue(byte[] value) {
        if (order.getOrderHeader() != DigitalConver.byte2Int(value[1])) {
            return;
        }
        if (0x01 != DigitalConver.byte2Int(value[2])) {
            return;
        }

        LogModule.i(order.getOrderName() + "成功");
        orderStatus = OrderTask.ORDER_STATUS_SUCCESS;

        response.responseValue = value;
        MokoSupport.getInstance().pollTask();
        callback.onOrderResult(response);
        MokoSupport.getInstance().executeTask(callback);
    }
}
