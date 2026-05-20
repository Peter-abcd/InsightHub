package com.greate.community.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Profile("develop")
@Controller
@RequestMapping("/dev/analytics")
public class BehaviorAnalyticsPageController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "/site/analytics-dashboard";
    }
}
