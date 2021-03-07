package com.lagou.demo.controller;

import com.lagou.edu.mcvframework.annotiation.LagouAutowired;
import com.lagou.edu.mcvframework.annotiation.LagouController;
import com.lagou.edu.mcvframework.annotiation.LagouRequestMapping;
import com.lagou.demo.service.IDemoService;
import com.lagou.edu.mcvframework.annotiation.Security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@LagouController
@LagouRequestMapping("/demo")
public class DemoController {


    @LagouAutowired
    private IDemoService demoService;


    /**
     * URL: /demo/query?name=lisi
     * @param request
     * @param response
     * @param name
     * @return
     */
    @LagouRequestMapping("/query")
    @Security
    public String query(HttpServletRequest request, HttpServletResponse response, String name) {
        return demoService.get(name);
    }

}
