package com.mango.edu.demo.controller;

import com.mango.edu.demo.service.DemoService;
import com.mango.edu.mvcframework.annotations.MangoAutowired;
import com.mango.edu.mvcframework.annotations.MangoController;
import com.mango.edu.mvcframework.annotations.MangoRequestMapping;
import com.mango.edu.mvcframework.annotations.Security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author mango
 * @date 2021/1/11 21:10
 * @description:
 */
@Security(value = {"mango", "zhangsan"})
@MangoController
@MangoRequestMapping("/demo")
public class DemoController {

    @MangoAutowired
    private DemoService demoService;

    @MangoRequestMapping("/query")
    public String query(HttpServletRequest request, HttpServletResponse response, String name) {
        return demoService.get(name);
    }
}
