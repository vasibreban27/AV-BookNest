package com.avbooknest.support.repository;

import com.avbooknest.support.model.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
  @Query(
      "select t from SupportTicket t where t.requester.id = :userId and (:status is null or t.status = :status)")
  Page<SupportTicket> forRequester(
      @Param("userId") Long userId, @Param("status") SupportStatus status, Pageable pageable);

  Optional<SupportTicket> findByIdAndRequesterId(Long id, Long requesterId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from SupportTicket t where t.id = :id")
  Optional<SupportTicket> findByIdForUpdate(@Param("id") Long id);

  @EntityGraph(attributePaths = {"requester", "assignedTo"})
  @Query(
      """
      select t from SupportTicket t
      where (:status is null or t.status = :status)
        and (:assignedToId is null or t.assignedTo.id = :assignedToId)
        and (:unassigned = false or t.assignedTo is null)
        and (:query = '' or lower(t.reference) like concat('%', :query, '%')
          or lower(t.subject) like concat('%', :query, '%')
          or lower(t.contactEmail) like concat('%', :query, '%')
          or lower(t.contactName) like concat('%', :query, '%'))
      """)
  Page<SupportTicket> forAdmin(
      @Param("status") SupportStatus status,
      @Param("assignedToId") Long assignedToId,
      @Param("unassigned") boolean unassigned,
      @Param("query") String query,
      Pageable pageable);
}
