package me.hyperbone.hakobi.hakobi.letter;

import lombok.Data;

import java.util.Map;

@Data
public class Letter {

    private String id;
    private Map<String, ?> data;

    public Letter(String id, Map<String, ?> data) {
        this.id = id;
        this.data = data;
    }
}
