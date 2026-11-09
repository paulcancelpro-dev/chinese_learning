package com.chinese.learning.vocabulary;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WordRepository extends JpaRepository<Word, UUID> {
  Optional<Word> findByHanziAndPinyin(String hanzi, String pinyin);

  @Query(
      "select distinct w from Word w left join w.translations t where lower(w.hanzi) like"
          + " lower(concat('%',:q,'%')) or lower(w.pinyin) like lower(concat('%',:q,'%')) or"
          + " lower(t) like lower(concat('%',:q,'%'))")
  Page<Word> search(@Param("q") String query, Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from Word w where w.id=:id")
  Optional<Word> lock(@Param("id") UUID id);
}
