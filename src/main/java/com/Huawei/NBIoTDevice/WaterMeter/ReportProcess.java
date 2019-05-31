package com.Huawei.NBIoTDevice.WaterMeter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ReportProcess {
    //private String identifier;

    private String msgType = "deviceReq";
    private int hasMore = 0;
    private int errcode = 0;
    private byte bDeviceReq = 0x00;
    private byte bDeviceRsp = 0x01;

    //serviceId=Brightness字段
    private int brightness = 0;

    //serviceId=Electricity字段
    private double voltage = 0.0;
    private int current = 0;
    private double frequency = 0.0;
    private double powerfactor = 0.0;

    //serviceId=Temperature字段
    private int temperature = 0;

    private byte noMid = 0x00;
    private byte hasMid = 0x01;
    private boolean isContainMid = false;
    private int mid = 0;

    /**
     * @param binaryData 设备发送给平台coap报文的payload部分
     *                   本例入参：AA 72 00 00 32 08 8D 03 20 62 33 99
     *                   byte[0]--byte[1]:  AA 72 命令头
     *                   byte[2]:   00 mstType 00表示设备上报数据deviceReq
     *                   byte[3]:   00 hasMore  0表示没有后续数据，1表示有后续数据，不带按照0处理
     *                   byte[4]--byte[11]:服务数据，根据需要解析//如果是deviceRsp,byte[4]表示是否携带mid, byte[5]--byte[6]表示短命令Id
     * @return
     */
    public ReportProcess(byte[] binaryData) {
        // identifier参数可以根据入参的码流获得，本例指定默认值123
        // identifier = "123";

        /*
        如果是设备上报数据，返回格式为
        {
            "identifier":"123",
            "msgType":"deviceReq",
            "hasMore":0,
            "data":[{"serviceId":"Brightness",
                      "serviceData":{"brightness":50},
                      {
                      "serviceId":"Electricity",
                      "serviceData":{"voltage":218.9,"current":800,"frequency":50.1,"powerfactor":0.98},
                      {
                      "serviceId":"Temperature",
                      "serviceData":{"temperature":25},
                      ]
	    }
	    */
        if (binaryData[2] == bDeviceReq) {
            msgType = "deviceReq";
            hasMore = binaryData[3];

            //serviceId=Brightness 数据解析
            brightness = binaryData[4];

            //serviceId=Electricity 数据解析
            voltage = (double) (((binaryData[5] << 8) + (binaryData[6] & 0xFF)) * 0.1f);
            current = (binaryData[7] << 8) + binaryData[8];
            powerfactor = (double) (binaryData[9] * 0.01);
            frequency = (double) binaryData[10] * 0.1f + 45;

            //serviceId=Temperature 数据解析
            temperature = (int) binaryData[11] & 0xFF - 128;
        }
        /*
        如果是设备对平台命令的应答，返回格式为：
       {
            "identifier":"123",
            "msgType":"deviceRsp",
            "errcode":0,
            "body" :{****} 特别注意该body体为一层json结构。
        }
	    */
        else if (binaryData[2] == bDeviceRsp) {
            msgType = "deviceRsp";
            errcode = binaryData[3];
            //此处需要考虑兼容性，如果没有传mId，则不对其进行解码
            if (binaryData[4] == hasMid) {
                mid = Utilty.getInstance().bytes2Int(binaryData, 5, 2);
                if (Utilty.getInstance().isValidofMid(mid)) {
                    isContainMid = true;
                }

            }
        } else {
            return;
        }


    }

    public ObjectNode toJsonNode() {
        try {
            //组装body体
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();

            // root.put("identifier", this.identifier);
            root.put("msgType", this.msgType);

            //根据msgType字段组装消息体
            if (this.msgType.equals("deviceReq")) {
                root.put("hasMore", this.hasMore);
                ArrayNode arrynode = mapper.createArrayNode();

                //serviceId=Brightness 数据组装
                ObjectNode brightNode = mapper.createObjectNode();
                brightNode.put("serviceId", "Brightness");
                ObjectNode brightData = mapper.createObjectNode();
                brightData.put("brightness", this.brightness);
                brightNode.put("serviceData", brightData);
                arrynode.add(brightNode);
                //serviceId=Electricity 数据组装
                ObjectNode electricityNode = mapper.createObjectNode();
                electricityNode.put("serviceId", "Electricity");
                ObjectNode electricityData = mapper.createObjectNode();
                electricityData.put("voltage", this.voltage);
                electricityData.put("current", this.current);
                electricityData.put("frequency", this.frequency);
                electricityData.put("powerfactor", this.powerfactor);
                electricityNode.put("serviceData", electricityData);
                arrynode.add(electricityNode);
                //serviceId=Temperature 数据组装
                ObjectNode temperatureNode = mapper.createObjectNode();
                temperatureNode.put("serviceId", "Temperature");
                ObjectNode temperatureData = mapper.createObjectNode();
                temperatureData.put("temperature", this.temperature);
                temperatureNode.put("serviceData", temperatureData);
                arrynode.add(temperatureNode);

                //serviceId=Connectivity 数据组装
                ObjectNode ConnectivityNode = mapper.createObjectNode();
                ConnectivityNode.put("serviceId", "Connectivity");
                ObjectNode  ConnectivityData = mapper.createObjectNode();
                ConnectivityData.put("signalStrength", 5);
                ConnectivityData.put("linkQuality", 10);
                ConnectivityData.put("cellId", 9);
                ConnectivityNode.put("serviceData", ConnectivityData);
                arrynode.add(ConnectivityNode);

                //serviceId=battery 数据组装
                ObjectNode batteryNode = mapper.createObjectNode();
                batteryNode.put("serviceId", "Battery");
                ObjectNode batteryData = mapper.createObjectNode();
                batteryData.put("batteryVoltage", 25);
                batteryData.put("battervLevel", 12);
                batteryNode.put("serviceData", batteryData);
                arrynode.add(batteryNode);

                root.put("data", arrynode);

            } else {
                root.put("errcode", this.errcode);
                //此处需要考虑兼容性，如果没有传mid，则不对其进行解码
                if (isContainMid) {
                    root.put("mid", this.mid);//mid
                }
                //组装body体，只能为ObjectNode对象
                ObjectNode body = mapper.createObjectNode();
                body.put("result", 0);
                root.put("body", body);
            }
            return root;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}