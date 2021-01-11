package com.mango.edu.mvcframework.servlet;

import com.mango.edu.mvcframework.annotations.MangoController;
import com.mango.edu.mvcframework.annotations.MangoService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mango
 * @date 2021/1/11 20:33
 * @description: 自定义DispatcherServlet
 */
public class MangoDispatcherServlet extends HttpServlet {

    /**
     * 加载的配置文件内容
     */
    private Properties properties = new Properties();

    /**
     * 缓存扫描到的className
     */
    private List<String> clasNames = new ArrayList<>();

    /**
     * 作为IoC容器
     */
    private Map<String, Object> iocMap = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // TODO: 2021/1/11 处理请求
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //  1.加载配置文件 springmvc.properties
        String configLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(configLocation);
        //  2.扫描类
        doScanAnnotations(properties.getProperty("scanPackage"));
        //  3.初始化bean对象(实现IoC容器)
        doInstance();
        //  4.实现依赖注入
        doAutowired();
        //  5.构造一个HandlerMapping(处理器映射器, 将配置好的url和method建立映射关系)
        doInitHandlerMapping();

        System.out.println("mango mvc 初始化完成...");
        //  6.等待请求进入，处理请求
    }

    /**
     * 构造一个HandlerMapping(处理器映射器, 将配置好的url和method建立映射关系)
     */
    private void doInitHandlerMapping() {
    }

    /**
     * 依赖注入
     */
    private void doAutowired() {
    }

    /**
     * 初始化bean对象 IoC容器
     */
    private void doInstance() {
        if (clasNames.size() == 0) {
            return;
        }
        for (String className : clasNames) {
            try {
                // className = com.mango.edu.demo.controller.DemoController
                Class<?> aClass = Class.forName(className);
                if (aClass.isAnnotationPresent(MangoController.class)) {
                    //首字母小写作为ID
                    putController(aClass);
                } else if (aClass.isAnnotationPresent(MangoService.class)) {
                    putService(aClass);
                } else {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private void putService(Class<?> aClass) throws InstantiationException, IllegalAccessException {
        MangoService service = aClass.getAnnotation(MangoService.class);
        String value = service.value();
        if ("".equals(value.trim())) {
            //没有指定ID 类名首字母
            value = lowerFirst(aClass.getSimpleName());
        }
        iocMap.put(value, aClass.newInstance());
        //方便注入  将接口名也放入ioc容器
        Class<?>[] interfaces = aClass.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            iocMap.put(anInterface.getName(), aClass.newInstance());
        }
    }

    private void putController(Class<?> aClass) throws InstantiationException, IllegalAccessException {
        String simpleName = aClass.getSimpleName();
        String lowerFirstName = lowerFirst(simpleName);
        Object o = aClass.newInstance();
        iocMap.put(lowerFirstName, o);
    }

    /**
     * 扫描类  扫描注解
     *
     * @param packagePath 包路径
     */
    private void doScanAnnotations(String packagePath) {
        //磁盘路径
        String path = Thread.currentThread()
                .getContextClassLoader()
                .getResource("")
                .getPath() + packagePath.replaceAll("\\.", "/");
        File file = new File(path);
        //是否多个文件或文件夹
        File[] files = file.listFiles();
        for (File file1 : files) {
            if (file.isDirectory()) {
                //如果是目录 进行递归
                doScanAnnotations(path + "." + file1.getName());
            } else if (file1.getName().endsWith(".class")) {
                //不是目录  那就是class文件  替换掉后缀 拿到className
                String className = path + "." + file1.getName().replaceAll(".class", "");
                clasNames.add(className);
            }
        }
    }

    /**
     * 加载配置文件
     *
     * @param configLocation
     */
    private void doLoadConfig(String configLocation) {
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream(configLocation));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 首字母小写
     *
     * @param name
     * @return
     */
    public String lowerFirst(String name) {
        char[] chars = name.toCharArray();
        if ('A' <= chars[0] && chars[0] <= 'Z') {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }
}
