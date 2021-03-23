package com.levin.commons.service.support;

import com.levin.commons.service.domain.InjectVar;
import com.levin.commons.utils.MapUtils;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SimpleVariableInjectorTest {


    @Data
    @Accessors(chain = true)
    class Org {

        String id;

        String name;

        Org parent;
    }

    @Data
    @Accessors(chain = true)
    class User {

        boolean isAdmin = false;

        String id;

        String name;

        Org org;
    }

    @Data
    class Dto {

        @InjectVar(InjectConsts.USER_ID)
        String userId;

        @InjectVar(InjectConsts.ORG_ID)
        String orgId;

        @InjectVar(InjectConsts.ORG)
        Object org;

        @InjectVar(InjectConsts.USER)
        Object user;

        /**
         * 想要操作的目标组织 ID
         * 这个值必须存在
         * <p>
         * 可以本部门，也可以是下级部门，其它部门不允许
         * <p>
         * 如果是超级管理员，可以操作任意部门
         */
        @InjectVar(value = InjectConsts.ORG_ID
                , isOverride = "!#user.isAdmin" // 如果不是超级管理员
                , exprPrefix = InjectVar.SPEL_PREFIX)
        String targetOrgId;


        /**
         * 想要统计目标组织 ID
         * <p>
         * 可以本部门，也可以是下级部门
         * <p>
         * 如果是超级管理员，可以是任意部门，或是不指定，统计全部的部门
         */
        @InjectVar(value = InjectConsts.ORG_ID
                , isOverride = "!#user.isAdmin" // 如果不是超级管理员
                , isRequired = "!#user.isAdmin" // 如果不是超级管理员，那么值是必须的
                , exprPrefix = InjectVar.SPEL_PREFIX)
        String statTargetOrgId;


        @InjectVar(value = InjectConsts.IP_ADDR, isRequired = "false")
        String ipAddr;

    }

    Org 公司 = new Org().setId("1").setName("XXX科技有限公司");

    Org 研发部 = new Org().setId("1-1").setParent(公司).setName("研发部");
    Org 研发一部 = new Org().setId("1-1-1").setParent(研发部).setName("研发1部");
    Org 研发二部 = new Org().setId("1-1-2").setParent(研发部).setName("研发2部");

    Org 销售部 = new Org().setId("1-2").setParent(公司).setName("销售部");


    User 超级管理员 = new User().setAdmin(true).setId("1").setName("超级管理员").setOrg(公司);
    User 普通管理员 = new User().setAdmin(false).setId("2").setName("管理员").setOrg(公司);

    User 张三 = new User().setId("2").setName("张三（程序员）").setOrg(研发部);

    User 李四 = new User().setId("2").setName("李四（销售）").setOrg(销售部);


    @Test
    void getVariableResolvers() {

//        Map<String, Object> ctx = MapUtils.put(InjectConsts.USER, (Object) user)
//                .put(InjectConsts.ORG, user.getOrg()).build();


    }

    @Test
    void getSimpleVariableResolver() {
    }

    @Test
    void getSpelVariableResolver() {

    }

    @Test
    void getGroovyVariableResolver() {
    }

    @Test
    void inject() {

        // SimpleVariableInjector.defaultSimpleVariableInjector.

    }

    @Test
    void injectByVariableResolver() {
    }


}