package vn.bookeasy.business;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "branch_hours")
@Getter
@Setter
@NoArgsConstructor
public class BranchHour {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long branchId;
    /** 1 = Thứ 2 ... 7 = Chủ nhật (ISO). */
    private int dayOfWeek;
    @Column(nullable = false)
    private LocalTime openTime;
    @Column(nullable = false)
    private LocalTime closeTime;
    private boolean closed;
}
