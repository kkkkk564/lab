package com.campus.lab.repository;

import com.campus.lab.entity.Lab;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LabRepository extends JpaRepository<Lab, Long> {

    /**
     * 悲观写锁读取实验室行（SELECT ... FOR UPDATE）。
     * 预约创建与审批均先锁此行，把同一实验室的写操作串行化，防止并发时段双占位。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lab l where l.id = :id")
    Optional<Lab> findByIdForUpdate(@Param("id") Long id);
}
