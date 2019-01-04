package com.ant.app.service.impl;

import com.ant.app.Constants;
import com.ant.app.dao.AdminDao;
import com.ant.app.dao.SysLogDao;
import com.ant.app.dao.UserDao;
import com.ant.app.entity.AppWebResult;
import com.ant.app.entity.LayUiResult;
import com.ant.app.entity.req.LayUiAuToReq;
import com.ant.app.entity.req.UserLogin;
import com.ant.app.entity.tree.TreNode;
import com.ant.app.exception.AddUserException;
import com.ant.app.model.SaleUser;
import com.ant.app.model.SysAdmin;
import com.ant.app.model.SysLog;
import com.ant.app.service.UserService;
import com.ant.app.utils.TreeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author lchunlei
 * @date 2018/12/12
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Resource
    UserDao userDao;
    @Autowired
    SysLogDao sysLogDao;
    @Autowired
    AdminDao adminDao;

    @Override
    public void appLogin(UserLogin login, AppWebResult<Integer> result) {
        log.info("用户登陆---》"+login);
        SaleUser user = userDao.selectUserByPhoneNum(login.getPhoneNum());
        if(user==null){
            result.setFail(Constants.USER_NULL);
        }else if(!user.getFirstPwd().equals(login.getFirstPwd())){
            result.setFail(Constants.PWD_ERR);
        }else if(!user.getUserStatus().equals(0)){
            result.setFail(Constants.USER_ICE);
        }else {
            if(user.getFirstPwd().equals(Constants.PWD)){
                result.setResultCode(Constants.RE_SET_PWD);
            }
            result.setData(user.getUserId());
        }
    }

    @Override
    public void findUsers(LayUiAuToReq req, LayUiResult<SaleUser> result) {
        req.setStartNum((req.getPage()-1)*10);
        Integer totallNumAll = userDao.selectTotalNum(req);
        if(totallNumAll>0){
            result.setCode(0);
            result.setMsg("成功");
            result.setCount(totallNumAll);
            result.setData(userDao.selectByPage(req));
        }else{
            result.setCode(Constants.PAGE_ERROR_CODE);
            result.setMsg(Constants.NOT_MORE_INFO);
        }
    }

    @Override
    @Transactional
    public void updaUserUse(Integer userId, Integer nowAdminId, AppWebResult result) {
        SaleUser saleUser = userDao.selectUserById(userId);
        if(saleUser==null){
            result.setFail(Constants.NULL_DATA);
        }else {
            Integer use = 0;
            if(saleUser.getUserStatus().equals(0)){
                use=1;
            }
            Integer s = userDao.updateUserStatus(use,userId);
            if(!s.equals(1)){
                result.setFail(Constants.ERR_MSG);
            }else {
                SysLog sysLog = new SysLog(nowAdminId,"updaUserUse_"+userId);
                sysLogDao.insertSysLog(sysLog);
            }
        }
    }

    //添加报单中心
    @Override
    @Transactional
    public void addUserCore(SaleUser user, Integer nowAdminId, AppWebResult result) {
        SaleUser oldUser = userDao.selectUserByPhoneNum(user.getPhoneNum());
        int t ;
        if(oldUser==null){
            user.setRegisteCore(1);
            t=userDao.insertUser(user);
        }else {
            t=userDao.upUserToCore(user.getRegisteCoreMoney());
        }
        if(t==0){
            result.setFail(Constants.ERR_MSG);
        }
    }

    /**
     * 添加下线用户
     * 用户实体，报单中心ID
     */
    @Override
    @Transactional
    public void addSaleUser(SaleUser user, Integer userId, AppWebResult result) {
        user.setFirstPwd(Constants.PWD);
        user.setSecondPwd(Constants.PWD);
        user.setThirdPwd(Constants.PWD_LAST);
        if(user.getJoinMoney()==null){
            user.setJoinMoney(Constants.JOIN_MONEY);
        }
        SaleUser loginUser = userDao.selectUserById(userId);
        SaleUser refereeUser = userDao.selectUserByPhoneNum(user.getRefereePhoneNum());
        if(refereeUser==null){
            result.setFail(Constants.REFORE_USER_NULL);
            return;
        }else {
            user.setRefereeId(refereeUser.getUserId());
        }
        SaleUser oldUser = userDao.selectUserByPhoneNum(user.getPhoneNum());
        SaleUser preTreeSup=refereeUser;
        user.setPuserId(userId);//赋值报单中心ID
        if(loginUser==null||loginUser.getUserStatus().equals(1)){
            result.setFail(Constants.NULL_DATA);
        }else if(loginUser.getRegisteCore().equals(0)){
            result.setFail(Constants.AUTH_LESS);
        }else if(oldUser!=null){
            result.setFail(Constants.PHONE_HAD);
        }else if(refereeUser.getTreeLeft()==null){
            user.setTreeSupId(refereeUser.getUserId());
            userDao.insertUser(user);
            userDao.upUserLeft(userDao.selectUserByPhoneNum(user.getPhoneNum()).getUserId(),user.getRefereeId());
        }else if(refereeUser.getTreeRight()==null){
            user.setTreeSupId(refereeUser.getUserId());
            userDao.insertUser(user);
            userDao.upUserRight(userDao.selectUserByPhoneNum(user.getPhoneNum()).getUserId(),user.getRefereeId());
        }else {
            int floorTotal = findFloorTotal(user.getRefereeId());
            log.info("用户--"+user.getRefereeId()+"--下线层数--》"+floorTotal);
            List<String> strs = TreeUtil.treeList(floorTotal);
            Integer minFloor = 10000000;
            String minStr = null;

            //判断最短的树枝线
            for(String s:strs){
                char[] chars = s.toCharArray();
                SaleUser u =refereeUser;
                int i = 0;
                for(char c:chars){
                    i++;
                    Integer sunId;
                    if(c=='0'){
                        sunId=u.getTreeLeft();
                    }else {
                        sunId=u.getTreeRight();
                    }
                   if(sunId==null){
                        if(i<minFloor){
                            minFloor=i;
                            minStr=s;
                            break;
                        }
                   }else {
                       u=userDao.selectUserById(sunId);
                   }

                }
            }
            log.info("树枝总数---》"+strs);
            log.info("最短层数---》"+minFloor);
            log.info("最短树枝---》"+minStr);
            //定位最短的树枝挂
            char[] minTree = minStr.toCharArray();
            for(char c:minTree){
                minFloor--;
                Integer sunId;
                if(c=='0'){
                    sunId=preTreeSup.getTreeLeft();
                }else {
                    sunId=preTreeSup.getTreeRight();
                }
                preTreeSup=userDao.selectUserById(sunId);
               if(minFloor==1){
                   break;
               }
            }
            log.info("预父树---》"+preTreeSup);
            user.setTreeSupId(preTreeSup.getUserId());
            userDao.insertUser(user);
            if(preTreeSup.getTreeRight()==null){
                userDao.upUserRight(userDao.selectUserByPhoneNum(user.getPhoneNum()).getUserId(),preTreeSup.getUserId());
            }else if(preTreeSup.getTreeLeft()==null){
                userDao.upUserLeft(userDao.selectUserByPhoneNum(user.getPhoneNum()).getUserId(),preTreeSup.getUserId());
            }else {
                throw new AddUserException();
            }
        }

    }

    @Override
    public void changePwd(SaleUser user, Integer nowUserId, AppWebResult result) {
        user.setUserId(nowUserId);
        SaleUser oldUser = userDao.selectUserById(nowUserId);
        if(oldUser.getFirstPwd().equals(user.getOldFirstPwd())){
            Integer i = userDao.updateUserPwd(user);
            if(!i.equals(1)){
                result.setFail(Constants.ERR_MSG);
            }
        }else {
            result.setFail(Constants.PWD_ERR);
        }
    }

    @Override
    public void userInfo(Integer userId, AppWebResult<SaleUser> result) {
        SaleUser user = userDao.selectUserById(userId);
        if(user==null){
            result.setFail(Constants.NULL_DATA);
        }else {
            result.setData(user);
        }
    }

    //查某个人的下线层数
    private Integer findFloorTotal(Integer userId){
        int i=1;
        SaleUser ur = userDao.selectUserById(userId);
        while (ur.getTreeRight()!=null){
            i++;
            ur = userDao.selectUserById(ur.getTreeRight());
        }
        return i;
    }
}
