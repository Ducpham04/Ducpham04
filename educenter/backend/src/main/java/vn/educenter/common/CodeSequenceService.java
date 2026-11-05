package vn.educenter.common;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Sinh mã tăng dần theo từng trung tâm (HV0001, HD2611-0001...). Khóa dòng để an toàn khi đồng thời. */
@Service
@RequiredArgsConstructor
public class CodeSequenceService {

    private final CodeSequenceRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public long next(Long tenantId, String key) {
        CodeSequence seq = repository.lockByKey(tenantId, key).orElse(null);
        if (seq == null) {
            try {
                repository.saveAndFlush(new CodeSequence(tenantId, key, 2L));
                return 1L;
            } catch (DataIntegrityViolationException race) {
                seq = repository.lockByKey(tenantId, key).orElseThrow();
            }
        }
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        return value;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextCode(Long tenantId, String key, String prefix, int width) {
        return prefix + String.format("%0" + width + "d", next(tenantId, key));
    }
}
