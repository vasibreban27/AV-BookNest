package com.avbooknest.reporting;

import static com.avbooknest.reporting.ReportDtos.*;

import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.common.exception.NotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportStore {
  private final JdbcTemplate jdbc;

  public ReportStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Summary get(long id, boolean lock) {
    return jdbc
        .query(
            "SELECT * FROM content_reports WHERE id=?" + (lock ? " FOR UPDATE" : ""), this::map, id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Report not found"));
  }

  public long create(
      long reporter,
      Target type,
      long targetUser,
      Long book,
      String label,
      Reason reason,
      String description) {
    return jdbc.queryForObject(
        """
        INSERT INTO content_reports(reporter_id,target_type,target_user_id,book_id,target_label,reason,description,target_id)
        VALUES (?,?,?,?,?,?,?,?) RETURNING id
        """,
        Long.class,
        reporter,
        type.name(),
        targetUser,
        book,
        label,
        reason.name(),
        description,
        book == null ? targetUser : book);
  }

  public long recentCount(long reporter) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM content_reports WHERE reporter_id=? AND created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours'",
        Long.class,
        reporter);
  }

  public boolean duplicate(long reporter, Target type, long targetUser, Long book) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            """
        SELECT EXISTS(SELECT 1 FROM content_reports WHERE reporter_id=? AND target_type=?
        AND target_user_id=? AND target_id=? AND status IN ('NEW','IN_PROGRESS'))
        """,
            Boolean.class,
            reporter,
            type.name(),
            targetUser,
            book == null ? targetUser : book));
  }

  public boolean tradingPartners(long a, long b) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            """
        SELECT EXISTS(SELECT 1 FROM seller_orders s JOIN orders o ON o.id=s.order_id
        WHERE (o.buyer_id=? AND s.seller_id=?) OR (o.buyer_id=? AND s.seller_id=?))
        """,
            Boolean.class,
            a,
            b,
            b,
            a));
  }

  public PageResponse<Summary> list(
      Long reporter, Status status, Long targetUser, int page, int size) {
    String where = " WHERE 1=1";
    var args = new java.util.ArrayList<Object>();
    if (reporter != null) {
      where += " AND reporter_id=?";
      args.add(reporter);
    }
    if (status != null) {
      where += " AND status=?";
      args.add(status.name());
    }
    if (targetUser != null) {
      where += " AND target_user_id=?";
      args.add(targetUser);
    }
    long total =
        jdbc.queryForObject(
            "SELECT count(*) FROM content_reports" + where, Long.class, args.toArray());
    args.add(size);
    args.add((long) page * size);
    var rows =
        jdbc.query(
            "SELECT * FROM content_reports"
                + where
                + " ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
            this::map,
            args.toArray());
    return new PageResponse<>(
        rows,
        total,
        (int) ((total + size - 1) / size),
        page,
        size,
        ((long) page + 1) * size < total);
  }

  public void start(long id, long admin) {
    jdbc.update(
        "UPDATE content_reports SET status='IN_PROGRESS',assigned_to_id=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
        admin,
        id);
  }

  public void resolve(long id, long admin, Resolve request, Instant until) {
    jdbc.update(
        "UPDATE content_reports SET status=?,assigned_to_id=?,decision=?,decision_note=?,suspended_until=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
        request.decision() == Decision.DISMISS ? "DISMISSED" : "RESOLVED",
        admin,
        request.decision().name(),
        request.note().trim(),
        until == null ? null : Timestamp.from(until),
        id);
  }

  public void event(long id, long actor, String action, String note) {
    jdbc.update(
        "INSERT INTO report_events(report_id,actor_id,action,note) VALUES (?,?,?,?)",
        id,
        actor,
        action,
        note);
  }

  public List<Event> events(long id) {
    return jdbc.query(
        "SELECT * FROM report_events WHERE report_id=? ORDER BY id",
        (r, n) ->
            new Event(
                r.getLong("id"),
                r.getLong("actor_id"),
                r.getString("action"),
                r.getString("note"),
                instant(r, "created_at")),
        id);
  }

  public void addEvidence(long id, Evidence evidence) {
    jdbc.update(
        "INSERT INTO report_evidence(report_id,content_type,content) VALUES (?,?,?)",
        id,
        evidence.contentType(),
        evidence.content());
  }

  public List<EvidenceInfo> evidenceInfo(long id) {
    return jdbc.query(
        "SELECT id,content_type FROM report_evidence WHERE report_id=? ORDER BY id",
        (r, n) -> new EvidenceInfo(r.getLong(1), r.getString(2)),
        id);
  }

  public Evidence evidence(long report, long id) {
    return jdbc
        .query(
            "SELECT content_type,content FROM report_evidence WHERE report_id=? AND id=?",
            (r, n) -> new Evidence(r.getString(1), r.getBytes(2)),
            report,
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Evidence not found"));
  }

  public List<HistoryEntry> accountHistory(long userId) {
    return jdbc.query(
        """
        SELECT action,reason,created_at FROM admin_audit_logs WHERE target_type='USER' AND target_id=?
        AND action IN ('USER_SUSPENDED','USER_REACTIVATED','USER_SUSPENSION_EXPIRED','REPORT_WARNING','REPORT_BOOK_HIDDEN','REPORT_TEMPORARY_SUSPENSION')
        ORDER BY created_at DESC,id DESC LIMIT 100
        """,
        (r, n) -> new HistoryEntry(r.getString(1), r.getString(2), instant(r, "created_at")),
        userId);
  }

  private Summary map(ResultSet r, int row) throws SQLException {
    String decision = r.getString("decision");
    return new Summary(
        r.getLong("id"),
        r.getLong("reporter_id"),
        Target.valueOf(r.getString("target_type")),
        r.getLong("target_user_id"),
        r.getObject("book_id", Long.class),
        r.getString("target_label"),
        Reason.valueOf(r.getString("reason")),
        r.getString("description"),
        Status.valueOf(r.getString("status")),
        r.getObject("assigned_to_id", Long.class),
        decision == null ? null : Decision.valueOf(decision),
        r.getString("decision_note"),
        instant(r, "suspended_until"),
        instant(r, "created_at"),
        instant(r, "updated_at"));
  }

  private static Instant instant(ResultSet r, String column) throws SQLException {
    Timestamp value = r.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
