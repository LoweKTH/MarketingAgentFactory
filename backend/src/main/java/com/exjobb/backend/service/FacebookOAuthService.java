package com.exjobb.backend.service;

import org.springframework.stereotype.Service;

@Service("facebookOAuthService")
public class FacebookOAuthService implements OAuthService {

    @Override
    public String generateAuthUrl(String state) {
        return "https://mock-facebook-auth.com?state=" + state;
    }
}
