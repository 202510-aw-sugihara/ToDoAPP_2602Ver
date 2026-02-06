package com.example.todo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
  List<Group> findAllByOrderByTypeAscNameAsc();
  Optional<Group> findByNameIgnoreCaseAndType(String name, GroupType type);
}
