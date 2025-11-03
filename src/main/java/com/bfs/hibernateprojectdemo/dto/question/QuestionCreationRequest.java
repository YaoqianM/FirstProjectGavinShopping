package com.bfs.hibernateprojectdemo.dto.question;

import lombok.*;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotBlank;

@Data
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreationRequest {

    @NotBlank(message = "description cannot be blank")
    String description;
    boolean isActive;
}
