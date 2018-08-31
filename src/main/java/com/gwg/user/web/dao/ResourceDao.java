package com.gwg.user.web.dao;



import com.gwg.user.web.exception.BusinessException;
import com.gwg.user.web.model.Resource;

import java.util.List;

public interface ResourceDao {


    public List<Resource> queryAllResources() throws BusinessException;

    public List<Resource> queryResourceListByIds(List<Long> idList) throws BusinessException;

    /**
     * 获取当前登陆用户菜单
     * @return
     * @throws BusinessException
     */
    public List<Resource> queryCurrentUserMenu(String userId) throws BusinessException;


}
