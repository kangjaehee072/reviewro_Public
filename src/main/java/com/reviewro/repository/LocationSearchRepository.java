package com.reviewro.repository;

import com.reviewro.dto.PlaceHitDTO;
import com.reviewro.entity.CategoryEntity;
import com.reviewro.entity.LocationEntity;
import com.reviewro.entity.TypeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LocationSearchRepository extends JpaRepository<LocationEntity, Long> {

    //조건이 null이면 그 조건은 통과
    @Query(
            value = """
        select l.* from location l
       WHERE ((:shortName IS NULL AND :fullName IS NULL)
             OR l.location_address LIKE CONCAT(:shortName, '%')
             OR l.location_address LIKE CONCAT(:fullName, '%'))
        AND (:lat IS NULL OR :lng IS NULL OR :radius IS NULL
             OR (6371 * ACOS(LEAST(1.0, GREATEST(-1.0,
             COS(RADIANS(:lat)) * COS(RADIANS(l.location_lat)) * COS(RADIANS(l.location_lng) - RADIANS(:lng))
                 + SIN(RADIANS(:lat)) * SIN(RADIANS(l.location_lat))))
                 ) <= :radius))
        AND (:noCategory = TRUE OR l.location_id IN (
                    SELECT lc.location_entity_location_id
                    FROM location_categories lc
                    WHERE lc.categories_category_id IN (:categories)
                      ))
        AND (:type IS NULL OR l.type_id = :type)
        AND (:element IS NULL OR l.location_element = :element)
        AND (:minRating IS NULL OR l.location_review_avg >= :minRating)
        AND (:keyword IS NULL
             OR l.location_name LIKE CONCAT('%', :keyword, '%')
             OR l.location_address LIKE CONCAT('%', :keyword, '%'))
        LIMIT :fetchLimit OFFSET :offset
""" , nativeQuery = true )
    List<LocationEntity> search(@Param("shortName") String shortName,
                                @Param("fullName") String fullName,
                                // 고른 카테고리 중 하나라도 붙은 장소. 안 골랐으면 noCategory=true
                                // (IN () 은 SQL 오류라 categories 에는 빈 목록 대신 아무 값이나 하나 넣는다)
                                @Param("noCategory") boolean noCategory,
                                @Param("categories") List<Long> categories,
                                @Param("type") Long type,
                                @Param("element") String element,
                                @Param("minRating") Double minRating,
                                @Param("keyword") String keyword,
                                @Param("lat") Double lat,
                                @Param("lng") Double lng,
                                @Param("radius") Integer radius,
                                @Param("offset") int offset,
                                @Param("fetchLimit") int fetchLimit
    );


    // ===== 랜덤 페이지(파트라슈) 전용 =====

    // AI 가 만든 조건으로 검색.
    // 지역·타입은 걸러내는 조건(WHERE), 카테고리·키워드는 가점 조건(ORDER BY)이다.
    // 가점으로 두는 이유: 필터로 쓰면 안 맞을 때 0건이 되고, 그러면 AI 가 없는 곳을 지어낸다.
    // 맞는 게 없어도 평점 높은 곳이 나오므로 AI 가 "딱 맞는 곳은 없지만" 이라고 말할 수 있다.
    // JPQL 로는 LIKE 결과를 더할 수 없어서 네이티브 쿼리를 쓴다.
    @Query(value = """
        SELECT l.location_id          AS locationId,
               l.location_name        AS locationName,
               l.location_address     AS locationAddress,
               l.location_description AS locationDescription,
               l.location_lat         AS locationLat,
               l.location_lng         AS locationLng,
               l.location_review_avg  AS locationReviewAvg,
               t.type_name            AS typeName,
               (SELECT GROUP_CONCAT(c.category_name ORDER BY c.category_sort_order)
                  FROM location_categories lc
                  JOIN category_entity c ON c.category_id = lc.categories_category_id
                 WHERE lc.location_entity_location_id = l.location_id)      AS allCategories,
               (SELECT GROUP_CONCAT(c.category_name ORDER BY c.category_sort_order)
                  FROM location_categories lc
                  JOIN category_entity c ON c.category_id = lc.categories_category_id
                 WHERE lc.location_entity_location_id = l.location_id
                   AND lc.categories_category_id IN (:categoryIds))         AS matchedCategories,
               ((SELECT COUNT(*) FROM location_categories lc
                  WHERE lc.location_entity_location_id = l.location_id
                    AND lc.categories_category_id IN (:categoryIds))
                + CASE WHEN :keyword <> '' AND (
                         l.location_name LIKE CONCAT('%', :keyword, '%')
                      OR l.location_description LIKE CONCAT('%', :keyword, '%')
                      OR l.location_address LIKE CONCAT('%', :keyword, '%'))
                       THEN 1 ELSE 0 END)                                   AS hits
          FROM location l
          LEFT JOIN type_entity t ON t.type_id = l.type_id
         WHERE (:areaShort = ''
             OR l.location_address LIKE CONCAT(:areaShort, '%')
             OR l.location_address LIKE CONCAT(:areaFull, '%'))
           AND (:city = '' OR l.location_address LIKE CONCAT('%', :city, '%'))
           AND (:typeId IS NULL OR l.type_id = :typeId)
         ORDER BY hits DESC, COALESCE(l.location_review_avg, 3.0) DESC
         LIMIT :limit
        """, nativeQuery = true)
    // areaShort·city 는 안 쓸 때 빈 문자열 (시/도 없이 시·군만으로도 찾는다)
    List<PlaceHitDTO> searchForAi(@Param("areaShort") String areaShort,
                                  @Param("areaFull") String areaFull,
                                  @Param("city") String city,
                                  @Param("typeId") Long typeId,
                                  @Param("categoryIds") List<Long> categoryIds,
                                  @Param("keyword") String keyword,
                                  @Param("limit") int limit);
}
