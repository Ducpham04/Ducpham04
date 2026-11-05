package vn.educenter.common;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "code_sequences")
@IdClass(CodeSequence.Key.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeSequence {
    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Id
    @Column(name = "seq_key")
    private String seqKey;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private Long tenantId;
        private String seqKey;
    }
}
