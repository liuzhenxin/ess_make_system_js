package com.clt.ess.base;

import com.clt.ess.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class EssInterceptor implements HandlerInterceptor {
    /**
     * Handler执行完成之后调用这个方法
     */
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler, Exception exc)
            throws Exception {
    }

    /**
     * Handler执行之后，ModelAndView返回之前调用这个方法
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
    }

    /**
     * Handler执行之前调用这个方法
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        //获取Session
        HttpSession session = request.getSession();
        User user = (User)session.getAttribute("user");

        if(user != null){
            return true;
        }
        //获取请求的URL
        String url = request.getRequestURI();
        //URL:login.jsp是公开的;这个demo是除了login.jsp是可以公开访问的，其它的URL都进行拦截控制
        if(url.contains("index")){
            return true;
        }
        if(url.contains("do")){
            return true;
        }
        if(url.contains("error")){
            return true;
        }
//        http://10.40.4.7:8080/EssSealPlatform/unit/logout
        response.sendRedirect("http://10.40.4.7:8080/EssSealPlatform/unit/logout");
        return false;
    }
}
