package com.taskbackend.taskbackend.security;

import org.springframework.stereotype.Component;

import com.taskbackend.taskbackend.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class SessionUserResolver {

    public static final String SESSION_USER_ID_ATTRIBUTE = "USER_ID";

    public Long requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long userId = session != null ? (Long) session.getAttribute(SESSION_USER_ID_ATTRIBUTE) : null;

        if (userId == null) {
            throw new UnauthorizedException();
        }

        return userId;
    }
}
