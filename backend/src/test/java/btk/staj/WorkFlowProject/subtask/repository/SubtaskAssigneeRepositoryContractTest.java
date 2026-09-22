package btk.staj.WorkFlowProject.subtask.repository;

import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.parser.PartTree;

import static org.assertj.core.api.Assertions.assertThat;

class SubtaskAssigneeRepositoryContractTest {

    @Test
    void bulkAssigneeLookupLoadsRolesInTheSameRepositoryCall() throws Exception {
        assertThat(new PartTree("findAllByIdIn", User.class)).isNotNull();

        Method method = UserRepository.class.getMethod("findAllByIdIn", Collection.class);
        assertThat(method.getAnnotation(EntityGraph.class).attributePaths())
                .containsExactly("role");
    }
}
