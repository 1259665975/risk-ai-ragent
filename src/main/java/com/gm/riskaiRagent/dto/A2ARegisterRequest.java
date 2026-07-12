package com.gm.riskaiRagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class A2ARegisterRequest {

    @NotBlank(message = "name 不能为空")
    @Size(max = 100, message = "name 长度不能超过 100")
    private String name;

    @Size(max = 500, message = "description 长度不能超过 500")
    private String description;

    @NotEmpty(message = "capabilities 不能为空")
    private List<@NotBlank(message = "capabilities 不能包含空值") String> capabilities;

    @NotBlank(message = "endpoint 不能为空")
    @Size(max = 255, message = "endpoint 长度不能超过 255")
    private String endpoint;
}
