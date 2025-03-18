package com.clipflow.clipflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaverUserResponse {

    private String username;
    private String email;
    private String nickname;
    private String profileImage;

}
