package vn.educenter.common;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, CodeSequence.Key> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CodeSequence s where s.tenantId = :tenantId and s.seqKey = :key")
    Optional<CodeSequence> lockByKey(Long tenantId, String key);
}
