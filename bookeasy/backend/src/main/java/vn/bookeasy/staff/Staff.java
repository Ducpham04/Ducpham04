package vn.bookeasy.staff;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;
    @Column(nullable = false)
    private Long branchId;
    @Column(nullable = false)
    private String fullName;
    private String title;
    private String phone;
    private String email;
    private String avatarUrl;
    @Column(nullable = false)
    private String color = "#3b82f6";
    private boolean active = true;

    @Override
    public boolean equals(Object o) {
        return o instanceof Staff s && id != null && id.equals(s.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
