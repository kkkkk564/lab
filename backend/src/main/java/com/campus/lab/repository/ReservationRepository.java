package com.campus.lab.repository;

import com.campus.lab.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByLabIdAndDateAndStatusIn(Long labId, LocalDate date, Collection<String> statuses);

    List<Reservation> findByLabIdAndDateOrderByIdAsc(Long labId, LocalDate date);

    List<Reservation> findByUserIdOrderByDateDescStartTimeDesc(Long userId);

    List<Reservation> findAllByOrderByDateDescStartTimeDesc();

    List<Reservation> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);

    List<Reservation> findByDate(LocalDate date);

    long countByStatus(String status);

    long countByLabId(Long labId);

    long countByUserId(Long userId);

    /**
     * 悲观写锁读取单条预约（SELECT ... FOR UPDATE），审批时与实验室行锁配合消除并发审批竞态。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    /**
     * 悲观写锁读取某实验室某日的有效预约。加锁读读取最新已提交数据，
     * 与实验室行锁配合后可串行化同一实验室的预约创建与审批，杜绝时段双占位。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.labId = :labId and r.date = :date and r.status in :statuses")
    List<Reservation> findActiveByLabAndDateForUpdate(@Param("labId") Long labId,
                                                      @Param("date") LocalDate date,
                                                      @Param("statuses") Collection<String> statuses);
}
