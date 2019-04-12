package com.clt.ess.utils.HTTP;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.clt.ess.utils.Base64Utils.encodeBase64File;


public class httpTest1 {

    public static void main(String[] args) throws Exception {

        List<SealData> sealDataList = readData("E:\\印章数据");

        String url = "http://10.116.0.60:6888/ess_make_system/make/importSeal.html";
        int a =0;
        if (sealDataList != null) {
            for(SealData sealData:sealDataList){
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("certBase64",sealData.getCertBase64());
                dataMap.put("imgBase64",sealData.getImgBase64());
                dataMap.put("sealName",sealData.getSealName());
                a++;
                String HttpResult = HttpClient.doPost(url,dataMap);
                System.out.println(HttpResult);
//                System.out.println(dataMap);
            }
            System.out.println(a);
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
                    String sealName =file2.getName();
//                    System.out.println(file2.getName());
                    if (file2.isDirectory()) {
                        //遍历部门文件夹下的图片文件
                        File[] files_1 = file2.listFiles();
                        if (files_1 != null) {
                            String imgBase64 ="";
                            String certBase64 ="";
                            for (File file3 : files_1) {
                                String fileName= file3.getName();
                                String name = "";
                                String certPath = "";
                                if (fileName.contains("gif")){
                                    //文件是图片
                                    name = fileName.substring(0,fileName.length()-4);
                                    certPath = "E:\\印章数据\\"+sealName+"\\"+name+"..cer";
//                                    System.out.println(certPath);
//                                    System.out.println(file3.getAbsolutePath());
//                                    System.out.println(name);
                                    certBase64 = encodeBase64File(certPath);
                                    imgBase64 = encodeBase64File(file3.getAbsolutePath());
                                    SealData sealData = new SealData();
                                    sealData.setCertBase64(certBase64);
                                    sealData.setImgBase64(imgBase64);
                                    sealData.setSealName(name);
                                    sealDataList.add(sealData);

                                }else{
                                    //文件是证书，不用管
                                }

                            }
                        }
                    } else {
                        System.out.println("文件:" + file2.getAbsolutePath());
                    }
                }
            }
        } else {
            System.out.println("文件夹不存在!");
            return null;
        }
        return sealDataList;
    }


}
