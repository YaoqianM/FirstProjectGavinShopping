package com.bfs.hibernateprojectdemo.domain;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name="question")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Integer id;

    @Column(nullable = false)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
