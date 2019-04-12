package com.clt.ess.utils;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.clt.ess.base.Constant.PFX_FILE_PATH;
import static com.clt.ess.utils.Base64Utils.decoderBase64File;
import static com.clt.ess.utils.Base64Utils.encodeBase64File;
import static com.clt.ess.utils.FileUtil.byte2Input;
import static com.clt.ess.utils.uuidUtil.getUUID;
import static com.multica.crypt.MuticaCrypt.ESSGetBase64Decode;

public class ReadPfx {
    public ReadPfx (){
    }
    //转换成十六进制字符串
    public static String Byte2String(byte[] b) {
        String hs="";
        String stmp="";

        for (int n=0;n<b.length;n++) {
            stmp=(Integer.toHexString(b[n] & 0XFF));
            if (stmp.length()==1) hs=hs+"0"+stmp;
            else hs=hs+stmp;
            //if (n<b.length-1)  hs=hs+":";
        }
        return hs.toUpperCase();
    }

    public static byte[] StringToByte(int number) {
        int temp = number;
        byte[] b=new byte[4];
        for (int i=b.length-1;i>-1;i--){
            b[i] = new Integer(temp&0xff).byteValue();//将最高位保存在最低位
            temp = temp >> 8; //向右移8位
        }
        return b;
    }

    /**
     * 解析获得pfx  证书包含的信息
     * @param pfxBase64  pfx证书的base64 字符串
     * @param strPassword 密码
     * @return 返回信息集合
     */
    public static Map<String,String> GetCertInfoFromPfxBase64(String pfxBase64, String strPassword) throws Exception {
        Map<String,String> cerInfoMap = new HashMap<>();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        //将传入的base64字符串生成临时文件
        String tempPath = PFX_FILE_PATH+getUUID()+".pfx";

        decoderBase64File(pfxBase64,tempPath);
        //将临时文件读取
        FileInputStream fis = new FileInputStream(tempPath);
        // If the keystore password is empty(""), then we have to set
        // to null, otherwise it won't work!!!
        char[] nPassword = null;
        if ((strPassword == null) || strPassword.trim().equals("")){
            nPassword = null;
        }
        else {
            nPassword = strPassword.toCharArray();
        }
        ks.load(fis, nPassword);
        fis.close();

        Enumeration enumas = ks.aliases();
        String keyAlias = null;
        if (enumas.hasMoreElements())// we are readin just one certificate.
        {
            keyAlias = (String)enumas.nextElement();
        }
        // Now once we know the alias, we could get the keys.
//            System.out.println("is key entry=" + ks.isKeyEntry(keyAlias));
        PrivateKey prikey = (PrivateKey) ks.getKey(keyAlias, nPassword);
        Certificate cert = ks.getCertificate(keyAlias);
        //创建X509工厂类
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate oCert =(X509Certificate)cf.generateCertificate(byte2Input(cert.getEncoded()));

        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        String info = null;
        //获得证书版本
        cerInfoMap.put("version",String.valueOf(oCert.getVersion()));
        //获得证书序列号
        cerInfoMap.put("sn",oCert.getSerialNumber().toString(16));
        //获得证书有效期
        cerInfoMap.put("startTime",dateformat.format(oCert.getNotBefore()));
        //获得证书失效日期
        cerInfoMap.put("endTime",dateformat.format(oCert.getNotAfter()));
        //获得证书主体信息
        cerInfoMap.put("owner",oCert.getSubjectDN().getName());
//            System.out.println("证书扩展域:" + oCert.getSubjectDN().getName());
//            String[] a = oCert.getSubjectDN().getName().split(", ");
//            for(String a12:a){
//                System.out.println(a12);
//            }
        //获得证书颁发者信息
        cerInfoMap.put("issuer",oCert.getIssuerDN().getName());
//            System.out.println("证书扩展域:" + oCert.getIssuerDN().getName());
        //获得证书签名算法名称
        cerInfoMap.put("algorithm",oCert.getSigAlgName());

        return cerInfoMap;

    }

    public static Map<String,String> showCertInfo(String cerBase64) {
        Map<String,String> cerInfoMap = new HashMap<>();
        try {
            //读取证书文件
            byte[] cerByte = ESSGetBase64Decode(cerBase64);
//            byte[] cerByte = cerBase64.getBytes();

            InputStream inStream = byte2Input(cerByte);
            //创建X509工厂类
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            //创建证书对象
            X509Certificate oCert = (X509Certificate)cf.generateCertificate(inStream);
            inStream.close();
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
            String info = null;
            //获得证书版本
            cerInfoMap.put("version",String.valueOf(oCert.getVersion()));
            //获得证书序列号
            cerInfoMap.put("sn",oCert.getSerialNumber().toString(16));
            //获得证书有效期
            cerInfoMap.put("startTime",dateformat.format(oCert.getNotBefore()));
            //获得证书失效日期
            cerInfoMap.put("endTime",dateformat.format(oCert.getNotAfter()));
            //获得证书主体信息
            cerInfoMap.put("owner",oCert.getSubjectDN().getName());
//            System.out.println("证书扩展域:" + oCert.getSubjectDN().getName());
            String[] a = oCert.getSubjectDN().getName().split(", ");
            for(String a12:a){
                System.out.println(a12);
            }
            //获得证书颁发者信息
            cerInfoMap.put("issuer",oCert.getIssuerDN().getName());
//            System.out.println("证书扩展域:" + oCert.getIssuerDN().getName());
            //获得证书签名算法名称
            cerInfoMap.put("algorithm",oCert.getSigAlgName());

//            byte[] byt = oCert.getExtensionValue("1.2.86.11.7.9");
//            String strExt = new String(byt);
//            System.out.println("证书扩展域:" + strExt);
//            byt = oCert.getExtensionValue("1.2.86.11.7.1.8");
//            String strExt2 = new String(byt);
//            System.out.println("证书扩展域2:" + strExt2);
        }
        catch (Exception e) {
            System.out.println("解析证书出错！");
        }

        return cerInfoMap;
    }//end showCertInfo


    /**
     *
     * @param CN 证书名称
     * @param OU 部门
     * @param O 单位
     * @param L 城市
     * @param S 省
     * @param C 国家
     * @return
     */
    public static String getOwnerString(String CN,String OU,String O,String L,String S,String C){

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("CN="+CN+" ");
        stringBuffer.append("OU="+OU+" ");
        stringBuffer.append("O="+O+" ");
        stringBuffer.append("L="+L+" ");
        stringBuffer.append("S="+S+" ");
        stringBuffer.append("C="+C+" ");

        return stringBuffer.toString();

    }

    public static void main(String[] args) throws Exception {
        int a = 11;
        String b = "a" +a;
        System.out.println(b);
    }

}