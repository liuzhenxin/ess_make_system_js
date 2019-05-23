package com.clt.ess.utils.HTTP;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

import static com.clt.ess.utils.HTTP.EssHttpUtil.sendPost;

public class httptest2 {
    public static void main(String[] args) {


//        String demo = "";
//
//
//        String[] res = demo.split("\n");
//        List<String> idNums1 = new ArrayList<>(Arrays.asList(res));
//
//        for (String id :idNums){
//            List<String> idNums = new ArrayList<>();
//            //循环取值
//            File file = new File("E:\\南京市-副本");
//            StringBuffer stringBuffer = new StringBuffer();
//            File[] files = file.listFiles();
//            for (File file2 : files) {
//                String fileName =file2.getName();
//                String idNum = fileName.substring(fileName.length()-22,fileName.length()-4);
////                String reg = "[^\u4e00-\u9fa5]";
////                String sealName = fileName.replaceAll(reg, "");
//                stringBuffer.append(idNum+"@");
//            }
//            String a = stringBuffer.toString();
//            if (!a.contains(id)){
//                System.out.println(id);
//            }
//
//        }



//        //循环取值
//        File file = new File("C:\\Users\\陈晓坤\\Desktop\\佳盟和至诚\\工会志成公司\\工会志成公司\\手签");
//        File[] files = file.listFiles();
//        for (File file2 : files) {
//
//            File[] file3 = file2.listFiles();
//            for (File file4 : file3) {
//                String fileName =file4.getName();
//                String idNum = fileName.substring(fileName.length()-22,fileName.length()-4);
//                String reg = "[^\u4e00-\u9fa5]";
//                String sealName = fileName.replaceAll(reg, "");
//                String phone = fileName.substring(0,10);
//                System.out.println(idNum+""+sealName+""+phone);
//            }
//
//            String fileName =file2.getName();
//            String idNum = fileName.substring(fileName.length()-22,fileName.length()-4);
//            String reg = "[^\u4e00-\u9fa5]";
//            String sealName = fileName.replaceAll(reg, "");
//            String phone = fileName.substring(10);
//            System.out.println(idNum+""+sealName+""+phone);
//        }



//        String aaa = "";
//        String[] res = aaa.split("@\n");
//        List<String> idNums1 = new ArrayList<>(Arrays.asList(res));
        List<String> idNums1 = new ArrayList<>();
        idNums1.add("32068119771224321X");
        idNums1.add("320105196803261212");
        idNums1.add("32108119720102753X");
//        idNums1.add("");
//        idNums1.add("");
//        idNums1.add("");
        for(String id :idNums1){

            String parms = "{\"pageNum\":\"1\",\"idCard\":\""+id+"\"}";
            String a = sendPost("http://10.40.4.190:9812/datacap/service/common/access/get_ygbm_org.do",
                    parms);
            System.out.println(a);
        }
    }
}
