package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.CalendarUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CalendarUserRepository extends JpaRepository<CalendarUser, Long> {

	Optional<CalendarUser> findByEmailIgnoreCase(String email);

	List<CalendarUser> findByEmailInIgnoreCase(Collection<String> emails);
}
