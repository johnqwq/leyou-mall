package com.leyou.page.web;

import com.leyou.page.pojo.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Controller
public class HelloController {
    @GetMapping("/hello")
    public String hello(Model model) {
        User user = new User();
        user.setAge(20);
        user.setName("Jack Chen");
        user.setFriend(new User("李小龙", 19, null));

        User user1 = new User("乔峰", 21, null);
        User user2 = new User("虚竹", 20, user1);
        List<User> users = Arrays.asList(user1, user2);

        model.addAttribute("user", user);
        model.addAttribute("users", users);
        return "hello";
    }
}
