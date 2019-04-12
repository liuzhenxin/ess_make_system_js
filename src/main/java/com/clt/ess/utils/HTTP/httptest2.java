package com.clt.ess.utils.HTTP;

import java.util.HashMap;
import java.util.Map;

public class httptest2 {
    public static void main(String[] args) {
        String url = "http://10.41.0.66:8080/createConvertLog.html";
        int a =0;
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("fileUrl","http://10.41.0.120:8080/SOA/tools/flexupload/downUpload.jsp?tableName=MMDATA&uploadName=fawen.doc&uploadID=201904101612082659");
        dataMap.put("systemId","025bs1");

        String HttpResult = HttpClient.doPost(url,dataMap);
        System.out.println(HttpResult);
    }
}
