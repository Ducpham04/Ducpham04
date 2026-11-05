package vn.bookeasy.business;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "holidays")
@Getter
@Setter
@NoArgsConstructor
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;
    private Long branchId;
    @Column(nullable = false)
    private LocalDate holidayDate;
    @Column(nullable = false)
    private String name;
}
