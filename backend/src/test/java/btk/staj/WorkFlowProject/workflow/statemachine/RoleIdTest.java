package btk.staj.WorkFlowProject.workflow.statemachine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

class RoleIdTest {
    @Test void rejectsMissingIdentity() {
        assertThatNullPointerException().isThrownBy(() -> new RoleId(null)).withMessage("value");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveIdentity(int value) {
        assertThatIllegalArgumentException().isThrownBy(() -> new RoleId(value))
                .withMessageContaining("positive");
    }

    @Test void equalValuesAddressTheSameMapEntryOutsideIntegerCache() {
        var map = new HashMap<RoleId, String>();
        map.put(new RoleId(1001), "role");
        assertThat(map.get(new RoleId(1001))).isEqualTo("role");
        assertThat(map).doesNotContainKey(new RoleId(1002));
    }
}
