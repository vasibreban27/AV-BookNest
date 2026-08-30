package com.avbooknest.support.repository;

import com.avbooknest.support.model.SupportMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
  Page<SupportMessage> findAllByTicketId(Long ticketId, Pageable pageable);
}
