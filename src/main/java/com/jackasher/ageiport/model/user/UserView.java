package com.jackasher.ageiport.model.user;

import com.alibaba.ageiport.processor.core.annotation.ViewField;


public class UserView {
    
    @ViewField(headerName = "用户ID")
    private Long id;
    
    @ViewField(headerName = "用户名")
    private String username;
    
    @ViewField(headerName = "账号")
    private String userAccount;

    @ViewField(headerName = "头像URL")
    private String avatarUrl;
    
    @ViewField(headerName = "性别")
    private String genderText; // 转换后的性别文本
    
    @ViewField(headerName = "手机号")
    private String phone;
    
    @ViewField(headerName = "邮箱")
    private String email;
    
    @ViewField(headerName = "用户状态")
    private String userStatusText; // 转换后的状态文本
    
    @ViewField(headerName = "创建时间")
    private String createTime; // 格式化后的时间
    
    @ViewField(headerName = "用户角色")
    private String userRoleText; // 转换后的角色文本
    
    @ViewField(headerName = "标签")
    private String tags;
    
    @ViewField(headerName = "个人简介")
    private String profile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGenderText() {
        return genderText;
    }

    public void setGenderText(String genderText) {
        this.genderText = genderText;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserStatusText() {
        return userStatusText;
    }

    public void setUserStatusText(String userStatusText) {
        this.userStatusText = userStatusText;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUserRoleText() {
        return userRoleText;
    }

    public void setUserRoleText(String userRoleText) {
        this.userRoleText = userRoleText;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}