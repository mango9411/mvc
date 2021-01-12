package com.mango.edu.mvcframework.pojo;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author mango
 * @date 2021/1/12 15:23
 * @description: 封装handler方法的相关信息
 */
public class Handler {

    private Object controller;

    private Method method;

    private Pattern pattern;

    /**
     * key   参数名
     * value 第几个参数
     */
    private Map<String, Integer> params;

    public Handler(Object controller, Method method, Pattern pattern){
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
        this.params = new ConcurrentHashMap<>();
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParams() {
        return params;
    }

    public void setParams(Map<String, Integer> params) {
        this.params = params;
    }
}
