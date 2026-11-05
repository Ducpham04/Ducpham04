package vn.educenter.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.BaseEntity;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant extends BaseEntity {
    public enum Plan { TRIAL, BASIC, PRO, LIFETIME }
    public enum Status { ACTIVE, SUSPENDED }

    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String slug;
    private String phone;
    private String email;
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.TRIAL;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
    @Column(name = "bank_code")
    private String bankCode;
    @Column(name = "bank_account_no")
    private String bankAccountNo;
    @Column(name = "bank_account_name")
    private String bankAccountName;

    public boolean hasBankInfo() {
        return bankCode != null && !bankCode.isBlank() && bankAccountNo != null && !bankAccountNo.isBlank();
    }
}
