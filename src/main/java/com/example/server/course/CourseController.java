package com.example.server.course;

import com.example.server.user.User;
import com.example.server.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository repo;
    private final UserRepository userRepo;

    public CourseController(CourseRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @GetMapping({ "", "/my" })
    public List<CourseDto> myCourses(@AuthenticationPrincipal UserDetails principal) {
        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }

        return repo.findAllByOwner_UserName(userName)
                .stream()
                .map(CourseDto::from)
                .toList();
    }

    @PostMapping
    public CourseDto create(@RequestBody @Valid CourseCreateRequest req,
                            @AuthenticationPrincipal UserDetails principal) {
        Long userName;
        try {
            userName = Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }

        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Course c = new Course();
        c.setCourseName(req.courseName());
        c.setCourseCode(req.courseCode());
        c.setOwner(owner);

        return CourseDto.from(repo.save(c));
    }
}
