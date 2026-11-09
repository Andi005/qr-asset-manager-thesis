package io.github.andi005.qram.asset;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, Long> {

  Optional<Asset> findByTag(String tag);

  boolean existsByTag(String tag);

  boolean existsBySerialNumber(String serialNumber);

  boolean existsBySerialNumberAndIdNot(String serialNumber, Long id);

  List<Asset> findByIdInOrderByTagAsc(Collection<Long> ids);

  List<Asset> findByTagIn(Collection<String> tags);

  List<Asset> findBySerialNumberIn(Collection<String> serialNumbers);

  List<Asset> findByLocationIdAndStatusNotOrderByTagAsc(Long locationId, AssetStatus status);

  long countByStatus(AssetStatus status);

  /** Free-text search over tag, serial, manufacturer and model, plus optional filters. */
  @Query(
      value =
          """
          select a from Asset a left join a.location l
          where (lower(a.tag) like :pattern
                 or lower(coalesce(a.serialNumber, '')) like :pattern
                 or lower(coalesce(a.manufacturer, '')) like :pattern
                 or lower(a.model) like :pattern)
            and (:status is null or a.status = :status)
            and (:locationId is null or l.id = :locationId)
          """)
  Page<Asset> search(
      @Param("pattern") String pattern,
      @Param("status") AssetStatus status,
      @Param("locationId") Long locationId,
      Pageable pageable);

  /** Warranties ending inside [from, to], for assets still in use. */
  @Query(
      """
      select a from Asset a
      where a.status <> io.github.andi005.qram.asset.AssetStatus.RETIRED
        and a.warrantyEndDate between :from and :to
      order by a.warrantyEndDate, a.tag
      """)
  List<Asset> warrantiesEndingBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

  /** Warranties that already ended before the given day, for assets still in use. */
  @Query(
      """
      select a from Asset a
      where a.status <> io.github.andi005.qram.asset.AssetStatus.RETIRED
        and a.warrantyEndDate < :day
      order by a.warrantyEndDate desc, a.tag
      """)
  List<Asset> warrantiesEndedBefore(@Param("day") LocalDate day);

  @Query(
      "select a.location.id, count(a) from Asset a where a.location is not null group by a.location.id")
  List<Object[]> countPerLocation();

  @Query(value = "select nextval('asset_tag_seq')", nativeQuery = true)
  long nextTagNumber();

  /**
   * After an import with explicit tags, moves the tag sequence past the highest tag in use, so
   * newly generated tags never collide with imported ones.
   */
  @Query(
      value =
          """
          select setval('asset_tag_seq',
                        greatest(coalesce(max(cast(substring(asset_tag from 4) as bigint)), 0), 1),
                        coalesce(max(cast(substring(asset_tag from 4) as bigint)), 0) > 0)
          from asset
          """,
      nativeQuery = true)
  long syncTagSequence();

  /** True while the asset has a holder (an open assignment row). */
  @Query(
      value =
          "select exists(select 1 from assignment where asset_id = :assetId and returned_at is null)",
      nativeQuery = true)
  boolean hasOpenAssignment(@Param("assetId") long assetId);
}
