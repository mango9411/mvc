package com.mango.edu.mvcframework.servlet;

import com.mango.edu.mvcframework.annotations.MangoAutowired;
import com.mango.edu.mvcframework.annotations.MangoController;
import com.mango.edu.mvcframework.annotations.MangoRequestMapping;
import com.mango.edu.mvcframework.annotations.MangoService;
import com.mango.edu.mvcframework.annotations.Security;
import com.mango.edu.mvcframework.pojo.Handler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * url 绑定 方法的 map
     */
    //    private Map<String, Method> handlerMapping = new ConcurrentHashMap<>();
    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //处理请求
        //        String requestURI = req.getRequestURI();
        //        Method method = handlerMapping.get(requestURI);
        //        method.invoke()
        try {
            Handler handler = getHandler(req, resp);
            if (handler == null) {
                resp.getWriter().write("404 not found");
            }
            //设置匹配到的方法参数
            Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            Map<String, String[]> parameterMap = req.getParameterMap();
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String value = StringUtils.join(entry.getValue(), ",");
                if (!handler.getParams().containsKey(entry.getKey())) {
                    continue;
                }
                Integer integer = handler.getParams().get(entry.getKey());
                args[integer] = value;
            }
            Integer reqInteger = handler.getParams().get(HttpServletRequest.class.getSimpleName());
            args[reqInteger] = req;
            Integer respInteger = handler.getParams().get(HttpServletResponse.class.getSimpleName());
            args[respInteger] = resp;
            handler.getMethod().invoke(handler.getController(), args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Handler getHandler(HttpServletRequest req, HttpServletResponse resp) {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        String url = req.getRequestURI();
        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            Security security = handler.getController().getClass().getAnnotation(Security.class);
            if (null != security) {
                //根据注解属性值  拦截用户
                String[] value = security.value();
                if (null != value && value.length > 0) {
                    //属性不为空时拦截
                    for (String userName : value) {
                        if (!handler.getParams().containsValue(userName)) {
                            //如果map里没有value数组里的值， 证明访问用户无访问权限
                            try {
//                                resp.getWriter().write("当前用户无访问权限"); 中文乱码 所以改为输出数字
                                resp.getWriter().write("1231231");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return handler;
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) {
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
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
            if (!aClass.isAnnotationPresent(MangoController.class)) {
                continue;
            }
            String baseUrl = "";
            if (aClass.isAnnotationPresent(MangoRequestMapping.class)) {
                baseUrl = aClass.getAnnotation(MangoRequestMapping.class).value();
            }
            for (Method method : aClass.getMethods()) {
                if (!method.isAnnotationPresent(MangoRequestMapping.class)) {
                    continue;
                }
                MangoRequestMapping annotation = method.getAnnotation(MangoRequestMapping.class);
                String methodUrl = annotation.value();
                String url = baseUrl + methodUrl;
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(url));
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                        handler.getParams().put(parameter.getType().getSimpleName(), i);
                    } else {
                        handler.getParams().put(parameter.getName(), i);
                    }
                }
                handlerMapping.add(handler);
            }
        }
    }

    /**
     * 依赖注入
     */
    private void doAutowired() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(MangoAutowired.class)) {
                        continue;
                    }
                    MangoAutowired annotation = field.getAnnotation(MangoAutowired.class);
                    String beanId = annotation.value();
                    if ("".equals(beanId.trim())) {
                        beanId = field.getType().getName();
                    }
                    field.setAccessible(true);
                    field.set(entry.getValue(), iocMap.get(beanId));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
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
            if (file1.isDirectory()) {
                //如果是目录 进行递归
                doScanAnnotations(packagePath + "." + file1.getName());
            } else if (file1.getName().endsWith(".class")) {
                //不是目录  那就是class文件  替换掉后缀 拿到className
                String className = packagePath + "." + file1.getName().replaceAll(".class", "");
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
