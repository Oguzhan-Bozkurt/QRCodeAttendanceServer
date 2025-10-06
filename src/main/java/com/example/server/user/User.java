package com.example.server.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "users", schema = "public")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false)
    private Long userName;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(name = "user_is_student", nullable = false)
    private boolean userIsStudent = true;

    private String title;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserName() { return userName; }
    public void setUserName(Long userName) { this.userName = userName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public boolean isUserIsStudent() { return userIsStudent; }
    public void setUserIsStudent(boolean userIsStudent) { this.userIsStudent = userIsStudent; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
