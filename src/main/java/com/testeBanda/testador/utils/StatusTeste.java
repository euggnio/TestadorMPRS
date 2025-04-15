package com.testeBanda.testador.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StatusTeste {

    private final AtomicBoolean flag = new AtomicBoolean(false);

    public boolean isFlag() {
        return flag.get();
    }

    public void setFlag(boolean value) {
        flag.set(value);
    }
}