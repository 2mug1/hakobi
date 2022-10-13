package me.hyperbone.hakobi.hakobi.letter.listener;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class LetterListenerData {

    private Object instance;
    private Method method;
    private String id;

    public LetterListenerData(Object instance, Method method, String id) {
        this.instance = instance;
        this.method = method;
        this.id = id;
    }
}
