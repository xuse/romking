package io.github.xuse.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;

import io.github.xuse.romaster.service.RomService;
import io.github.xuse.romking.repo.dal.RomDirRepository;
import io.github.xuse.security.dev.SampleUsers;
import jakarta.validation.ValidationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TaskServiceIT { // TODO Rename to TaskServiceTest to run it together with the unit tests.

    @Autowired
    RomService taskService;

    @Autowired
    RomDirRepository taskRepository;

    @Autowired
    Clock clock;


    @Test
    @WithUserDetails(SampleUsers.ADMIN_USERNAME)
    public void tasks_are_validated_before_they_are_stored() {
        assertThatThrownBy(() -> taskService.createTask("X".repeat(255 + 1), null))
                .isInstanceOf(ValidationException.class);
    }
}
