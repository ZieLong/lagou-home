package com.lagou.edu.mcvframework.servlet;

import com.lagou.edu.mcvframework.annotiation.*;
import com.lagou.edu.mcvframework.pojo.Handler;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.ObjectHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LgDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private List<Handler> handlerMapping = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件springmvc.properties
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);

        //2.扫描注解相关的类
        doScan(properties.getProperty("scanPackage"));


        //3.初始化bean对象，实现ioc容器，基于注解
        doInstance();

        //4.实现依赖注入

        doAutowired();


        //5.构造一个HandlerMapping处理器映射器，将配置好的url和method建立映射关系

        initHandlerMapping();

        System.out.println("初始化完成，等待进入");

    }

    private void initHandlerMapping() {

        if(ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();

            if (!aClass.isAnnotationPresent(LagouController.class)) {
                continue;
            }

            String baseUrl = "";
            if (aClass.isAnnotationPresent(LagouRequestMapping.class)) {
                LagouRequestMapping annotation = aClass.getAnnotation(LagouRequestMapping.class);
                baseUrl = annotation.value();
           }

            //获取类中的方法

            Method[] methods = aClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                if (!method.isAnnotationPresent(LagouRequestMapping.class)) {
                    continue;
                }
                LagouRequestMapping annotation = method.getAnnotation(LagouRequestMapping.class);
                String methodUrl = annotation.value();
                String url = baseUrl + methodUrl;

                //把metho所有的信息及url封装成一个Handler
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(url));

                Parameter[] parameters = method.getParameters();

                for (int j = 0; j < parameters.length ; j++) {
                    Parameter parameter = parameters[j];

                    if(parameter.getType() == HttpServletRequest.class
                            || parameter.getType() == HttpServletResponse.class) {
                        handler.getParamIndexMapping().put(parameter.getType().getSimpleName(), j);
                    } else {
                        handler.getParamIndexMapping().put(parameter.getName(), j);
                    }
                }

                handlerMapping.add(handler);

            }

        }

    }

    private void doAutowired() {

        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] declareFields = entry.getValue().getClass().getDeclaredFields();
            for (int i = 0; i < declareFields.length; i++) {
                Field declareField = declareFields[i];
                if (!declareField.isAnnotationPresent(LagouAutowired.class)) {
                    continue;
                }

                LagouAutowired annotation = declareField.getAnnotation(LagouAutowired.class);
                String beanName = annotation.value();
                if ("".equals(beanName.trim())) {
                    beanName = declareField.getType().getName();
                }
                //强制开启赋值
                declareField.setAccessible(true);
                try {
                    declareField.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * ioc容器
     */
    private void doInstance() {

        if (classNames.size() == 0) {
            return;
        }

        try {
            for (int i = 0; i < classNames.size(); i++) {
                String className = classNames.get(i);

                Class<?> aClass = Class.forName(className);

                if (aClass.isAnnotationPresent(LagouController.class)) {
                    String simpleName = aClass.getSimpleName();
                    String lowerFirstName = lowerFirstName(simpleName);
                    Object o = aClass.newInstance();
                    ioc.put(lowerFirstName,o);
                } else if (aClass.isAnnotationPresent(LagouService.class)) {
                    LagouService annotation = aClass.getAnnotation(LagouService.class);
                        String beanName = annotation.value();

                        if(!"".equals(beanName.trim())) {
                            ioc.put(beanName, aClass.newInstance());
                        } else {
                            beanName = lowerFirstName(aClass.getSimpleName());
                            ioc.put(beanName,aClass.newInstance());
                        }

                        Class<?>[] interfaces = aClass.getInterfaces();
                        for (int j = 0; j < interfaces.length; j++) {
                            Class<?> anInterface = interfaces[j];
                            ioc.put(anInterface.getName(), aClass.newInstance());
                        }

                    } else {
                    continue;
                    }
                }
            }catch (Exception e){
            e.printStackTrace();
        }

    }

    private String lowerFirstName(String str) {
        char[] chars = str.toCharArray();
        if ('A' <= chars[0] && chars[0] < 'Z') {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    private void doScan(String scanPackage) {

        String scanPackagePath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + scanPackage.replaceAll("\\.", "/");
        File pack = new File(scanPackagePath);

        File[] files = pack.listFiles();

        for(File file: files) {
            if(file.isDirectory()) { // 子package
                // 递归
                doScan(scanPackage + "." + file.getName());  // com.lagou.demo.controller
            }else if(file.getName().endsWith(".class")) {
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                classNames.add(className);
            }

        }


    }

    private void doLoadConfig(String contextConfigLocation) {

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    //接收处理请求
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 处理请求：根据url，找到对应的Method方法，进行调用
        // 获取uri
//        String requestURI = req.getRequestURI();
//        Method method = handlerMapping.get(requestURI);// 获取到一个反射的方法
        // 反射调用，需要传入对象，需要传入参数，此处无法完成调用，没有把对象缓存起来，也没有参数！！！！改造initHandlerMapping();
//        method.invoke() //

        Handler handler = getHandler(req);

        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }

        Set<String> strings = new HashSet<>();
        String[] stringList = null;
        if(handler.getMethod().isAnnotationPresent(Security.class)) {
            Security annotation = handler.getMethod().getAnnotation(Security.class);
             stringList = annotation.value();
        }
        if(stringList != null) {
            for (String str : stringList) {
                strings.add(str);
            }
        }
        String name = req.getParameter("name");
        if(!strings.contains(name)) {
            resp.getWriter().write("no auth to access");
            return;
        }
        //参数绑定
        //获取所有参数类型数组，这个数组的长度就是我们最后要传入的args数组的长度
        Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();

        //根据上述数组长度创建一个新的数组（参数数组，是要传入反射调用）
        Object[] paramValues = new Object[parameterTypes.length];

        //以下就是为了向参数数组中塞值，而且还得保证参数的顺序和方法中形参的顺序一致
        Map<String, String[]> paramterMap = req.getParameterMap();

        for (Map.Entry<String, String[]> param : paramterMap.entrySet()) {
            String value = StringUtils.join(param.getValue(), ",");

            //如果参数和方法中的参数匹配上了，填充数据

            if (!handler.getParamIndexMapping().containsKey(param.getKey())) {
                continue;
            }
            Integer index = handler.getParamIndexMapping().get(param.getKey());

            paramValues[index] = value;

        }

        int requestIndex = handler.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
        paramValues[requestIndex] = req;

        int responseIndex = handler.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
        paramValues[responseIndex] = resp;





        try {
            handler.getMethod().invoke(handler.getController(), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }


    private Handler getHandler(HttpServletRequest req) {
        if(handlerMapping.isEmpty()){return null;}

        String url = req.getRequestURI();

        for(Handler handler: handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if(!matcher.matches()){continue;}
            return handler;
        }

        return null;

    }
}
