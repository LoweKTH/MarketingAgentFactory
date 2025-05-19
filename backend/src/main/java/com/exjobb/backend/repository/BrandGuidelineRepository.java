package com.exjobb.backend.repository;

import com.exjobb.backend.entity.BrandGuideline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BrandGuideline entity operations.
 *
 * Provides data access methods for managing brand guidelines and identity.
 * These guidelines are used by the Task Router when calling the Python
 * Content Agent to ensure brand consistency.
 */
@Repository
public interface BrandGuidelineRepository extends JpaRepository<BrandGuideline, Long> {

    /**
     * Find brand guidelines by brand name.
     * Allows supporting multiple brands in the future.
     *
     * @param brandName The name of the brand
     * @return Optional containing the brand guidelines if found
     */
    Optional<BrandGuideline> findByBrandName(String brandName);

    /**
     * Find all active brand guidelines.
     * Only active guidelines should be used for content generation.
     *
     * @return List of all active brand guidelines
     */
    List<BrandGuideline> findByActiveTrue();

    /**
     * Find the most recent version of brand guidelines for a brand.
     * Useful when multiple versions exist and you need the latest.
     *
     * @param brandName The name of the brand
     * @return Optional containing the latest brand guidelines
     */
    @Query("SELECT bg FROM BrandGuideline bg WHERE bg.brandName = :brandName AND bg.active = true ORDER BY bg.version DESC")
    Optional<BrandGuideline> findLatestByBrandName(@Param("brandName") String brandName);

    /**
     * Find brand guidelines by version.
     * Useful for tracking changes and rollbacks.
     *
     * @param brandName The name of the brand
     * @param version The version number
     * @return Optional containing the specific version
     */
    Optional<BrandGuideline> findByBrandNameAndVersion(String brandName, Integer version);

    /**
     * Find all versions of brand guidelines for a brand.
     * Useful for version history and comparison.
     *
     * @param brandName The name of the brand
     * @return List of all versions ordered by version number (newest first)
     */
    @Query("SELECT bg FROM BrandGuideline bg WHERE bg.brandName = :brandName ORDER BY bg.version DESC")
    List<BrandGuideline> findAllVersionsByBrandName(@Param("brandName") String brandName);

    /**
     * Check if a brand name already exists.
     * Used when creating new brand guidelines to prevent duplicates.
     *
     * @param brandName The brand name to check
     * @return True if brand name exists, false otherwise
     */
    boolean existsByBrandName(String brandName);

    /**
     * Get the default/primary brand guidelines.
     * Returns the active brand guidelines for the main brand.
     * This is typically used when no specific brand is requested.
     *
     * @return Optional containing the default brand guidelines
     */
    @Query("SELECT bg FROM BrandGuideline bg WHERE bg.active = true ORDER BY bg.createdAt ASC")
    Optional<BrandGuideline> findDefaultBrandGuidelines();

    /**
     * Count active brand guidelines.
     * Provides statistics for administrative purposes.
     *
     * @return Number of active brand guideline sets
     */
    long countByActiveTrue();
}