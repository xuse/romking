package io.github.xuse.romaster.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.github.xuse.querydsl.lambda.LambdaTable;

import io.github.xuse.romking.RomConsole;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.dal.RomRepoRepository;
import io.github.xuse.romking.repo.obj.RomDir;

@Service
@PreAuthorize("isAuthenticated()")
public class RomService {

    private final RomRepoRepository dirRepository;
    private final RomFileRepository romRepository;
    private final Clock clock;

    RomService(RomConsole romConsole, Clock clock) {
        this.dirRepository = romConsole.getBean(RomRepoRepository.class);
        this.romRepository = romConsole.getBean(RomFileRepository.class);
        this.clock = clock;
    }

    public void createTask(String description, @Nullable LocalDate dueDate) {
        if ("fail".equals(description)) {
            throw new RuntimeException("This is for testing the error handler");
        }
        var task = new RomDir();
        task.setDescription(description);
        dirRepository.insert(task);
    }

    public List<RomDir> list(Pageable pageable) {
        LambdaTable<RomDir> table = () -> RomDir.class;
        return dirRepository.getFactory().selectFrom(table).offset(pageable.getOffset()).limit(pageable.getPageSize())
                .fetch();
    }

}
