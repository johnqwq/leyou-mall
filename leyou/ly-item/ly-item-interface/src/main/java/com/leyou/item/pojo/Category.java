package com.leyou.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "tb_category")
@Data
public class Category {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private String name;
    private Long parentId; // 这个和下面一个的属性名和数据库里的不完全一致，数据填充到对象里的时候是怎么识别的呢？待解决
    private Boolean isParent; // 这里的类型记得一定要写包装类，否则查询到的数据会少一个字段无法填充(后面还导致我的tree莫名其妙没法加载子级)
    private Integer sort;
}
