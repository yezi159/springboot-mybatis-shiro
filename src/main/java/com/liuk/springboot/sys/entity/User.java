package com.liuk.springboot.sys.entity;


import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.liuk.springboot.entity.BaseEntity;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 *
 *
 * @author kl
 * @date 2018/6/14
 */
@Data
@ToString
@TableName("sys_user")
@Accessors(chain = true)
public class User extends BaseEntity{

    private String companyId;

    private String officeId;

    private String loginName;

    @JsonIgnore
    private String password;

    private String no;

    private String name;

    private String email;

    private String phone;

    private String mobile;

    private String photo;

    private String loginIp;

    private Date loginDate;

    private String loginFlag;

    private String qrcode;

    private String sign;

    @TableField(exist = false)
    private Office office;

    @TableField(exist = false)
    private Office company;

    @TableField(exist = false)
    private List<String> roleIds;

    @TableField(exist = false)
    private String confirmPassword;

    public boolean isAdmin(){
        return "1".equals(id);
    }

}
