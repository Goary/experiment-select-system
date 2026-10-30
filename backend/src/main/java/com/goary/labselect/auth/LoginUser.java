package com.goary.labselect.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUser {

    private Long id;
    private String employeeNo;
    private String username;
    private String role;
}
