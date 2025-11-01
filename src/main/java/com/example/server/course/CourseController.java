package com.example.server.course;

import com.example.server.user.User;
import com.example.server.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseRepository repo;
    private final UserRepository userRepo;

    public CourseController(CourseRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<CourseDto> myCourses(@AuthenticationPrincipal UserDetails principal) {
        Long userName = parsePrincipal(principal);
        return repo.findAllByOwner_UserName(userName)
                .stream().map(CourseDto::from).toList();
    }

    @PostMapping
    public CourseDto create(@RequestBody @Valid CourseCreateRequest req,
                            @AuthenticationPrincipal UserDetails principal) {
        Long userName = parsePrincipal(principal);

        User owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (req.studentIds() == null || req.studentIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "En az bir öğrenci seçmelisiniz");
        }

        List<User> fetched = userRepo.findAllById(req.studentIds());
        if (fetched.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz öğrenci listesi");
        }

        Course c = new Course();
        c.setCourseName(req.courseName());
        c.setCourseCode(req.courseCode());
        c.setOwner(owner);

        Set<User> students = new HashSet<>(fetched);
        c.setStudents(students);

        return CourseDto.from(repo.save(c));
    }

    @GetMapping("/my")
    public List<CourseDto> myCoursesAlt(@AuthenticationPrincipal UserDetails principal) {
        Long userName = parsePrincipal(principal);
        return repo.findAllByOwner_UserName(userName)
                .stream().map(CourseDto::from).toList();
    }

    private Long parsePrincipal(UserDetails principal) {
        try {
            return Long.parseLong(principal.getUsername());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid principal");
        }
    }
}
