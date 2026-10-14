package com.smartshield.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartshield.model.SafeRoute;

@Repository
public interface SafeRouteRepository extends JpaRepository<SafeRoute, Long> {

    Optional<SafeRoute> findBySafeRouteId(String safeRouteId);

    List<SafeRoute> findAllByOrderByCreatedAtDesc();

    List<SafeRoute> findBySafetyScoreLessThanEqualOrderBySafetyScoreAsc(Integer threshold);

    @Query("SELECT r FROM SafeRoute r WHERE " +
           "ABS(r.originLatitude - :oLat) < 0.002 AND " +
           "ABS(r.originLongitude - :oLng) < 0.002 AND " +
           "ABS(r.destinationLatitude - :dLat) < 0.002 AND " +
           "ABS(r.destinationLongitude - :dLng) < 0.002 AND " +
           "r.travelMode = :mode ORDER BY r.createdAt DESC")
    List<SafeRoute> findMatchingCachedRoutes(@Param("oLat") Double originLat,
                                            @Param("oLng") Double originLng,
                                            @Param("dLat") Double destLat,
                                            @Param("dLng") Double destLng,
                                            @Param("mode") String travelMode);

    long countBySafetyScoreLessThanEqual(Integer threshold);

    long countByAutoGrievanceFiledTrue();
}
