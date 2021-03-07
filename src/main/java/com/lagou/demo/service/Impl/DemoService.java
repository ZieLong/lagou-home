package com.lagou.demo.service.Impl;

import com.lagou.edu.mcvframework.annotiation.LagouService;
import com.lagou.demo.service.IDemoService;

/**
 * @author wz
 * @date 2021/3/6
 */

@LagouService("demoService")
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        System.out.println("Service 实现类中的name参数：" + name);
        return name;
    }
}
