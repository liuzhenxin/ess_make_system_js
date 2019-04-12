package com.clt.ess.utils.HTTP;

public class SealData {

    private String certBase64 ;
    private String imgBase64 ;
    private String sealName ;

    public String getCertBase64() {
        return certBase64;
    }

    public void setCertBase64(String certBase64) {
        this.certBase64 = certBase64;
    }

    public String getImgBase64() {
        return imgBase64;
    }

    public void setImgBase64(String imgBase64) {
        this.imgBase64 = imgBase64;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    @Override
    public String toString() {
        return "SealData{" +
                "certBase64='" + certBase64 + '\'' +
                ", imgBase64='" + imgBase64 + '\'' +
                ", sealName='" + sealName + '\'' +
                '}';
    }
}
