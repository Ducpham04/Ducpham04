package vn.bookeasy.business;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;
    @Column(nullable = false)
    private String name;
    private String address;
    private String phone;
    private boolean active = true;
}
