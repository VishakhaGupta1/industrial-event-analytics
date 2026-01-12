package com.industrial.eventanalytics.repository;

import com.industrial.eventanalytics.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    Optional<Event> findByEventId(String eventId);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.machineId = :machineId " +
           "AND e.eventTime >= :start AND e.eventTime < :end")
    long countEventsByMachineAndTimeWindow(@Param("machineId") String machineId,
                                          @Param("start") Instant start,
                                          @Param("end") Instant end);
    
    @Query("SELECT SUM(e.defectCount) FROM Event e WHERE e.machineId = :machineId " +
           "AND e.eventTime >= :start AND e.eventTime < :end " +
           "AND e.defectCount != -1")
    Long sumDefectsByMachineAndTimeWindow(@Param("machineId") String machineId,
                                         @Param("start") Instant start,
                                         @Param("end") Instant end);
    
    @Query("SELECT e.machineId as lineId, SUM(e.defectCount) as totalDefects, COUNT(e) as eventCount " +
           "FROM Event e WHERE e.eventTime >= :from AND e.eventTime <= :to " +
           "AND e.defectCount != -1 " +
           "GROUP BY e.machineId " +
           "ORDER BY SUM(e.defectCount) DESC")
    List<Object[]> findTopDefectLinesByTimeWindow(@Param("from") Instant from,
                                                  @Param("to") Instant to);
}
