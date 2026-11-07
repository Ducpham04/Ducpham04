package vn.bookeasy.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class ServiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;
    private Long categoryId;
    @Column(nullable = false)
    private String name;
    private String description;
    private int durationMinutes;
    private long price;
    private boolean active = true;
    private int sortOrder;

    @ManyToMany
    @JoinTable(name = "service_staff",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "staff_id"))
    private Set<vn.bookeasy.staff.Staff> staff = new HashSet<>();
}
