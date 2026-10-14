package com.smartshield.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartshield.model.SafeRouteRating;

@Repository
public interface SafeRouteRatingRepository extends JpaRepository<SafeRouteRating, Long> {

    List<SafeRouteRating> findByRouteIdOrderByCreatedAtDesc(Long routeId);

    List<SafeRouteRating> findByRouteSafeRouteIdOrderByCreatedAtDesc(String safeRouteId);

    Optional<SafeRouteRating> findByRouteIdAndCitizenId(Long routeId, Long citizenId);

    Optional<SafeRouteRating> findByRouteSafeRouteIdAndCitizenId(String safeRouteId, Long citizenId);

    long countByRouteId(Long routeId);

    long countByRouteSafeRouteId(String safeRouteId);

    @Query("SELECT AVG(r.rating) FROM SafeRouteRating r WHERE r.route.id = :routeId")
    Double getAverageRatingForRoute(@Param("routeId") Long routeId);

    @Query("SELECT AVG(r.rating) FROM SafeRouteRating r WHERE r.route.safeRouteId = :safeRouteId")
    Double getAverageRatingForSafeRouteId(@Param("safeRouteId") String safeRouteId);
}
