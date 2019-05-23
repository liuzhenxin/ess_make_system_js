package com.clt.ess.utils.HTTP;

public class SealData {

    private String certBase64 ;
    private String imgBase64 ;
    private String sealName ;
    private String unitId ;
    private String IdNum ;
    private String city ;
    private String unitName ;
    private String departName ;
    private String certName ;

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

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getIdNum() {
        return IdNum;
    }

    public void setIdNum(String idNum) {
        IdNum = idNum;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getDepartName() {
        return departName;
    }

    public void setDepartName(String departName) {
        this.departName = departName;
    }

    public String getCertName() {
        return certName;
    }

    public void setCertName(String certName) {
        this.certName = certName;
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
