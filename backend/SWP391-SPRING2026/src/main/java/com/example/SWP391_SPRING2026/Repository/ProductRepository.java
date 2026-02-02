package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.Product;
import com.example.SWP391_SPRING2026.Enum.ProductStatus;
import com.example.SWP391_SPRING2026.Repository.Projection.ProductSearchProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(
            value = """
                select
                    p.id as id,
                    p.name as name,
                    p.brandName as brandName,
                    p.status as status,
                    p.productImage as productImage,
                    min(v.price) as minPrice,
                    max(v.price) as maxPrice,
                    coalesce(sum(v.stockQuantity), 0) as totalStock
                from Product p
                join p.variants v
                where
                    (:status is null or p.status = :status)
                    and (:brand is null or :brand = '' or lower(coalesce(p.brandName,'')) like lower(concat('%', :brand, '%')))
                    and (:minPrice is null or v.price >= :minPrice)
                    and (:maxPrice is null or v.price <= :maxPrice)
                    and (:inStock is null or :inStock = false or v.stockQuantity > 0)
                    and (
                        :keyword is null or :keyword = '' or
                        lower(coalesce(p.name,'')) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(p.description,'')) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(p.brandName,'')) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(v.sku,'')) like lower(concat('%', :keyword, '%'))
                        or exists (
                            select 1
                            from VariantAttribute a
                            where a.productVariant = v
                              and (
                                   lower(coalesce(a.attributeName,'')) like lower(concat('%', :keyword, '%'))
                                or lower(coalesce(a.attributeValue,'')) like lower(concat('%', :keyword, '%'))
                              )
                        )
                    )
                group by p.id, p.name, p.brandName, p.status, p.productImage
                """,
            countQuery = """
                select count(distinct p.id)
                from Product p
                join p.variants v
                where
                    (:status is null or p.status = :status)
                    and (:brand is null or :brand = '' or lower(coalesce(p.brandName,'')) like lower(concat('%', :brand, '%')))
                    and (:minPrice is null or v.price >= :minPrice)
                    and (:maxPrice is null or v.price <= :maxPrice)
                    and (:inStock is null or :inStock = false or v.stockQuantity > 0)
                    and (
                        :keyword is null or :keyword = '' or
                        lower(coalesce(p.name,'')) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(p.description,'')) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(p.brandName,'')) like lower(concat('%', :keyword, '%'))
                        or lower(coalesce(v.sku,'')) like lower(concat('%', :keyword, '%'))
                        or exists (
                            select 1
                            from VariantAttribute a
                            where a.productVariant = v
                              and (
                                   lower(coalesce(a.attributeName,'')) like lower(concat('%', :keyword, '%'))
                                or lower(coalesce(a.attributeValue,'')) like lower(concat('%', :keyword, '%'))
                              )
                        )
                    )
                """
    )
    Page<ProductSearchProjection> searchPublicProducts(
            @Param("keyword") String keyword,
            @Param("brand") String brand,
            @Param("status") ProductStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStock") Boolean inStock,
            Pageable pageable
    );

    @Query("""
    select distinct p from Product p
    left join fetch p.variants v
    where p.id = :id
""")
    Optional<Product> findDetailById(@Param("id") Long id);

}
