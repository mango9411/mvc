package com.mango.edu.demo.service.impl;

import com.mango.edu.demo.service.DemoService;
import com.mango.edu.mvcframework.annotations.MangoService;

/**
 * @author mango
 * @date 2021/1/11 21:12
 * @description:
 */
@MangoService
public class DemoServiceImpl implements DemoService {

    @Override
    public String get(String name) {
        System.out.println("service 实现类中的name参数: " + name);
        return name;
    }
}
