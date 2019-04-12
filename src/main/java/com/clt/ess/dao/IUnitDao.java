package com.clt.ess.dao;


import com.clt.ess.entity.Unit;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface IUnitDao {

    Unit findUnitByUnitId(String unitId);

    /**
     * 递归查询所有的子单位
     * @param parentUnitId 父单位
     * @return unit集合
     */
    List<Unit> findUnitByParentUnitId(String parentUnitId);

    String findBusinessUnitId(Map<String, String> map);

    List<Unit> findUnitByOrgId(@Param("OrgID")String OrgID, @Param("businessSysId")String businessSysId);
}
