package com.xydp.entity;

import lombok.*;

/**
 * @author 付淇
 * @version 1.0
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Student {
    private Integer no;
    private String password;
    private String phone;
}
