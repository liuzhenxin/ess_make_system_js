package com.clt.ess.dao;

import com.clt.ess.entity.Certificate;

import java.util.List;

public interface ICertificateDao {
    /**
     * 添加证书记录
     * @param certificate 证书对象
     * @return 影响行数
     */
    int addCertificate(Certificate certificate);

    /**
     * 更新证书记录
     * @param certificate 证书对象
     * @return 影响行数
     */
    int updateCertificate(Certificate certificate);

    /**
     * 删除证书记录
     * @param certificate 证书对象
     * @return 影响行数
     */
    int deleteCertificate(Certificate certificate);

    Certificate selectCertificate(Certificate certificate);

    List<Certificate> selectCertificateList(Certificate certificate);

    /**
     * 查找证书列表
     * @param certificate 证书对象
     * @return 符合条件的证书列表
     */
    List<Certificate> findCertificate(Certificate certificate);

    /**
     * 根据证书id查找证书对象
     * @param certId 证书id
     * @return 证书对象
     */
    Certificate findCertificateById(String certId);

    /**
     * 根据证书hash查找证书对象
     * @param certHash 证书hash
     * @return 证书对象
     */
    Certificate findCertificateByCertHash(String certHash);
}
