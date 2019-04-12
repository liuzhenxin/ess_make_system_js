package com.clt.ess.utils;

import com.clt.ess.base.Constant;
import com.clt.ess.entity.IssuerUnit;
import com.multica.crypt.MuticaCryptException;
import sun.misc.BASE64Encoder;
import sun.security.x509.*;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.clt.ess.utils.FileUtil.byte2File;
import static com.clt.ess.utils.StringUtils.getDecryptPwd;
import static com.clt.ess.utils.dateUtil.getDate;
import static com.clt.ess.utils.uuidUtil.getUUID;
import static com.multica.crypt.MuticaCrypt.ESSGetBase64Decode;

public class CertificateUtil {

    private static int HASHBUFSIZE = 1024*1024;

    public static byte[] ESSGetDigest(byte[] bMsg){
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            int iOffset = 0;
            do {
                int len = bMsg.length - iOffset;
                if (len > HASHBUFSIZE)
                    len = HASHBUFSIZE;
                md.update(bMsg, iOffset, len);
                iOffset += len;
            } while (iOffset < bMsg.length);
            return md.digest();
        }catch(GeneralSecurityException e)
        {
            return null;
        }
    }
    /***
     * 这个函数根据传入的证书，返回颁发者证书（也就是根证书）的私钥
     * 参数在这里没有任何意义
     * */
    private static PrivateKey GetPrivateKeyByCert(IssuerUnit issuerUnit)
    {
        //作为根证书的PFX证书路径
//        String sFile = IssuerUnitRootPath;
        //作为根证书的PFX证书的密码
        String sPwd = "111111";
        String sFileType = "PKCS12";
        try
        {
            FileInputStream fis;
            fis = new FileInputStream(issuerUnit.getIssuerUnitPfx());
            char[] nPassword = null;
            nPassword = issuerUnit.getPfxPwd().toCharArray();
            KeyStore inputKeyStore = KeyStore.getInstance(sFileType);
            inputKeyStore.load(fis, nPassword);
            Enumeration<String> enuma = inputKeyStore.aliases();
            String keyAlias = null;
            keyAlias = (String) enuma.nextElement();
            Key key = null;
            if (inputKeyStore.isKeyEntry(keyAlias))
                key = inputKeyStore.getKey(keyAlias, nPassword);
            fis.close();
            PrivateKey pk = (PrivateKey)key;
            return pk;
        }catch(IOException e)
        {

        }catch(GeneralSecurityException e)
        {

        }
        return null;
    }


    /**
     * 这个函数返回根证书的基本信息
     * **/
    private static X500Name GetIssuerInfo(IssuerUnit issuerUnit)
    {
        File f = new File(issuerUnit.getIssuerUnitRoot());
        long len = f.length();
        byte[] bIssuerPfx = new byte[(int) len];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            fis.read(bIssuerPfx);
            fis.close();
            X509CertImpl cimpl=new X509CertImpl(bIssuerPfx);
            X509CertInfo cinfol=(X509CertInfo)cimpl.get(X509CertImpl.NAME+"."+X509CertImpl.INFO);
            X500Name bIssuer=(X500Name)cinfol.get(X509CertInfo.SUBJECT+"."+CertificateIssuerName.DN_NAME);
            return bIssuer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * 这个按说再是不用管，一时半会用不上
     * 生成自签名证书 ，应将其改造一下。
     * 第一句 x500String= 后面的内容应该改为传入
     * CN 是印章或人员的身份证号码
     * OU 是部门名称，如果是人员，填写其姓名
     * O 是单位名称，如果是人员，填写“ ”
     * L 是单位所在地（市级），如果是人员，填“ ”
     * S 是单位所在地（省级）
     * C 固定是 中国
     * **/
    public static void CreatePfxItSelf() throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException, InvalidKeyException, NoSuchProviderException, SignatureException
    {
        String x500String = "CN = 乌江渡发电厂电子签章,OU = 乌江渡发电厂电子签章,O = 乌江渡发电厂,L = 遵义,S = 贵州,C = 中国";
        long lDateLen = 10*365;
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        KeyPair keyPair = kpg.generateKeyPair();
        X509CertInfo info = new X509CertInfo();
        Date from = new Date(new Date().getTime()-86400000l);
        long time = from.getTime();
        long agoTime = time-86400000l;
        Date to = new Date(agoTime + lDateLen * 86400000l);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());

        X500Name owner = new X500Name(x500String);

        X500Name bIssuer = new X500Name(x500String);
        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
        info.set(X509CertInfo.ISSUER, new CertificateIssuerName(bIssuer));
        info.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(keyPair.getPrivate(), "MD5WithRSA");
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry("esspfx", keyPair.getPrivate(), "123456".toCharArray(), new Certificate[] { cert });
        FileOutputStream fos =new FileOutputStream("d:/temp/selfPfx.pfx");
        store.store(fos, "123456".toCharArray());
        fos.close();
        return;
    }


    private static String GetDefaultCertOwnerInfo()
    {
        return "CN = ccn,OU = oou,O = oox,L = ool,S = ooS,C = ooC";
    }



    /**
     * @param sC			所在国家
     * @param sS			所在省
     * @param sL 			所在市
     * @param sO			单位名称
     * @param sOU			部门（单位）名称
     * @param sDN			印章名称或个人姓名
     * @param dateStart     有效期起始
     * @param dateEnd		有效期到期
     * @param sPwd			新证书的使用密钥   6--8  字符  数字 组合
     * @param algorithm     签名算法
     * @return				返回新生成的cer、pfx证书
    //	 * @throws MuticaCryptException
     */
    public static  Map<String, String> CreatePfxFile(String sC,String sS,String sL,String sO,String sOU,String sDN,Date dateEnd,Date dateStart,
                                       String sPwd,String algorithm,IssuerUnit issuerUnit)
    {
        /*
         *  先生成一份自签名证书，然后对自签名证书的公钥证书使用颁发者证书签�?
         * */
        try {
            //获取颁发者证书私钥
            PrivateKey issuer_PrivateKey = GetPrivateKeyByCert(issuerUnit);
            //根证书的基本信息
            X500Name bIssuer=GetIssuerInfo(issuerUnit);
            if(bIssuer == null){
                return null;
            }
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            //获得密钥对
            KeyPair keyPair = kpg.generateKeyPair();
            //证书有效期
            CertificateValidity interval = new CertificateValidity(dateStart, dateEnd);

            BigInteger sn = new BigInteger(64, new SecureRandom());
            //使用人信息
            String sCertOwner = GetDefaultCertOwnerInfo();
            sCertOwner = sCertOwner.replace("ccn", sDN);
            sCertOwner = sCertOwner.replace("oou", sOU);
            sCertOwner = sCertOwner.replace("oox", sO);
            sCertOwner = sCertOwner.replace("ool", sL);
            sCertOwner = sCertOwner.replace("ooS", sS);
            sCertOwner = sCertOwner.replace("ooC", sC);

            X500Name owner = new X500Name(sCertOwner);
            //证书信息对象
            X509CertInfo info = new X509CertInfo();
            //有效期
            info.set(X509CertInfo.VALIDITY, interval);
            //
            info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
            //证书所有人
            info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
            //证书颁发者
            info.set(X509CertInfo.ISSUER, new CertificateIssuerName(bIssuer));
            //公钥
            info.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
            //版本
            info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            //算法
            AlgorithmId algo = new AlgorithmId(AlgorithmId.sha1WithRSAEncryption_oid);
            info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

            X509CertImpl cert = new X509CertImpl(info);
            cert.sign(issuer_PrivateKey, algorithm);


            KeyStore store = KeyStore.getInstance("PKCS12");
            //System.out.println(sNewPfxPath);
            store.load(null, null);
            store.setKeyEntry("esspfx", keyPair.getPrivate(), sPwd.toCharArray(), new Certificate[] { cert });

            //生成pfx证书
            String pfxPath =Constant.PFX_FILE_PATH+getUUID()+".pfx";
            FileOutputStream fos =new FileOutputStream(pfxPath);
            store.store(fos, sPwd.toCharArray());
            fos.close();
            //生成cer证书
            BASE64Encoder encoder = new BASE64Encoder();
            String cerBase64 = encoder.encode(cert.getEncoded());
            String pfxBase64 = Base64Utils.encodeBase64File(pfxPath);

            Map<String, String> CerAndPfxMap =  new HashMap<String, String>();
            CerAndPfxMap.put("pfxBase64", pfxBase64);
            CerAndPfxMap.put("cerBase64", cerBase64);

            return CerAndPfxMap;
        } catch (Exception e1) {
            System.out.println(e1.toString());
        }
        return  null;
    }

    /**
     * 获取私钥别名等信息
     */
    public static String getPrivateKeyInfo(String privKeyFileString,String privKeyPswdString)
    {
//        String privKeyFileString = Conf_Info.PrivatePath;
//        String privKeyPswdString = "" + Conf_Info.password;
        String keyAlias = null;
        try
        {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream fileInputStream = new FileInputStream(privKeyFileString);
            char[] nPassword = null;
            if ((privKeyPswdString == null) || privKeyPswdString.trim().equals(""))
            {
                nPassword = null;
            } else
            {
                nPassword = privKeyPswdString.toCharArray();
            }
            keyStore.load(fileInputStream, nPassword);
            fileInputStream.close();
            System.out.println("keystore type=" + keyStore.getType());

            Enumeration<String> enumeration = keyStore.aliases();

            if (enumeration.hasMoreElements())
            {
                keyAlias = (String) enumeration.nextElement();
                System.out.println("alias=[" + keyAlias + "]");
            }
            System.out.println("is key entry=" + keyStore.isKeyEntry(keyAlias));
            PrivateKey prikey = (PrivateKey) keyStore.getKey(keyAlias, nPassword);
            Certificate cert = keyStore.getCertificate(keyAlias);
            PublicKey pubkey = cert.getPublicKey();
            System.out.println("cert class = " + cert.getClass().getName());
            System.out.println("cert = " + cert);
            System.out.println("public key = " + pubkey);
            System.out.println("private key = " + prikey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keyAlias;
    }

    public static void main(String[] args) throws Exception {

//        IssuerUnit issuerUnit = new IssuerUnit();
//        issuerUnit.setIssuerUnitPfx("E:\\temp\\issuerUnit\\002\\root.pfx");
//        issuerUnit.setIssuerUnitRoot("E:\\temp\\issuerUnit\\002\\root.cer");
//        issuerUnit.setPfxPwd("111111");
//        Map<String, String> a= CreatePfxFile("中国","江苏省","南京市","烟草","办公司",
//                "陈坤",new Date(),new Date(),"111111",
//                "SHA1withRSA",issuerUnit);

//        FileWriter fw = new FileWriter("E:\\temp\\demo1.pfx");
//        assert a != null;
//        fw.write(ESSGetBase64Decode(a.get("pfxBase64")));
//        fw.close();

        String baseString  ="MIIHgAIBAzCCBzoGCSqGSIb3DQEHAaCCBysEggcnMIIHIzCCAyAGCSqGSIb3DQEHAaCCAxEEggMN\n" +
                "MIIDCTCCAwUGCyqGSIb3DQEMCgECoIICsjCCAq4wKAYKKoZIhvcNAQwBAzAaBBTwurKD2O88yp36\n" +
                "Lie5hnD3TU18uAICBAAEggKAPbw7Us94wc1tmnbQo2SlQpJFdlzogUTWT2BnjojH77y/87dQfpiV\n" +
                "3FS98dVU7RupFLArpTGMr1J+xvMH13XKQHybXUp56HdzuB9LFFq3AC/F5xvsv2vtY/8Sej2kQYPz\n" +
                "RzgKRCwvGXu9r2kIavUpKqbKBuHNlKk/dbm3/khzu7I+SrYurv38fGaEaSqjn9c/K+MUV72dqnJP\n" +
                "fbNX78+UplScLNNA6oDU7dM/KhOb3dsOnJM26MNdwf/ooMfjL7mMq90etYJ90n+jP4L4Boy+vCvX\n" +
                "Sjauz8XAy95VBm0DRZwlxJXAUg64nnecbaoO3rtFa5hTVGC2Ef1+B7dYSf+pkYNuLminu4QF+9Tf\n" +
                "SMQg+9CYfErjtkIXOscZPNgrlTUNCu342YRx2XBKra2UGS2u4gHOfFwTQLxpOV+ipm81DFoD22iH\n" +
                "b4bP6ChFSjeMCgA42YyPp7ZQ2wZAg2zepI7Ilke7kSt8w+R8aYGFfM1lIUlEVxP632YibsUvTJUo\n" +
                "FrBH+jGOvNsLHiQmqwatXmKToDXPifgst29uG4Q9FCkUDxB5dLs8DEqDKw40Xkk9iq3biUiWDPgj\n" +
                "SLRJuXYyCP7XFmYPaJgPOEapS+N+e/knzjJgjGnNks3S3zoENoDiWhRyS3tsfZTnzj0h9VWRHj+9\n" +
                "6mQjU9KMkD4eUezZlMmTvR//l2yxF3p8OIz4kJtgwaMMct30/72zyhbTq4mshpSNf7n+rsFi3Uz2\n" +
                "L9EK8PiXYHTUZ9Qw2nitCSLq5OfotmPcbRnU6giDA42YdN2I7EBiqSe/Yc/ot3ZGM95vF27vhped\n" +
                "/SGzu47npaxmjYb+fjzyCZhz6bPG44EaSW5WaapEXTFAMBsGCSqGSIb3DQEJFDEOHgwAZQBzAHMA\n" +
                "cABmAHgwIQYJKoZIhvcNAQkVMRQEElRpbWUgMTUzNjg5Mjc4MzUwNTCCA/sGCSqGSIb3DQEHBqCC\n" +
                "A+wwggPoAgEAMIID4QYJKoZIhvcNAQcBMCgGCiqGSIb3DQEMAQYwGgQUoKEhpVORoaUfEvedkUmO\n" +
                "/Fa6CLICAgQAgIIDqBwCWuDefhKOx2L05V3/rDidsxsH4Gls2oZpwkPvyXfLv58T55ovqgsHWFLE\n" +
                "1ChPOxB/hfbT7MhbpfF+7zdISNR9+dtrP8F5zSWUk+KaPo28BkNQu25FaVQRdqTJuKKv9Z6GRhJs\n" +
                "j1cpcYQ6iJgPjeWbGsaZHUpUe339dOnZMHQPbGVuwX77Spy2N798Sy0tJtYzXCLHNt5tkFi6Wdtx\n" +
                "QGkSamS2tv4oPDRNmZoiSP4po/CdChXUkkyodH5MQkTEouTpJJMyT2pmcboavO2iFSm+VErFNa53\n" +
                "b+hqB8VxitDHgZQE2kIxGpcsPKx8jGPUk3ue2DUYeJ4V74aSs8dAXFb3bzTYsCfrMB2GVqGlAaRn\n" +
                "XVEzCB5wpC+BPUGnoA1rRW9gTpNGQCEYvXl72AOx7YTQJcsLJ9Hl/eB4KrTISucDEBukXbsJ64Gc\n" +
                "IXPD/LBo70rZmXROuUa9prng1BWVEveW3tP3Nvo0DJrYIKPLzAOoKsAKwKpeNIaat6bp+YhX8xMW\n" +
                "8J0i8tVQoAN9hFGx51h3gh3uD68+0CYRfHy37+uWrR6UiFVZV/WhogS1XrkvBQyt9d8V7G6XDXzO\n" +
                "ukCseC6FI2GEQHJFYnNIAGMTH7hll+Uqphqnck1uCjOd67IK4oBhYETCQ3Ws/ky8QBF+miIiess+\n" +
                "yQYCEDkQyl9fO03RCTVXh/HU5W4n/f3x3/rz0t67sjkYtXkwrBSBV9obu0XxoOxd6RHq/ql7d9qv\n" +
                "28SWrERoUclbdSd1NMdJm1tbkALS2j/EaTZzpqlOMOfRaklwr6Xwr/vdowWDvS+/K+53nf/o4Bkh\n" +
                "hOlPNeZYeJKFQCcf334NDIGLY2GNE/ajxYbBOyQiflN6EF7M2VIw9+dtgQzCliR3Iu8ogcML3D7n\n" +
                "QmX7CqsvQ8W0b+bMmjqUCgkGo6ZSQtf0VzjyuxMo7C2CoExlEqm4U/x+BwA9jhvg21AppZrOPj3W\n" +
                "O4YFgrQKMPGKqTHTEbi4CI/T/MZk5YSW2um1iBj7oHplIGzmYrQTyt4yl3PD86ilqK/PDyjcksA6\n" +
                "C1yjVkBjfQpqcb2JBFeP8SDWaB/gVbKcZzaYeoJHAqAUw5zjXzqBY/xke9MnbqgJPQQbEye25K5h\n" +
                "/RMsLk7bZS3xgZjp98Dm23N1cXqJw+KMDLvzAK5Z1oZpL9LLRaokmiJ8ZsDQlYkXzLgtDVFC84NR\n" +
                "+0N8zrwLCLxQW6BbdyH4FdsX/JMnf7OmVJOLcc4rQEByjZiYWjA9MCEwCQYFKw4DAhoFAAQUG5ex\n" +
                "46+5kiSpXJzERRzYeqoO2WQEFNE1bipH2D8WKwgyfmLF4nFZ+LL7AgIEAA==";
        byte2File(ESSGetBase64Decode(baseString),"E:\\temp\\","demo1.pfx");
        System.out.println(getDecryptPwd("AAEIAAAAAAAAAAgAAAAAAAAAQ2Grfi9clooBAA=="));

//        //获取颁发者证书私钥
//        PrivateKey issuer_PrivateKey = GetPrivateKeyByCert("D:\\temp\\root.pfx");
//        //读取证书文件
//        String base = "MIIDwTCCAyqgAwIBAgIBADANBgkqhkiG9w0BAQUFADCBtTENMAsGA1UEBh4ETi1W/TENMAsGA1UECB4EW4lfvTENMAsGA1UEBx4EgpxuVjElMCMGA1UECh4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UECx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UEAx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDERMA8GCSqGSIb3DQEJAR4CACAwHhcNMTcxMjA2MDEzNzAxWhcNMjcxMjA0MDEzNzAxWjCBtTENMAsGA1UEBh4ETi1W/TENMAsGA1UECB4EW4lfvTENMAsGA1UEBx4EgpxuVjElMCMGA1UECh4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UECx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UEAx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDERMA8GCSqGSIb3DQEJAR4CACAwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANGfGJR9qo+4x0XS8P5qQSBMO736zuOAun/7C9BGzMTkXx737ic8vZy012PFiniTMf3JpcktuIMKIw25KlJoO2fLny20pv6kvl/mVwduHDsRw1BhrzgaRkKcoPhIDQIu0RLpdikccISMhLiY1GuVFX5SGTg538kJ2OPR/kjAWasJAgMBAAGjgd4wgdswJAYKKwYBBAGpQ2QCBQQWFhRJRDEzMDIwMzE5NzcwMzA2MDYxODAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIBBjCBlwYDVR0lBIGPMIGMBggrBgEFBQcDAwYIKwYBBQUHAwEGCCsGAQUFBwMEBggrBgEFBQcDAgYIKwYBBQUHAwgGCisGAQQBgjcCARYGCisGAQQBgjcKAwEGCisGAQQBgjcKAwMGCisGAQQBgjcKAwQGCisGAQQBgjcUAgIGCCsGAQUFBwMFBggrBgEFBQcDBgYIKwYBBQUHAwcwDQYJKoZIhvcNAQEFBQADgYEALCNCtkrpZKUjm7vSwmSWq0oWEM6L0Lknu58G+PtaMJWeafADZrTToO3P8qNDq7t61Ai85hEwPX2pH6qAwHswgpO31Lz5Jq43JaN+FOAwufHmpAyjOtLsSRsTG9BL7SALGIlX7LkJBHuZMflbwJ4v0wHqoY0iTre3xkvishdL9ng=";
//        byte[] cerByte = ESSGetBase64Decode("MIIDwTCCAyqgAwIBAgIBADANBgkqhkiG9w0BAQUFADCBtTENMAsGA1UEBh4ETi1W/TENMAsGA1UECB4EW4lfvTENMAsGA1UEBx4EgpxuVjElMCMGA1UECh4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UECx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UEAx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDERMA8GCSqGSIb3DQEJAR4CACAwHhcNMTcxMjA2MDEzNzAxWhcNMjcxMjA0MDEzNzAxWjCBtTENMAsGA1UEBh4ETi1W/TENMAsGA1UECB4EW4lfvTENMAsGA1UEBx4EgpxuVjElMCMGA1UECh4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UECx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDElMCMGA1UEAx4cgpxuVl4CTrpSm41EbpBUjHk+TxpP3ZacXEAAIDERMA8GCSqGSIb3DQEJAR4CACAwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANGfGJR9qo+4x0XS8P5qQSBMO736zuOAun/7C9BGzMTkXx737ic8vZy012PFiniTMf3JpcktuIMKIw25KlJoO2fLny20pv6kvl/mVwduHDsRw1BhrzgaRkKcoPhIDQIu0RLpdikccISMhLiY1GuVFX5SGTg538kJ2OPR/kjAWasJAgMBAAGjgd4wgdswJAYKKwYBBAGpQ2QCBQQWFhRJRDEzMDIwMzE5NzcwMzA2MDYxODAMBgNVHRMEBTADAQH/MAsGA1UdDwQEAwIBBjCBlwYDVR0lBIGPMIGMBggrBgEFBQcDAwYIKwYBBQUHAwEGCCsGAQUFBwMEBggrBgEFBQcDAgYIKwYBBQUHAwgGCisGAQQBgjcCARYGCisGAQQBgjcKAwEGCisGAQQBgjcKAwMGCisGAQQBgjcKAwQGCisGAQQBgjcUAgIGCCsGAQUFBwMFBggrBgEFBQcDBgYIKwYBBQUHAwcwDQYJKoZIhvcNAQEFBQADgYEALCNCtkrpZKUjm7vSwmSWq0oWEM6L0Lknu58G+PtaMJWeafADZrTToO3P8qNDq7t61Ai85hEwPX2pH6qAwHswgpO31Lz5Jq43JaN+FOAwufHmpAyjOtLsSRsTG9BL7SALGIlX7LkJBHuZMflbwJ4v0wHqoY0iTre3xkvishdL9ng=");
//        singCertByPrivateKey(cerByte,issuer_PrivateKey);

        //2.执行签名
//        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(issuer_PrivateKey.getEncoded());
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
//
//        Signature signature = Signature.getInstance("MD5withRSA");
//        signature.initSign(issuer_PrivateKey);
//        signature.update(cerByte);
//        byte[] result = signature.sign();
//
//        //生成cer证书
//        BASE64Encoder encoder = new BASE64Encoder();
//        String cerBase64 = encoder.encode(result);
//
//        FileWriter fw = new FileWriter("D:\\temp\\demo1.cer");
//        fw.write(cerBase64);
//        fw.close();
    }



}
