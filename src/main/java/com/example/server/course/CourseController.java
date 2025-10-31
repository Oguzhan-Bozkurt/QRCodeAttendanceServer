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
    private final CourseEnrollmentRepository enrollmentRepo;


    public CourseController(CourseRepository repo, UserRepository userRepo, CourseEnrollmentRepository enrollmentRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.enrollmentRepo = enrollmentRepo;
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

    @GetMapping("/{courseId}/students")
    public List<com.example.server.course.EnrollmentDto.CourseStudentDto> listStudents(
            @PathVariable Long courseId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {

        Long userName = Long.parseLong(principal.getUsername());
        var owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));

        var course = repo.findById(courseId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (!course.getOwner().getId().equals(owner.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);

        return enrollmentRepo.findAllByCourse_Id(courseId).stream()
                .map(e -> new com.example.server.course.EnrollmentDto.CourseStudentDto(
                        e.getStudent().getId(),
                        e.getStudent().getUserName(),
                        e.getStudent().getName(),
                        e.getStudent().getSurname()
                ))
                .toList();
    }

    @PostMapping("/{courseId}/students")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void addStudents(@PathVariable Long courseId,
                            @RequestBody com.example.server.course.EnrollmentDto.AddStudentsRequest req,
                            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {

        Long userName = Long.parseLong(principal.getUsername());
        var owner = userRepo.findByUserName(userName)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));

        var course = repo.findById(courseId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (!course.getOwner().getId().equals(owner.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);

        if (req == null || req.userNames() == null) return;

        for (Long un : req.userNames()) {
            var student = userRepo.findByUserName(un)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "User not found: " + un));
            if (enrollmentRepo.existsByCourse_IdAndStudent_Id(courseId, student.getId())) continue;

            var en = new CourseEnrollment();
            en.setCourse(course);
            en.setStudent(student);
            enrollmentRepo.save(en);
        }
    }

    @DeleteMapping("/{courseId}/students/{userName}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void removeStudent(@PathVariable Long courseId,
                              @PathVariable Long userName,
                              @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {

        Long ownerUN = Long.parseLong(principal.getUsername());
        var owner = userRepo.findByUserName(ownerUN)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));

        var course = repo.findById(courseId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if (!course.getOwner().getId().equals(owner.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);

        var student = userRepo.findByUserName(userName)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        enrollmentRepo.deleteByCourse_IdAndStudent_Id(courseId, student.getId());
    }
}
