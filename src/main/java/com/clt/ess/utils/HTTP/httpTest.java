package com.clt.ess.utils.HTTP;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.clt.ess.utils.Base64Utils.encodeBase64File;


public class httpTest {
    public static String unitId = "";
    public static String unitName = "";
    public static String city = "南京市";
    public static void main(String[] args) throws Exception {

        //String unitId,String imgBase64,String sealName,String idNum,String city,String unitName,
        //                             String departName,String certName



        List<SealData> sealDataList = readData("C:\\Users\\陈晓坤\\Desktop\\佳盟和至诚\\工会志成公司\\工会志成公司\\手签");

        String url = "http://10.40.4.7:8080/ess_make_system/make/importSeal.html";
        int i =0;
        if (sealDataList != null) {
            for(SealData sealData:sealDataList){
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("unitId",sealData.getUnitId());
                dataMap.put("imgBase64",sealData.getImgBase64());
                dataMap.put("sealName",sealData.getSealName());
                dataMap.put("idNum",sealData.getIdNum());
                dataMap.put("city",sealData.getCity());
                dataMap.put("unitName",sealData.getUnitName());
                dataMap.put("departName",sealData.getDepartName());
                dataMap.put("certName",sealData.getCertName());

//                String HttpResult = HttpClient.doPost(url,dataMap);
                i++;
                System.out.println(dataMap);
            }
            System.out.println(i);
        }

    }

    public static List<SealData> readData(String path) throws Exception {
        List<SealData> sealDataList = new ArrayList<>();
        //循环取值
        File file = new File(path);
        if (file.exists()) {
            //遍历部门文件夹
            File[] files = file.listFiles();
            if (null == files || files.length == 0) {
                System.out.println("文件夹是空的!");
                return null;
            } else {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
//                        System.out.println("文件夹:" + file2.getAbsolutePath());
                        //在此处遍历文件夹下文件
//                        System.out.println(file2.getName());
                        //遍历部门文件夹下的图片文件
                        File[] files_1 = file2.listFiles();
                        if (files_1 != null) {
                            for (File file3 : files_1) {
                                SealData sealData = new SealData();
                                String data = file3.getName();
                                String idNum = data.substring(data.length()-22,data.length()-4);
                                String reg = "[^\u4e00-\u9fa5]";
                                String sealName = data.replaceAll(reg, "");
                                String imgBase64 = encodeBase64File(file3.getAbsolutePath());
                                sealData.setCity(city);
                                sealData.setUnitName(unitName);
                                sealData.setUnitId(unitId);
//                                sealData.setImgBase64(imgBase64);
                                sealData.setDepartName(file2.getName());
                                sealData.setCertName(sealName);
                                sealData.setIdNum(idNum);
                                sealData.setSealName(sealName);
                                sealDataList.add(sealData);
//                                System.out.println(sealData);
                            }
                        }
                    } else {
                        System.out.println("文件:" + file2.getAbsolutePath());
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
            return null;
        }


        return sealDataList;
    }


}
