package com.gofly.entity;


import lombok.Data;

import java.util.Date;

@Data
public class Table {
    private Date createTime;
    private Date updateTime;
    private String dataRows;
    private String name;
    private String remark;
}
