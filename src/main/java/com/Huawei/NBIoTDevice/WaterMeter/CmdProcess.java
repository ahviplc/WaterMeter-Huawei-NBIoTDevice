package com.Huawei.NBIoTDevice.WaterMeter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CmdProcess {

    //private String identifier = "123";
    private String msgType = "deviceReq";
    private String serviceId = "Brightness";
    private String cmd = "SET_DEVICE_LEVEL";
    private int hasMore = 0;
    private int errcode = 0;
    private int mid = 0;
    private JsonNode paras;


    public CmdProcess() {
    }

    public CmdProcess(ObjectNode input) {

        try {
            // this.identifier = input.get("identifier").asText();
            this.msgType = input.get("msgType").asText();
            /*
            平台收到设备上报消息，编码ACK
            {
                "identifier":"0",
                "msgType":"cloudRsp",
                "request": ***,//设备上报的码流
                "errcode":0,
                "hasMore":0
            }
            * */
            if (msgType.equals("cloudRsp")) {
                //在此组装ACK的值
                this.errcode = input.get("errcode").asInt();
                this.hasMore = input.get("hasMore").asInt();
            } else {
            /*
            平台下发命令到设备，输入
            {
                "identifier":0,
                "msgType":"cloudReq",
                "serviceId":"WaterMeter",
                "cmd":"SET_DEVICE_LEVEL",
                "paras":{"value":"20"},
                "hasMore":0

            }
            * */
                //此处需要考虑兼容性，如果没有传mId，则不对其进行编码
                if (input.get("mid") != null) {
                    this.mid = input.get("mid").intValue();
                }
                this.cmd = input.get("cmd").asText();
                this.paras = input.get("paras");
                this.hasMore = input.get("hasMore").asInt();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public byte[] toByte() {
        try {
            if (this.msgType.equals("cloudReq")) {
                /*
                应用服务器下发的控制命令，本例只有一条控制命令：SET_DEVICE_LEVEL
                如果有其他控制命令，增加判断即可。
                * */
                if (this.cmd.equals("SET_DEVICE_LEVEL")) {
                    int brightlevel = paras.get("value").asInt();
                    byte[] byteRead = new byte[5];
                    ByteBufUtils buf = new ByteBufUtils(byteRead);
                    buf.writeByte((byte) 0xAA);
                    buf.writeByte((byte) 0x72);
                    buf.writeByte((byte) brightlevel);

                    //此处需要考虑兼容性，如果没有传mId，则不对其进行编码
                    if (Utilty.getInstance().isValidofMid(mid)) {
                        byte[] byteMid = new byte[2];
                        byteMid = Utilty.getInstance().int2Bytes(mid, 2);
                        buf.writeByte(byteMid[0]);
                        buf.writeByte(byteMid[1]);
                    }

                    return byteRead;
                }
            }

            /*
            平台收到设备的上报数据，根据需要编码ACK，对设备进行响应，如果此处返回null，表示不需要对设备响应。
            * */
            else if (this.msgType.equals("cloudRsp")) {
                byte[] ack = new byte[4];
                ByteBufUtils buf = new ByteBufUtils(ack);
                buf.writeByte((byte) 0xAA);
                buf.writeByte((byte) 0xAA);
                buf.writeByte((byte) this.errcode);
                buf.writeByte((byte) this.hasMore);
                return ack;
            }
            return null;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return null;
        }
    }

}
