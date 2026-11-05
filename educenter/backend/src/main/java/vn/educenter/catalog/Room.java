package vn.educenter.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room extends TenantEntity {
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private int capacity;
    @Column(nullable = false)
    private boolean active = true;
}
