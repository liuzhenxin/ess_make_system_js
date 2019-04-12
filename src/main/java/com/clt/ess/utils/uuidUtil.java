package com.clt.ess.utils;

import org.springframework.beans.factory.annotation.Value;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class uuidUtil {
    /**
     * 自动生成32位的UUid，对应数据库的主键id进行插入用。
     * @return
     */
    public static String getUUID() {

        return UUID.randomUUID().toString().replace("-", "");
    }
    /**
     * 自动生成32位的UUid，对应数据库的主键id进行插入用。
     * @return
     */
    public static String getEssUUID(String topUnitId) {
        String unitCode = topUnitId.substring(0,3);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return unitCode+uuid;
    }
    /**
     * 自动生成32位的UUid，对应数据库的主键id进行插入用。
     * @return
     */
    public static String getCertId(String unitId,String sealType) {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateString = formatter.format(currentTime);
        return unitId+sealType+dateString;
    }

}