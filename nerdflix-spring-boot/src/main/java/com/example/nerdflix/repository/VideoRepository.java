package com.example.nerdflix.repository;

import com.example.nerdflix.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {
//    List<Video> findByCategoryId(Integer categoryId);

    @Query("SELECT v FROM Video v WHERE v.category.id IN :ids")
    List<Video> findAllByCategoryIds(@Param("ids") List<Integer> categoryIds);

}
