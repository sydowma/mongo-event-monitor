package com.sydow.mongoeventmonitor;

import lombok.Data;

import java.util.Date;

@Data
public class UserEntity {
    private Long id;
    private String name;
    private Date createTime;
    private Date modifyTime;
}