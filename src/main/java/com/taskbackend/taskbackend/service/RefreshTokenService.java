package com.taskbackend.taskbackend.service;

import com.taskbackend.taskbackend.entity.RefreshToken;
import com.taskbackend.taskbackend.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken validateAndGet(String token);

    void revokeOwnedByUser(String token, User user);
}
