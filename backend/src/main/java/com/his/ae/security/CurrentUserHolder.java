package com.his.ae.security;

/** 请求级当前用户持有者（ThreadLocal，拦截器设置/清除） */
public class CurrentUserHolder {

    private static final ThreadLocal<LoginUser> TL = new ThreadLocal<>();

    public static void set(LoginUser user) {
        TL.set(user);
    }

    public static LoginUser get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }
}
