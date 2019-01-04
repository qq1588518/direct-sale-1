package com.ant.app.controller.pc;

import com.ant.app.Constants;
import com.ant.app.entity.AppWebResult;
import com.ant.app.entity.req.AdminLogin;
import com.ant.app.entity.resp.IndexData;
import com.ant.app.model.SysAdmin;
import com.ant.app.service.AdminService;
import com.ant.app.service.SysService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * @author lchunlei
 * @date 2019/1/4
 */
@RestController
@RequestMapping("/pc/sys")
public class SysController {
    private static final Logger log = LoggerFactory.getLogger(SysController.class);

    @Autowired
    SysService sysService;
    @Autowired
    AdminService adminService;

    /**
     * 初始化首页
     */
    @RequestMapping(value = "/index/init",method = RequestMethod.GET)
    public AppWebResult loginApp(){
        AppWebResult<IndexData> result = new AppWebResult();
        sysService.initPcIndex(result);
        log.info("初始化首页--->"+result);
        return result;
    }

    /**
     * 查看权限
     */
    @RequestMapping(value = "/auth",method = RequestMethod.GET)
    public AppWebResult findAdminAuth(HttpSession session){
        AppWebResult<SysAdmin> re = new AppWebResult();
        AppWebResult<Integer> result = new AppWebResult();
        Object obj = session.getAttribute(Constants.ADMIN_ID);

        if(obj==null){
            result.setResultCode(Constants.NO_LOGIN_CODE);
            result.setResultMsg(Constants.NO_LOGIN);
        }else {
            Integer nowAdminId = (int)obj;
            adminService.findAdmin(nowAdminId,re);
            if(re.getData()!=null){
                result.setData(re.getData().getSysRole());
            }else {
                result.setFail(re.getResultMsg());
            }
        }
        log.info("查看权限--->"+result);
        return result;
    }

}