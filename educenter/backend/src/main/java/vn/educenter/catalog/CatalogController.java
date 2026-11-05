package vn.educenter.catalog;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Khóa học & phòng")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService service;

    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<CatalogDtos.CourseResponse> courses() {
        return service.courses();
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public CatalogDtos.CourseResponse createCourse(@Valid @RequestBody CatalogDtos.CourseRequest req) {
        return service.createCourse(req);
    }

    @PutMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public CatalogDtos.CourseResponse updateCourse(@PathVariable Long id, @Valid @RequestBody CatalogDtos.CourseRequest req) {
        return service.updateCourse(id, req);
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public void deleteCourse(@PathVariable Long id) {
        service.deleteCourse(id);
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF','TEACHER')")
    public List<CatalogDtos.RoomResponse> rooms() {
        return service.rooms();
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public CatalogDtos.RoomResponse createRoom(@Valid @RequestBody CatalogDtos.RoomRequest req) {
        return service.createRoom(req);
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public CatalogDtos.RoomResponse updateRoom(@PathVariable Long id, @Valid @RequestBody CatalogDtos.RoomRequest req) {
        return service.updateRoom(id, req);
    }

    @DeleteMapping("/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CENTER_ADMIN','STAFF')")
    public void deleteRoom(@PathVariable Long id) {
        service.deleteRoom(id);
    }
}
